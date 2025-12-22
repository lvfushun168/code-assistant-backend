package com.lfs.codeassistantbackend.controller;

import com.lfs.codeassistantbackend.common.Result;
import com.lfs.codeassistantbackend.domain.response.CloudCheckResponse;
import com.lfs.codeassistantbackend.service.CloudFsService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 云端同步功能
 */
@RestController
@RequestMapping("/api/fs")
@AllArgsConstructor
public class CloudFsController {

    private final CloudFsService cloudFsService;

    /**
     * 检查云端文件是否存在
     * @param path 云端绝对路径 (如:/MyProject/src/Main.java)
     */
    @GetMapping("/check")
    public Result<CloudCheckResponse> checkFile(@RequestParam("path") String path) {
        boolean exists = cloudFsService.checkFileExists(path);
        return Result.success(new CloudCheckResponse(exists));
    }

    /**
     * 上传文件到云端
     * @param file 文件二进制流
     * @param destDir 云端目标目录路径 (如:/MyProject/src)
     * @param override 是否覆盖 (true: 覆盖, false/null: 不覆盖)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("destDir") String destDir,
            @RequestParam(value = "override", required = false, defaultValue = "false") Boolean override) {
        cloudFsService.uploadFile(file, destDir, override);
        return Result.success();
    }

    /**
     * 从云端下载文件
     * @param path 云端绝对路径 (如:/MyProject/src/Main.java)
     */
    @GetMapping("/download")
    public void downloadFile(@RequestParam("path") String path, HttpServletResponse response) {
        cloudFsService.downloadFile(path, response);
    }
}