package com.lfs.codeassistantbackend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.codec.Base64;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.request.ChangePasswordRequest;
import com.lfs.codeassistantbackend.domain.request.UserRequest;
import com.lfs.codeassistantbackend.domain.entity.UserEntity;
import com.lfs.codeassistantbackend.domain.response.KeyPackageResponse;
import com.lfs.codeassistantbackend.exception.BizException;
import com.lfs.codeassistantbackend.repository.UserRepository;
import com.lfs.codeassistantbackend.utils.UserUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import com.lfs.codeassistantbackend.config.JwtUtil;
import com.lfs.codeassistantbackend.domain.request.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;

import cn.hutool.crypto.digest.DigestUtil;
import com.google.common.cache.Cache;

@Service
@AllArgsConstructor
@Slf4j
public class AccountService {
    private final UserRepository userRepository;
    private final HttpServletRequest httpServletRequest;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final Cache<String, Object> nonceCache;
    private DirService dirService;

    // Argon2 配置
    private static final int ARGON2_ITERATIONS = 3;
    private static final int ARGON2_MEMORY = 65536;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int SALT_LENGTH = 16;
    private static final int DEK_LENGTH = 32; // 256 bits
    private static final int NONCE_LENGTH = 12; // GCM Standard

    public void register(UserRequest request) {
        this.checkCaptcha(request.getCaptcha());
        String username = request.getUsername().trim();
        boolean exists = userRepository.exists(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (exists) throw new BizException("用户已存在");

        UserEntity user = new UserEntity();
        BeanUtil.copyProperties(request, user, CopyOptions.create().setIgnoreNullValue(true));

        // 设置登录密码 (用于获取JWT Token, 依然使用 BCrypt)
        user.setPassword(passwordEncoder.encode(DigestUtil.sha256Hex(request.getPassword())));

        // 生成端到端加密密钥包 (Client-Side Encryption Vault)
        try {
            generateAndEncryptDek(user, request.getPassword());
        } catch (Exception e) {
            log.error("密钥生成失败", e);
            throw new BizException("密钥生成失败，请重试");
        }
        userRepository.insert(user);
        // 初始化用户目录
        dirService.init(user);
    }

    /**
     * 修改密码
     * @param request 请求体，包含旧密码和新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        // 1. 获取当前登录用户
        Long userId = Optional.ofNullable(UserUtil.getUserInfo()).map(com.lfs.codeassistantbackend.domain.dto.UserDto::getUserId).orElseThrow(() -> new BizException("用户未登录"));
        UserEntity user = userRepository.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        // 2. 验证旧密码
        String hashedOldPassword = DigestUtil.sha256Hex(request.getOldPassword());
        if (!passwordEncoder.matches(hashedOldPassword, user.getPassword())) {
            throw new BizException("旧密码错误");
        }

        // 3. 更新为新密码
        user.setPassword(passwordEncoder.encode(DigestUtil.sha256Hex(request.getNewPassword())));

        // 4. 使用新密码重新生成和加密DEK
        try {
            generateAndEncryptDek(user, request.getNewPassword());
        } catch (Exception e) {
            log.error("修改密码时，重新生成密钥包失败", e);
            throw new BizException("密钥更新失败，请重试");
        }

        // 5. 保存到数据库
        userRepository.updateById(user);
    }


    /**
     * 获取用户的加密密钥包
     * @param username 用户名
     * @return 密钥包
     */
    public KeyPackageResponse getKeyPackage(String username) {
        UserEntity user = userRepository.selectOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (StringUtils.isAnyBlank(user.getDekSalt(), user.getDekEncrypted(), user.getDekNonce())) {
            throw new BizException("该用户未启用设备信任加密，请联系管理员重置");
        }
        return KeyPackageResponse.builder()
                .salt(user.getDekSalt())
                .encryptedDek(user.getDekEncrypted())
                .nonce(user.getDekNonce())
                .memoryCost(ARGON2_MEMORY)
                .iterations(ARGON2_ITERATIONS)
                .parallelism(ARGON2_PARALLELISM)
                .build();
    }

    /**
     * 生成 DEK 并用 Password 派生的 KEK 加密
     * 服务端全程只在内存处理，不落盘明文DEK
     */
    private void generateAndEncryptDek(UserEntity user, String plainPassword) throws Exception {
        SecureRandom secureRandom = new SecureRandom();

        // A. 生成随机 Salt (16 bytes)
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        // B. 生成随机 DEK (32 bytes)
        byte[] dek = new byte[DEK_LENGTH];
        secureRandom.nextBytes(dek);

        // C. 使用 Argon2id 派生 KEK
        byte[] kek = deriveKek(plainPassword, salt);

        // D. 生成 AES-GCM Nonce (12 bytes)
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);

        // E. 使用 KEK 加密 DEK
        byte[] encryptedDek = aesGcmEncrypt(dek, kek, nonce);

        // F. 保存 Base64 结果到实体
        user.setDekSalt(Base64.encode(salt));
        user.setDekNonce(Base64.encode(nonce));
        user.setDekEncrypted(Base64.encode(encryptedDek));

        // 清除敏感内存
        Arrays.fill(dek, (byte) 0);
        Arrays.fill(kek, (byte) 0);
    }

    /**
     * Argon2 密钥派生
     */
    private byte[] deriveKek(String password, byte[] salt) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ARGON2_ITERATIONS)
                .withMemoryAsKB(ARGON2_MEMORY)
                .withParallelism(ARGON2_PARALLELISM)
                .withSalt(salt);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        byte[] result = new byte[32]; // KEK length = 32 bytes
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        generator.generateBytes(passwordBytes, result, 0, result.length);

        return result;
    }

    /**
     * AES-GCM 加密
     */
    private byte[] aesGcmEncrypt(byte[] plaintext, byte[] key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, nonce); // 128 bit auth tag

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        return cipher.doFinal(plaintext);
    }

    /**
     * 验证码校验（仅Linux环境）
     * @param captcha 用户输入的验证码
     */
    private void checkCaptcha(String captcha) {
        if (System.getProperty("os.name").contains("Linux")) {
            String captchaCode = getCaptchaCodeFromCookie();
            if (StringUtils.isEmpty(captcha)) {
                throw new BizException("验证码不能为空");
            }
            if (captchaCode == null || !captchaCode.equalsIgnoreCase(captcha)) {
                throw new BizException("验证码错误");
            }
        }
    }

    public String login(LoginRequest request) {
        // 验证码校验
        this.checkCaptcha(request.getCaptcha());
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