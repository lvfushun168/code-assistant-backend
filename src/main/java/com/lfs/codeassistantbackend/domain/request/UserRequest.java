package com.lfs.codeassistantbackend.domain.request;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserRequest {

    private String username;

    private String password;

    private String nickname;

    private String phoneNum;
}
