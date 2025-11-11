package com.lfs.codeassistantbackend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.request.UserRequest;
import com.lfs.codeassistantbackend.domain.entity.UserEntity;
import com.lfs.codeassistantbackend.exception.BizException;
import com.lfs.codeassistantbackend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import com.lfs.codeassistantbackend.config.JwtUtil;
import com.lfs.codeassistantbackend.domain.request.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.Cookie;
import java.util.Arrays;

import cn.hutool.crypto.digest.DigestUtil;
import com.google.common.cache.Cache;

@Service
@AllArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final HttpServletRequest httpServletRequest;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final Cache<String, Object> nonceCache;
    private DirService dirService;


    public void register(UserRequest request) {
        String captchaCode = getCaptchaCodeFromCookie();
        if (captchaCode == null || !captchaCode.equalsIgnoreCase(request.getCaptcha())) {
            throw new BizException("验证码错误");
        }
        String username = request.getUsername().trim();
        boolean exists = userRepository.exists(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (exists) throw new BizException("用户已存在");
        UserEntity user = new UserEntity();
        BeanUtil.copyProperties(request, user, CopyOptions.create().setIgnoreNullValue(true));
        user.setPassword(passwordEncoder.encode(DigestUtil.sha256Hex(request.getPassword())));
        userRepository.insert(user);
        //初始化用户目录
        dirService.init(user);
    }

    public String login(LoginRequest request) {
        // 验证码校验
        String captchaCode = getCaptchaCodeFromCookie();
        if (captchaCode == null || !captchaCode.equalsIgnoreCase(request.getCaptcha())) {
            throw new BizException("验证码错误");
        }

        // 时间戳校验
        long timestamp = Long.parseLong(request.getTimestamp());
        if (System.currentTimeMillis() - timestamp > 1000 * 60 * 5) { // 5 minutes
            throw new BizException("请求超时");
        }

        // nonce校验
        if (nonceCache.getIfPresent(request.getNonce()) != null) {
            throw new BizException("重复的请求");
        }
        nonceCache.put(request.getNonce(), true);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), DigestUtil.sha256Hex(request.getPassword()))
        );
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        return jwtUtil.generateToken(userDetails);
    }

    private String getCaptchaCodeFromCookie() {
        if (httpServletRequest.getCookies() == null) {
            return null;
        }
        return Arrays.stream(httpServletRequest.getCookies())
                .filter(cookie -> "captchaCode".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
