package com.lfs.codeassistantbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName(value = "dir")
public class DirEntity {

    /**
     * id
     */
    private Long id;

    /**
     * 目录名
     */
    private String name;

    /**
     * 上级目录id
     */
    private Long parentId;

    /**
     * 用户id
     */
    private Long userId;

}
