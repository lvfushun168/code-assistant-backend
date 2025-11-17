package com.lfs.codeassistantbackend.domain.request;

import com.lfs.codeassistantbackend.domain.request.group.Update;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class ContentRequest {

    @NotNull(message = "ID不能为空", groups = Update.class)
    private Long id;

    @NotNull(message = "目录ID不能为空")
    private Long dirId;

    @NotNull(message = "文档名不能为空")
    private String title;

    @NotNull(message = "文档内容不能为空")
    private String content;
}
