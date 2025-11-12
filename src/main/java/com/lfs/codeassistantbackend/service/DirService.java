package com.lfs.codeassistantbackend.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.dto.UserDto;
import com.lfs.codeassistantbackend.domain.entity.ContentEntity;
import com.lfs.codeassistantbackend.domain.entity.DirEntity;
import com.lfs.codeassistantbackend.domain.entity.UserEntity;
import com.lfs.codeassistantbackend.domain.response.ContentResponse;
import com.lfs.codeassistantbackend.domain.response.DirTreeResponse;
import com.lfs.codeassistantbackend.repository.ContentRepository;
import com.lfs.codeassistantbackend.repository.DirRepository;
import com.lfs.codeassistantbackend.utils.UserUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
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
        return null;
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


}
