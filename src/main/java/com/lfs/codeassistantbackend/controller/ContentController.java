package com.lfs.codeassistantbackend.controller;

import com.lfs.codeassistantbackend.common.Result;
import com.lfs.codeassistantbackend.domain.request.ContentRequest;
import com.lfs.codeassistantbackend.domain.request.group.Update;
import com.lfs.codeassistantbackend.service.ContentService;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/content")
public class ContentController {
    ContentService contentService;

    /**
     * 创建文档
     * @param request 文档请求体
     * @return 操作结果
     */
    @PostMapping
    public Result<?> create(@RequestBody @Validated ContentRequest request){
        contentService.create(request);
        return Result.success();
    }

    /**
     * 更新文档
     * @param request 文档请求体
     * @return 操作结果
     */
    @PutMapping
    public Result<?> update(@RequestBody @Validated(Update.class) ContentRequest request){
        contentService.update(request);
        return Result.success();
    }

    /**
     * 删除文档
     * @param id 文档ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id){
        contentService.delete(Long.valueOf(id));
        return Result.success();
    }

    /**
     * 列出目录下的所有文档
     * @param dirId 目录ID
     * @return 文档列表
     */
    @GetMapping("/{dirId}")
    public Result<?> list(@PathVariable String dirId){
        return Result.success(contentService.list(Long.valueOf(dirId)));
    }
}
