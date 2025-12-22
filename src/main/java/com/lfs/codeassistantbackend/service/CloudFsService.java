package com.lfs.codeassistantbackend.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lfs.codeassistantbackend.domain.entity.ContentEntity;
import com.lfs.codeassistantbackend.domain.entity.DirEntity;
import com.lfs.codeassistantbackend.exception.BizException;
import com.lfs.codeassistantbackend.repository.ContentRepository;
import com.lfs.codeassistantbackend.repository.DirRepository;
import com.lfs.codeassistantbackend.utils.UserUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CloudFsService {

    private final DirRepository dirRepository;
    private final ContentRepository contentRepository;
    private final LocalFileStorage localFileStorage;

    /**
     * 检查文件是否存在
     * @param path 全路径，例如 /Project/src/Main.java
     */
    public boolean checkFileExists(String path) {
        try {
            resolveContentEntity(path);
            return true;
        } catch (BizException e) {
            // 如果是因为找不到而抛出的特定异常，返回false
            // 这里为了简化，假设解析失败即不存在
            return false;
        }
    }

    /**
     * 上传文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void uploadFile(MultipartFile file, String destDir, Boolean override) {
        // 获取当前用户根目录
        Long rootDirId = getRootDirId();
        // 解析目标目录 ID
        Long targetDirId = resolveDirId(rootDirId, destDir);
        String filename = file.getOriginalFilename();
        if (StrUtil.isBlank(filename)) {
            throw new BizException("文件名不能为空");
        }
        // 检查文件是否已存在
        ContentEntity existingFile = contentRepository.selectOne(new LambdaQueryWrapper<ContentEntity>()
                .eq(ContentEntity::getDirId, targetDirId)
                .eq(ContentEntity::getTitle, filename));

        if (existingFile != null) {
            if (!override) {
                throw new BizException("文件已存在，请确认是否覆盖");
            }
            // 覆盖逻辑
            updateFileContent(existingFile, file);
        } else {
            // 新增逻辑
            createFileContent(targetDirId, filename, file);
        }
    }

    /**
     * 下载文件
     */
    public void downloadFile(String path, HttpServletResponse response) {
        // 1. 解析文件实体
        ContentEntity content = resolveContentEntity(path);

        // 2. 设置响应头
        try {
            response.reset();
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode(content.getTitle(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName);
            
            // 3. 写入流
            localFileStorage.downloadToStream(content.getFilePath(), response.getOutputStream());
        } catch (IOException e) {
            log.error("下载流写入失败", e);
            throw new BizException("文件下载失败");
        }
    }


    /**
     * 根据全路径解析出文件实体
     */
    private ContentEntity resolveContentEntity(String path) {
        // 路径标准化处理，去除首尾斜杠
        path = StrUtil.trim(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        List<String> parts = Arrays.stream(path.split("/"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());

        if (parts.isEmpty()) {
            throw new BizException("无效的文件路径");
        }

        String fileName = parts.get(parts.size() - 1);
        String dirPath = String.join("/", parts.subList(0, parts.size() - 1));

        // 解析目录
        Long rootDirId = getRootDirId();
        Long dirId = resolveDirId(rootDirId, "/" + dirPath); // 补回前导斜杠以适配resolveDirId逻辑

        // 查找文件
        ContentEntity content = contentRepository.selectOne(new LambdaQueryWrapper<ContentEntity>()
                .eq(ContentEntity::getDirId, dirId)
                .eq(ContentEntity::getTitle, fileName));
        
        if (content == null) {
            throw new BizException("文件不存在: " + path);
        }
        return content;
    }

    /**
     * 解析目录路径为 ID
     * @param rootDirId 用户根目录ID
     * @param path 相对根目录的路径，如 /Project/src
     */
    private Long resolveDirId(Long rootDirId, String path) {
        if (StrUtil.isBlank(path) || "/".equals(path.trim())) {
            return rootDirId;
        }

        // 去除前导斜杠
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String[] dirs = path.split("/");
        Long currentDirId = rootDirId;

        for (String dirName : dirs) {
            if (StrUtil.isBlank(dirName)) continue;
            
            DirEntity dir = dirRepository.selectOne(new LambdaQueryWrapper<DirEntity>()
                    .eq(DirEntity::getParentId, currentDirId)
                    .eq(DirEntity::getName, dirName)
                    .eq(DirEntity::getUserId, UserUtil.getUserInfo().getUserId())); // 确保是自己的目录

            if (dir == null) {
                throw new BizException("云端目录不存在: " + dirName);
            }
            currentDirId = dir.getId();
        }
        return currentDirId;
    }

    /**
     * 获取当前用户的根目录ID
     */
    private Long getRootDirId() {
        DirEntity root = dirRepository.selectOne(new LambdaQueryWrapper<DirEntity>()
                .eq(DirEntity::getUserId, UserUtil.getUserInfo().getUserId())
                .isNull(DirEntity::getParentId));
        if (root == null) {
            throw new BizException("用户根目录初始化异常");
        }
        return root.getId();
    }

    private void createFileContent(Long dirId, String filename, MultipartFile file) {
        try {
            String filePath = localFileStorage.saveStream(file.getInputStream());
            String hash = DigestUtil.sha256Hex(file.getInputStream());
            
            ContentEntity entity = ContentEntity.builder()
                    .dirId(dirId)
                    .title(filename)
                    .type(FileUtil.extName(filename))
                    .filePath(filePath)
                    .contentHash(hash)
                    .creator(UserUtil.getUserInfo().getUserId())
                    .encrypted(true) // LocalFileStorage 默认加密
                    .build();
            
            contentRepository.insert(entity);
        } catch (IOException e) {
            throw new BizException("文件读取失败");
        }
    }

    private void updateFileContent(ContentEntity entity, MultipartFile file) {
        try {
            // 重新计算 Hash
            String newHash = DigestUtil.sha256Hex(file.getInputStream());
            // 如果内容不同，才进行实际的 IO 操作
            if (!newHash.equals(entity.getContentHash())) {
                // 删除旧文件
                if (StrUtil.isNotBlank(entity.getFilePath())) {
                    localFileStorage.deleteFile(entity.getFilePath());
                }
                // 保存新文件 (流是一次性的，需要重新获取)
                String newPath = localFileStorage.saveStream(file.getInputStream());
                
                entity.setFilePath(newPath);
                entity.setContentHash(newHash);
                entity.setType(FileUtil.extName(file.getOriginalFilename()));
                contentRepository.updateById(entity);
            } else {
                log.info("文件内容无变化，跳过IO操作: {}", entity.getTitle());
            }
        } catch (IOException e) {
            throw new BizException("文件更新失败");
        }
    }
}