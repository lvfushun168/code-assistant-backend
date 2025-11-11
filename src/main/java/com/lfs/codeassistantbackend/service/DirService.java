package com.lfs.codeassistantbackend.service;

import com.lfs.codeassistantbackend.domain.entity.DirEntity;
import com.lfs.codeassistantbackend.domain.entity.UserEntity;
import com.lfs.codeassistantbackend.repository.DirRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DirService {

    private DirRepository dirRepository;

    /**
     * 用户目录初始化
     * @param user 用户信息
     */
    public void init(UserEntity user) {
        dirRepository.insert(DirEntity.builder()
                .name(user.getNickname())
                .userId(user.getId())
                .build());
    }

}
