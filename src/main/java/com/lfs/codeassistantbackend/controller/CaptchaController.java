package com.lfs.codeassistantbackend.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.lfs.codeassistantbackend.common.Result;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/captcha")
@AllArgsConstructor
public class CaptchaController {

    @GetMapping("/generate")
    public void generate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 定义图形验证码的长、宽、验证码字符数、干扰线宽度
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(70, 35, 4, 20);
        // 将验证码放入cookie
        Cookie cookie = new Cookie("captchaCode", lineCaptcha.getCode());
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 5); // 5 minutes
        response.addCookie(cookie);
        // 设置响应头
        response.setContentType("image/png");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expire", 0);
        // 输出图片
        lineCaptcha.write(response.getOutputStream());
    }
}
