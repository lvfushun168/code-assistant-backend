package com.lfs.codeassistantbackend.domain.request;

import com.lfs.codeassistantbackend.domain.request.group.Update;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentRequest {

    @NotNull(message = "ID不能为空", groups = Update.class)
    private Long id;

    @NotNull(message = "目录ID不能为空")
    private Long dirId;

    @NotBlank(message = "文档名不能为空")
    private String title;

    @NotBlank(message = "文档内容不能为空")
    private String content;
}
