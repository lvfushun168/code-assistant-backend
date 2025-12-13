package com.lfs.codeassistantbackend.controller;

import com.lfs.codeassistantbackend.common.Result;
import com.lfs.codeassistantbackend.domain.request.ChangePasswordRequest;
import com.lfs.codeassistantbackend.domain.request.UserRequest;
import com.lfs.codeassistantbackend.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.lfs.codeassistantbackend.domain.request.LoginRequest;

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
    public Result<?> register(@RequestBody @Validated UserRequest request) {
        accountService.register(request);
        return Result.success();
    }

    /**
     * 用户登录
     * @param request 登录信息
     * @return token
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody @Validated LoginRequest request) {
        return Result.success(accountService.login(request));
    }

    /**
     * 获取用户密钥包
     * 用于新设备首次登录时，拉取加密的 DEK 并在本地恢复
     * @param username 用户名
     * @return 密钥包数据
     */
    @GetMapping("/key-package")
    public Result<?> getKeyPackage(@RequestParam String username) {
        return Result.success(accountService.getKeyPackage(username));
    }

    /**
     * 修改密码
     * @param request 包含旧密码和新密码
     * @return 成功或失败
     */
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestBody @Validated ChangePasswordRequest request) {
        accountService.changePassword(request);
        return Result.success("密码修改成功");
    }

}