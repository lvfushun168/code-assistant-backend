package com.lfs.codeassistantbackend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.request.UserRequest;
import com.lfs.codeassistantbackend.domain.entity.UserEntity;
import com.lfs.codeassistantbackend.exception.BizException;
import com.lfs.codeassistantbackend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountService {
    UserRepository userRepository;

    public void register(UserRequest request) {
        String username = request.getUsername().trim();
        boolean exists = userRepository.exists(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (exists) throw new BizException("用户已存在");
        UserEntity user = new UserEntity();
        BeanUtil.copyProperties(request, user, CopyOptions.create().setIgnoreNullValue(true));
        userRepository.insert(user);
    }
}
