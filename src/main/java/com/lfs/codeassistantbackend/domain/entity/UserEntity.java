package com.lfs.codeassistantbackend.domain.entity;

import lombok.*;


/**
 * 注册的用户
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserEntity extends BaseEntity{

    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String phoneNum;

}
