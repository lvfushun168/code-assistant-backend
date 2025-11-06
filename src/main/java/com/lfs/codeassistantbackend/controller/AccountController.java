package com.lfs.codeassistantbackend.controller;

import com.lfs.codeassistantbackend.common.Result;
import com.lfs.codeassistantbackend.domain.request.UserRequest;
import com.lfs.codeassistantbackend.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/register")
    public Result<?> register(UserRequest request) {
        accountService.register(request);
        return Result.success();
    }

}
