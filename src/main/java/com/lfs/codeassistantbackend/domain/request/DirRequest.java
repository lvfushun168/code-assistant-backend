package com.lfs.codeassistantbackend.domain.request;

import com.lfs.codeassistantbackend.domain.request.group.Update;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 目录修改请求
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DirRequest {

    @NotNull(message = "目录ID不能为空", groups = Update.class)
    private Long id;

    @NotNull(message = "父目录ID不能为空")
    private Long parentId;

    @NotBlank(message = "目录名称不能为空")
    private String name;

}
