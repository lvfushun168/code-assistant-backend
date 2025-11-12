package com.lfs.codeassistantbackend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lfs.codeassistantbackend.domain.entity.ContentEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends BaseMapper<ContentEntity> {
}
