package com.lfs.codeassistantbackend.domain.response;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.dto.UserDto;
import com.lfs.codeassistantbackend.domain.entity.ContentEntity;
import com.lfs.codeassistantbackend.domain.entity.DirEntity;
import com.lfs.codeassistantbackend.repository.ContentRepository;
import com.lfs.codeassistantbackend.repository.DirRepository;
import com.lfs.codeassistantbackend.utils.UserUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 目录树
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DirTreeResponse {
    /**
     * id
     */
    private Long id;

    /**
     * 父id
     */
    private Long parentId;

    /**
     * 目录名
     */
    private String name;

    /**
     * 目录下的文档内容
     */
    private List<ContentResponse> contents;

    /**
     * 子目录
     */
    private List<DirTreeResponse> children;


    /**
     * 获取该节点的所有下级目录树
     * @return 目录树
     */
    @JsonIgnore
    public DirTreeResponse getTree(){
        DirRepository dirRepository = SpringUtil.getBean(DirRepository.class);
        ContentRepository contentRepository = SpringUtil.getBean(ContentRepository.class);
        UserDto userInfo = UserUtil.getUserInfo();
        List<DirEntity> dirList = dirRepository.selectList(new LambdaQueryWrapper<DirEntity>().eq(DirEntity::getUserId, userInfo.getUserId()));
        //目录下的文档map
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
        this.generateTree(this, parentIdMap, dirContentsMap);
        return this;
    }


    /**
     * 组装目录树与相关文档
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
