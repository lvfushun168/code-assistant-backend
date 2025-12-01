package com.lfs.codeassistantbackend.controller;

import com.lfs.codeassistantbackend.common.Result;
import com.lfs.codeassistantbackend.domain.request.ContentRequest;
import com.lfs.codeassistantbackend.domain.request.group.Update;
import com.lfs.codeassistantbackend.service.ContentService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

@AllArgsConstructor
@RestController
@RequestMapping("/content")
public class ContentController {
    ContentService contentService;

    /**
     * 创建文档
     * @param file 文件流
     * @param request 元数据
     * @return 操作结果
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> create(@RequestPart("file") MultipartFile file, @RequestPart("meta") @Validated ContentRequest request){
        contentService.create(file, request);
        return Result.success();
    }

    /**
     * 更新文档
     * @param file 文件流(允许用户不更新文件，只更新元数据)
     * @param request 元数据
     * @return 操作结果
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> update(@RequestPart(value = "file", required = false) MultipartFile file, @RequestPart("meta") @Validated(Update.class) ContentRequest request){
        contentService.update(file, request);
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


    /**
     * 下载文档
     * @param id 文档ID
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable String id, HttpServletResponse response) {
        contentService.download(Long.valueOf(id), response);
    }
}
