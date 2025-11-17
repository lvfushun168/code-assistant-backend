package com.lfs.codeassistantbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName("prompt")
public class ContentEntity extends BaseEntity{

    private Long dirId;

    private String title;

    private String content;

    //用户id
    private String creator;
}
