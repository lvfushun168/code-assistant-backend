package com.lfs.codeassistantbackend.controller;

import com.lfs.codeassistantbackend.common.Result;
import com.lfs.codeassistantbackend.domain.request.UserRequest;
import com.lfs.codeassistantbackend.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户账户
 */
@RequestMapping("/account")
@RestController
@AllArgsConstructor
public class AccountController {

    AccountService accountService;

    /**
     * 用户注册
     * @param request 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody UserRequest request) {
        accountService.register(request);
        return Result.success();
    }

}
