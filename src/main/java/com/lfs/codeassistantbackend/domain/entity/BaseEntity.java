package com.lfs.codeassistantbackend.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseEntity {

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean delete;
}
