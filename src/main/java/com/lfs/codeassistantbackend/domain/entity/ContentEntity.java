package com.lfs.codeassistantbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName("content")
public class ContentEntity extends BaseEntity{

    /**
     * 目录ID
     */
    private Long dirId;

    /**
     * 标题
     */
    private String title;

    /**
     * 存储路径
     */
    private String filePath;

    //用户id
    private Long creator;

    /**
     * 是否加密
     */
    private Boolean encrypted;

    /**
     * 内容摘要
     */
    private String contentHash;
}
