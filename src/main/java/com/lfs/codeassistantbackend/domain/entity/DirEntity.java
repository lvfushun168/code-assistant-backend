package com.lfs.codeassistantbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName(value = "dir")
public class DirEntity extends BaseEntity{

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
