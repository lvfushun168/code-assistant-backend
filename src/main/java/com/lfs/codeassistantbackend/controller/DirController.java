package com.lfs.codeassistantbackend.controller;

import com.lfs.codeassistantbackend.common.Result;
import com.lfs.codeassistantbackend.domain.response.DirTreeResponse;
import com.lfs.codeassistantbackend.service.DirService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
