package com.lfs.codeassistantbackend.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeyPackageResponse {
    /**
     * 用户注册时生成的随机Salt (Base64)
     */
    private String salt;

    /**
     * 使用KEK加密后的DEK (Base64)
     */
    private String encryptedDek;

    /**
     * AES-GCM的IV (Base64)
     */
    private String nonce;

    /**
     * Argon2 参数: 内存开销 (KB)
     */
    private Integer memoryCost;

    /**
     * Argon2 参数: 迭代次数
     */
    private Integer iterations;

    /**
     * Argon2 参数: 并行度
     */
    private Integer parallelism;
}