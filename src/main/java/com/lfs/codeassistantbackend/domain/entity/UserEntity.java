package com.lfs.codeassistantbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;


/**
 * 注册的用户
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName("user")
public class UserEntity extends BaseEntity{

    private String username;

    private String password;

    private String nickname;

    private String phoneNum;

    /**
     * Argon2 Salt (Base64)
     * 用于客户端派生 KEK
     */
    private String dekSalt;

    /**
     * AES-GCM Encrypted DEK (Base64)
     * 被 KEK 加密后的数据加密密钥
     */
    private String dekEncrypted;

    /**
     * AES-GCM IV (Base64)
     * 加密 DEK 时使用的随机 nonce
     */
    private String dekNonce;

}