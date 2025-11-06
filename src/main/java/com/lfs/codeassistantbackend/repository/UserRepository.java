package com.lfs.codeassistantbackend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lfs.codeassistantbackend.domain.entity.UserEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends BaseMapper<UserEntity> {

}
