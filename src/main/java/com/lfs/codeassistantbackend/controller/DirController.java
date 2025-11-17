package com.lfs.codeassistantbackend.controller;

import com.lfs.codeassistantbackend.common.Result;
import com.lfs.codeassistantbackend.domain.request.DirRequest;
import com.lfs.codeassistantbackend.domain.request.group.Update;
import com.lfs.codeassistantbackend.domain.response.DirTreeResponse;
import com.lfs.codeassistantbackend.service.DirService;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/dir")
@RestController
@AllArgsConstructor
public class DirController {
    private DirService dirService;

    /**
     * 获取当前用户的目录树
     * @return 目录树
     */
    @GetMapping("/tree")
    public Result<DirTreeResponse> tree(){
        return Result.success(dirService.getTree());
    }

    /**
     * 创建目录
     * @param request 目录请求参数
     * @return 操作结果
     */
    @PostMapping
    public Result<?> create(@RequestBody @Validated DirRequest request){
        dirService.create(request);
        return Result.success();
    }

    /**
     * 更新目录
     * @param request 目录请求参数
     * @return 操作结果
     */
    @PutMapping
    public Result<?> update(@RequestBody @Validated(Update.class) DirRequest request){
        dirService.update(request);
        return Result.success();
    }

    /**
     * 删除目录
     * @param id 目录ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id){
        dirService.delete(Long.valueOf(id));
        return Result.success();
    }

}
