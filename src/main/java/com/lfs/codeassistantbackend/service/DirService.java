package com.lfs.codeassistantbackend.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.entity.ContentEntity;
import com.lfs.codeassistantbackend.domain.entity.DirEntity;
import com.lfs.codeassistantbackend.domain.entity.UserEntity;
import com.lfs.codeassistantbackend.domain.request.DirRequest;
import com.lfs.codeassistantbackend.domain.response.DirTreeResponse;
import com.lfs.codeassistantbackend.exception.BizException;
import com.lfs.codeassistantbackend.repository.ContentRepository;
import com.lfs.codeassistantbackend.repository.DirRepository;
import com.lfs.codeassistantbackend.utils.UserUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class DirService {

    private DirRepository dirRepository;
    private ContentRepository contentRepository;

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

    /**
     * 获取当前用户的目录树
     * @return 目录树
     */
    public DirTreeResponse getTree(){
        DirEntity root = dirRepository.selectOne(new LambdaQueryWrapper<DirEntity>().eq(DirEntity::getUserId, UserUtil.getUserInfo().getUserId()).isNull(DirEntity::getParentId));
        DirTreeResponse response = BeanUtil.copyProperties(root, DirTreeResponse.class);
        return response.getTree();
    }


    /**
     * 新建目录
     * @param request 目录请求
     */
    public DirTreeResponse create(DirRequest request){
        boolean exists = dirRepository.exists(new LambdaQueryWrapper<DirEntity>()
                .eq(DirEntity::getUserId, UserUtil.getUserInfo().getUserId())
                .eq(DirEntity::getParentId, request.getParentId())
                .eq(DirEntity::getName, request.getName())
        );
        if (exists) {
            throw new BizException("目录名称已存在");
        }
        DirEntity entity = DirEntity.builder().name(request.getName()).parentId(request.getParentId()).userId(UserUtil.getUserInfo().getUserId()).build();
        dirRepository.insert(entity);
        return BeanUtil.copyProperties(entity, DirTreeResponse.class);
    }

    /**
     * 修改目录
     * @param request 修改目录
     */
    public DirTreeResponse update(DirRequest request){
        DirEntity dirEntity = dirRepository.selectById(request.getId());
        if (null == dirEntity) {
            throw new BizException("目录不存在");
        }
        boolean exists = dirRepository.exists(new LambdaQueryWrapper<DirEntity>()
                .eq(DirEntity::getUserId, UserUtil.getUserInfo().getUserId())
                .eq(DirEntity::getParentId, dirEntity.getParentId())
                .eq(DirEntity::getName, request.getName())
                .ne(DirEntity::getId, request.getId())
        );
        if (exists) {
            throw new BizException("目录名称已存在");
        }
        dirEntity.setName(request.getName());
        dirRepository.updateById(dirEntity);
        DirTreeResponse dirTreeResponse = BeanUtil.copyProperties(dirEntity, DirTreeResponse.class);
        return dirTreeResponse.getTree();
    }

    /**
     * 删除目录及其子目录，以及各目录下的文件
     * @param id 目录id
     */
    public void delete(Long id) {
        DirEntity dirEntity = dirRepository.selectById(id);
        if (dirEntity.getParentId() == null) {
            throw new BizException("根目录禁止删除");
        }
        // 获取所有要删除的目录ID
        Set<DirEntity> subDirList = dirEntity.getSubDirList();
        Set<Long> ids = subDirList.stream().map(DirEntity::getId).collect(Collectors.toSet());
        // 删除目录下的文件
        contentRepository.delete(new LambdaQueryWrapper<ContentEntity>().in(ContentEntity::getDirId, ids));
        // 删除目录
        dirRepository.deleteBatchIds(ids);
    }

}
