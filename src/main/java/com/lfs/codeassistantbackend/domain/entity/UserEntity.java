package com.lfs.codeassistantbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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

}
