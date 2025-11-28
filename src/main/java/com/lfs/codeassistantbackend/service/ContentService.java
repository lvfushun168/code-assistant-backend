package com.lfs.codeassistantbackend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.entity.ContentEntity;
import com.lfs.codeassistantbackend.domain.request.ContentRequest;
import com.lfs.codeassistantbackend.domain.response.ContentResponse;
import com.lfs.codeassistantbackend.exception.BizException;
import com.lfs.codeassistantbackend.repository.ContentRepository;
import com.lfs.codeassistantbackend.utils.UserUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ContentService {
    ContentRepository contentRepository;
    LocalFileStorage localFileStorage;


    public void create(MultipartFile file, ContentRequest request) {
        if (file == null || file.isEmpty()) throw new BizException("必须上传文档");
        boolean exists = contentRepository.exists(new LambdaQueryWrapper<ContentEntity>()
                .eq(ContentEntity::getDirId, request.getDirId())
                .eq(ContentEntity::getTitle, request.getTitle()));
        if (exists) throw new BizException("文档已存在");

        ContentEntity contentEntity = BeanUtil.copyProperties(request, ContentEntity.class);
        try {
            contentEntity.setContentHash(DigestUtil.sha256Hex(file.getInputStream()));
            contentEntity.setFilePath(localFileStorage.saveStream(file.getInputStream()));
        } catch (IOException e) {
            throw new BizException("文件处理失败");
        }
        contentEntity.setCreator(UserUtil.getUserInfo().getUserId());
        contentRepository.insert(contentEntity);
    }

    public void update(MultipartFile file, ContentRequest request) {
        Long id = request.getId();
        ContentEntity contentEntity = contentRepository.selectById(id);
        if (contentEntity == null) throw new BizException("文档不存在");

        boolean exists = contentRepository.exists(new LambdaQueryWrapper<ContentEntity>()
                .eq(ContentEntity::getDirId, request.getDirId())
                .eq(ContentEntity::getTitle, request.getTitle())
                .ne(ContentEntity::getId, id));
        if (exists) throw new BizException("文档已存在");

        BeanUtil.copyProperties(request, contentEntity);
        // 用户传了新文件就需要比对Hash
        if (file != null && !file.isEmpty()) {
            try {
                // 计算新文件Hash并比对
                String newHash = DigestUtil.sha256Hex(file.getInputStream());
                if (!newHash.equals(contentEntity.getContentHash())) {
                    // 内容变了删除旧文件写入新文件 (重新获取流)
                    if (StringUtils.isNotEmpty(contentEntity.getFilePath())) {
                        localFileStorage.deleteFile(contentEntity.getFilePath());
                    }
                    String newPath = localFileStorage.saveStream(file.getInputStream());
                    contentEntity.setFilePath(newPath);
                    contentEntity.setContentHash(newHash);
                } else {
                    log.info("新旧文件内容相同，跳过更新文件存储");
                }
            } catch (IOException e) {
                throw new BizException("文件读取异常");
            }
        }
        contentRepository.updateById(contentEntity);
    }

    public void delete(Long id) {
        ContentEntity contentEntity = contentRepository.selectById(id);
        if (contentEntity == null) {
            throw new BizException("文档不存在");
        }
        contentRepository.deleteById(id);
        localFileStorage.deleteFile(contentEntity.getFilePath()); //确实没多少空间
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
