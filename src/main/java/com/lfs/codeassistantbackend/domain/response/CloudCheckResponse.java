package com.lfs.codeassistantbackend.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检查文件响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudCheckResponse {
    private boolean exists;
}