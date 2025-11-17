package com.lfs.codeassistantbackend.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.dto.UserDto;
import com.lfs.codeassistantbackend.domain.entity.ContentEntity;
import com.lfs.codeassistantbackend.domain.entity.DirEntity;
import com.lfs.codeassistantbackend.domain.entity.UserEntity;
import com.lfs.codeassistantbackend.domain.request.DirRequest;
import com.lfs.codeassistantbackend.domain.response.ContentResponse;
import com.lfs.codeassistantbackend.domain.response.DirTreeResponse;
import com.lfs.codeassistantbackend.exception.BizException;
import com.lfs.codeassistantbackend.repository.ContentRepository;
import com.lfs.codeassistantbackend.repository.DirRepository;
import com.lfs.codeassistantbackend.utils.UserUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        UserDto userInfo = UserUtil.getUserInfo();
        List<DirEntity> dirList = dirRepository.selectList(new LambdaQueryWrapper<DirEntity>().eq(DirEntity::getUserId, userInfo.getUserId()));
        //目录下的文档
        Map<Long, List<ContentResponse>> dirContentsMap = new HashMap<>();
        List<ContentEntity> contentList = contentRepository.selectList(new LambdaQueryWrapper<ContentEntity>().in(ContentEntity::getDirId, dirList.stream().map(DirEntity::getId).collect(Collectors.toList())));
        if (!CollectionUtils.isEmpty(contentList)) {
            dirContentsMap = contentList.stream().collect(Collectors.groupingBy(ContentEntity::getDirId, Collectors.mapping(contentEntity -> {
                ContentResponse contentResponse = new ContentResponse();
                BeanUtil.copyProperties(contentEntity, contentResponse);
                return contentResponse;
            }, Collectors.toList())));
        }
        List<DirTreeResponse> dirTreeResponseList = dirList.stream().map(dirEntity -> DirTreeResponse.builder()
                .id(dirEntity.getId())
                .parentId(dirEntity.getParentId())
                .name(dirEntity.getName())
                .build()).collect(Collectors.toList());
        // 父idMap
        Map<Long, List<DirTreeResponse>> parentIdMap = dirTreeResponseList.stream().filter(dirTreeResponse -> null != dirTreeResponse.getParentId()).collect(Collectors.groupingBy(DirTreeResponse::getParentId));
        // 根节点
        DirTreeResponse root = dirList.stream().filter(dirEntity -> null == dirEntity.getParentId()).findFirst().map(dirEntity -> DirTreeResponse.builder()
                .id(dirEntity.getId())
                .parentId(dirEntity.getParentId())
                .name(dirEntity.getName())
                .build()).orElse(null);
        this.generateTree(root,
                parentIdMap,
                dirContentsMap);
        return root;
    }

    /**
     * 组装目录树于相关文档
     * @param root 根节点
     * @param parentIdMap 父id对应的目录
     * @param dirContentsMap 目录对应的文档
     */
    private void generateTree(DirTreeResponse root, Map<Long, List<DirTreeResponse>> parentIdMap, Map<Long, List<ContentResponse>> dirContentsMap ) {
        if (root == null) return;
        Long id = root.getId();
        root.setContents(dirContentsMap.get(id));
        List<DirTreeResponse> children = parentIdMap.get(id);
        if (!CollectionUtils.isEmpty(children)) {
            root.setChildren(children);
            children.forEach(child -> this.generateTree(child, parentIdMap, dirContentsMap));
        }
    }

    /**
     * 新建目录
     * @param request 目录请求
     */
    public void create(DirRequest request){
        dirRepository.insert(DirEntity.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .userId(UserUtil.getUserInfo().getUserId())
                .build());
    }

    /**
     * 修改目录
     * @param request 修改目录
     */
    public void update(DirRequest request){
        DirEntity dirEntity = dirRepository.selectById(request.getId());
        if (null == dirEntity) {
            throw new BizException("目录不存在");
        }
        boolean exists = dirRepository.exists(new LambdaQueryWrapper<DirEntity>()
                .eq(DirEntity::getUserId, UserUtil.getUserInfo().getUserId())
                .eq(DirEntity::getName, request.getName())
                .ne(DirEntity::getId, request.getId())
        );
        if (exists) {
            throw new BizException("目录名称已存在");
        }
        dirEntity.setName(request.getName());
        dirRepository.updateById(dirEntity);
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
