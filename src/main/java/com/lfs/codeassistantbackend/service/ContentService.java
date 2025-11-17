package com.lfs.codeassistantbackend.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.entity.ContentEntity;
import com.lfs.codeassistantbackend.domain.request.ContentRequest;
import com.lfs.codeassistantbackend.domain.response.ContentResponse;
import com.lfs.codeassistantbackend.exception.BizException;
import com.lfs.codeassistantbackend.repository.ContentRepository;
import com.lfs.codeassistantbackend.utils.UserUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ContentService {
    ContentRepository contentRepository;


    public void create(ContentRequest request) {
        boolean exists = contentRepository.exists(new LambdaQueryWrapper<ContentEntity>()
                .eq(ContentEntity::getDirId, request.getDirId())
                .eq(ContentEntity::getTitle, request.getTitle()));
        if (exists) {
            throw new BizException("文档已存在");
        }
        ContentEntity contentEntity = BeanUtil.copyProperties(request, ContentEntity.class);
        contentEntity.setCreator(UserUtil.getUserInfo().getUserId());
        contentRepository.insert(contentEntity);
    }

    public void update(ContentRequest request) {
        Long id = request.getId();
        ContentEntity contentEntity = contentRepository.selectById(id);
        if (contentEntity == null) {
            throw new BizException("文档不存在");
        }
        boolean exists = contentRepository.exists(new LambdaQueryWrapper<ContentEntity>()
                .eq(ContentEntity::getDirId, request.getDirId())
                .eq(ContentEntity::getTitle, request.getTitle())
                .ne(ContentEntity::getId, id));
        if (exists) {
            throw new BizException("文档已存在");
        }
        BeanUtil.copyProperties(request, contentEntity);
        contentRepository.updateById(contentEntity);
    }

    public void delete(Long id) {
        ContentEntity contentEntity = contentRepository.selectById(id);
        if (contentEntity == null) {
            throw new BizException("文档不存在");
        }
        contentRepository.deleteById(id);
    }

    public List<ContentResponse> list(Long dirId) {
        List<ContentEntity> contentEntities = contentRepository.selectList(new LambdaQueryWrapper<ContentEntity>().eq(ContentEntity::getDirId, dirId));
        return contentEntities.stream().map(contentEntity -> {
            ContentResponse contentResponse = new ContentResponse();
            BeanUtil.copyProperties(contentEntity, contentResponse);
            return contentResponse;
        }).collect(Collectors.toList());
    }
}
