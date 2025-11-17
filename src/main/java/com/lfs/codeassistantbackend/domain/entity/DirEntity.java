package com.lfs.codeassistantbackend.domain.entity;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.repository.DirRepository;
import com.lfs.codeassistantbackend.utils.UserUtil;
import lombok.*;
import org.springframework.util.CollectionUtils;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@TableName(value = "dir")
public class DirEntity extends BaseEntity{

    /**
     * 目录名
     */
    private String name;

    /**
     * 上级目录id
     */
    private Long parentId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 获取某个目录下的子目录（包含自身）
     * @return 目录列表
     */
    public Set<DirEntity> getSubDirList() {
        DirRepository dirRepository = SpringUtil.getBean(DirRepository.class);
        List<DirEntity> allDirs = dirRepository.selectList(new LambdaQueryWrapper<DirEntity>().eq(DirEntity::getUserId, UserUtil.getUserInfo().getUserId()));
        Set<DirEntity> result = new HashSet<>(){{add(DirEntity.this);}};
        if (!CollectionUtils.isEmpty(allDirs)) {
            this.getSubDirList(this, allDirs, result);
        }
        return result;
    }

    /**
     * 递归获取某目录下的所有子目录
     * @param root 根目录
     * @param allDirs 所有子目录
     * @param result 结果目录列表
     */
    private void getSubDirList(DirEntity root, List<DirEntity> allDirs, Set<DirEntity> result) {
        for (DirEntity dir : allDirs) {
            if (Objects.equals(dir.getParentId(), root.getId())) {
                result.add(dir);
                getSubDirList(dir, allDirs, result);
            }
        }
    }
}
