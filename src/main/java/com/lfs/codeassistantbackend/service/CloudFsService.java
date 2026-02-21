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
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
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
     * 上传 ZIP 文件并解压到云端目录（保留目录结构）
     * @param zipFile 上传的 ZIP 文件
     * @param destDir 云端目标目录路径 (如:/MyProject)
     * @param override 是否覆盖已存在的文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void uploadZip(MultipartFile zipFile, String destDir, Boolean override) {
        Long rootDirId = getRootDirId();
        Long targetDirId = resolveDirId(rootDirId, destDir);

        try {
            // 创建临时文件用于解压缩
            File tempZipFile = File.createTempFile("upload_", ".zip");
            zipFile.transferTo(tempZipFile);

            try (java.util.zip.ZipFile zipFileObj = new java.util.zip.ZipFile(tempZipFile, StandardCharsets.UTF_8)) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFileObj.entries();
                
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    
                    // 跳过目录本身（只处理文件）
                    if (entry.isDirectory()) {
                        continue;
                    }
                    
                    // 安全检查：防止路径穿越攻击
                    validateZipEntry(entryName, targetDirId);
                    
                    // 处理文件路径
                    String[] parts = entryName.split("/");
                    String fileName = parts[parts.length - 1];
                    
                    // 计算相对于目标目录的子路径
                    String relativePath = "";
                    if (parts.length > 1) {
                        relativePath = String.join("/", Arrays.copyOf(parts, parts.length - 1));
                    }
                    
                    // 递归创建目录结构
                    Long currentDirId = targetDirId;
                    if (StrUtil.isNotBlank(relativePath)) {
                        currentDirId = createDirStructure(rootDirId, targetDirId, relativePath);
                    }
                    
                    // 读取 ZIP 中的文件内容
                    try (InputStream zipInputStream = zipFileObj.getInputStream(entry);
                         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }
                        byte[] fileContent = baos.toByteArray();
                        
                        // 检查文件是否已存在
                        ContentEntity existingFile = contentRepository.selectOne(new LambdaQueryWrapper<ContentEntity>()
                                .eq(ContentEntity::getDirId, currentDirId)
                                .eq(ContentEntity::getTitle, fileName));
                        
                        if (existingFile != null) {
                            if (!override) {
                                log.info("文件已存在，跳过: {}", entryName);
                                continue;
                            }
                            // 覆盖已存在的文件
                            updateFileContentFromBytes(existingFile, fileContent, fileName);
                        } else {
                            // 创建新文件
                            createFileContentFromBytes(currentDirId, fileName, fileContent);
                        }
                    }
                }
            } finally {
                // 删除临时文件
                tempZipFile.delete();
            }
        } catch (IOException e) {
            log.error("ZIP解压失败", e);
            throw new BizException("ZIP解压失败: " + e.getMessage());
        }
    }

    /**
     * 上传 ZIP 文件并解压到云端目录（通过目录ID）
     * @param zipFile 上传的 ZIP 文件
     * @param targetDirId 云端目标目录ID
     * @param override 是否覆盖已存在的文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void uploadZipById(MultipartFile zipFile, Long targetDirId, Boolean override) {
        // 验证目录属于当前用户
        DirEntity targetDir = dirRepository.selectById(targetDirId);
        if (targetDir == null) {
            throw new BizException("目标目录不存在");
        }
        if (!targetDir.getUserId().equals(UserUtil.getUserInfo().getUserId())) {
            throw new BizException("无权访问此目录");
        }

        uploadZipToDirectory(zipFile, targetDirId, override);
    }

    /**
     * 内部方法：上传 ZIP 到指定目录
     */
    private void uploadZipToDirectory(MultipartFile zipFile, Long targetDirId, Boolean override) {
        try {
            // 创建临时文件用于解压缩
            File tempZipFile = File.createTempFile("upload_", ".zip");
            zipFile.transferTo(tempZipFile);

            try (java.util.zip.ZipFile zipFileObj = new java.util.zip.ZipFile(tempZipFile, StandardCharsets.UTF_8)) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFileObj.entries();
                
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    
                    // 跳过目录本身（只处理文件）
                    if (entry.isDirectory()) {
                        continue;
                    }
                    
                    // 安全检查：防止路径穿越攻击
                    validateZipEntry(entryName, targetDirId);
                    
                    // 处理文件路径
                    String[] parts = entryName.split("/");
                    String fileName = parts[parts.length - 1];
                    
                    // 计算相对于目标目录的子路径
                    String relativePath = "";
                    if (parts.length > 1) {
                        relativePath = String.join("/", Arrays.copyOf(parts, parts.length - 1));
                    }
                    
                    // 递归创建目录结构
                    Long currentDirId = targetDirId;
                    if (StrUtil.isNotBlank(relativePath)) {
                        currentDirId = createDirStructure(targetDirId, targetDirId, relativePath);
                    }
                    
                    // 读取 ZIP 中的文件内容
                    try (InputStream zipInputStream = zipFileObj.getInputStream(entry);
                         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }
                        byte[] fileContent = baos.toByteArray();
                        
                        // 检查文件是否已存在
                        ContentEntity existingFile = contentRepository.selectOne(new LambdaQueryWrapper<ContentEntity>()
                                .eq(ContentEntity::getDirId, currentDirId)
                                .eq(ContentEntity::getTitle, fileName));
                        
                        if (existingFile != null) {
                            if (!override) {
                                log.info("文件已存在，跳过: {}", entryName);
                                continue;
                            }
                            // 覆盖已存在的文件
                            updateFileContentFromBytes(existingFile, fileContent, fileName);
                        } else {
                            // 创建新文件
                            createFileContentFromBytes(currentDirId, fileName, fileContent);
                        }
                    }
                }
            } finally {
                // 删除临时文件
                tempZipFile.delete();
            }
        } catch (IOException e) {
            log.error("ZIP解压失败", e);
            throw new BizException("ZIP解压失败: " + e.getMessage());
        }
    }

    /**
     * 验证 ZIP 条目名称，防止路径穿越攻击
     */
    private void validateZipEntry(String entryName, Long targetDirId) throws IOException {
        // 标准化路径分隔符
        String normalizedName = entryName.replace("\\", "/");
        
        // 禁止绝对路径
        if (normalizedName.startsWith("/")) {
            throw new BizException("禁止绝对路径: " + entryName);
        }
        
        // 禁止路径穿越 (..)
        if (normalizedName.contains("..")) {
            throw new BizException("禁止路径穿越: " + entryName);
        }
        
        // 验证最终路径在目标目录内
        // 注意：由于我们是通过数据库存储的，这里主要验证名称合法性
    }

    /**
     * 创建目录结构（如果不存在）
     * @param rootDirId 用户根目录ID
     * @param parentId 父目录ID
     * @param relativePath 相对路径（如 "src/main/java"）
     * @return 最终目录的ID
     */
    private Long createDirStructure(Long rootDirId, Long parentId, String relativePath) {
        String[] dirs = relativePath.split("/");
        Long currentDirId = parentId;
        
        for (String dirName : dirs) {
            if (StrUtil.isBlank(dirName)) continue;
            
            // 查找目录是否已存在
            DirEntity existingDir = dirRepository.selectOne(new LambdaQueryWrapper<DirEntity>()
                    .eq(DirEntity::getParentId, currentDirId)
                    .eq(DirEntity::getName, dirName)
                    .eq(DirEntity::getUserId, UserUtil.getUserInfo().getUserId()));
            
            if (existingDir != null) {
                currentDirId = existingDir.getId();
            } else {
                // 创建新目录
                DirEntity newDir = DirEntity.builder()
                        .name(dirName)
                        .parentId(currentDirId)
                        .userId(UserUtil.getUserInfo().getUserId())
                        .build();
                dirRepository.insert(newDir);
                currentDirId = newDir.getId();
            }
        }
        return currentDirId;
    }

    /**
     * 从字节数组创建文件内容
     */
    private void createFileContentFromBytes(Long dirId, String filename, byte[] content) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            String filePath = localFileStorage.saveStream(bais);
            String hash = DigestUtil.sha256Hex(content);
            
            ContentEntity entity = ContentEntity.builder()
                    .dirId(dirId)
                    .title(filename)
                    .type(FileUtil.extName(filename))
                    .filePath(filePath)
                    .contentHash(hash)
                    .creator(UserUtil.getUserInfo().getUserId())
                    .encrypted(true)
                    .build();
            
            contentRepository.insert(entity);
        } catch (IOException e) {
            throw new BizException("文件读取失败");
        }
    }

    /**
     * 从字节数组更新文件内容
     */
    private void updateFileContentFromBytes(ContentEntity entity, byte[] content, String filename) {
        String newHash = DigestUtil.sha256Hex(content);
        
        if (!newHash.equals(entity.getContentHash())) {
            // 删除旧文件
            if (StrUtil.isNotBlank(entity.getFilePath())) {
                localFileStorage.deleteFile(entity.getFilePath());
            }
            // 保存新文件
            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                String newPath = localFileStorage.saveStream(bais);
                entity.setFilePath(newPath);
                entity.setContentHash(newHash);
                entity.setType(FileUtil.extName(filename));
                contentRepository.updateById(entity);
            } catch (IOException e) {
                throw new BizException("文件更新失败");
            }
        } else {
            log.info("文件内容无变化，跳过IO操作: {}", entity.getTitle());
        }
    }

    /**
     * 下载目录为 ZIP 文件
     * @param path 云端目录路径 (如:/MyProject)
     * @param response HTTP响应
     */
    public void downloadZip(String path, HttpServletResponse response) {
        Long rootDirId = getRootDirId();
        
        // 解析目录实体
        DirEntity dir = resolveDirEntity(rootDirId, path);
        
        downloadZipInternal(dir, response);
    }

    /**
     * 根据目录ID下载目录为 ZIP 文件
     * @param dirId 云端目录ID
     * @param response HTTP响应
     */
    public void downloadZipById(Long dirId, HttpServletResponse response) {
        // 验证目录属于当前用户
        DirEntity dir = dirRepository.selectById(dirId);
        if (dir == null) {
            throw new BizException("目录不存在");
        }
        if (!dir.getUserId().equals(UserUtil.getUserInfo().getUserId())) {
            throw new BizException("无权访问此目录");
        }
        
        downloadZipInternal(dir, response);
    }

    /**
     * 内部方法：执行 ZIP 下载
     */
    private void downloadZipInternal(DirEntity dir, HttpServletResponse response) {
        try {
            // 创建临时 ZIP 文件
            File tempZipFile = File.createTempFile("download_", ".zip");
            
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                    new FileOutputStream(tempZipFile), StandardCharsets.UTF_8)) {
                
                // 递归添加目录内容到 ZIP
                addDirToZip(zos, dir, "");
            }
            
            // 设置响应头
            response.reset();
            response.setContentType("application/zip");
            response.setCharacterEncoding("utf-8");
            String zipName = dir.getName() + ".zip";
            String encodedName = URLEncoder.encode(zipName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encodedName);
            
            // 写入响应流
            try (FileInputStream fis = new FileInputStream(tempZipFile);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }
            
            // 删除临时文件
            tempZipFile.delete();
            
        } catch (IOException e) {
            log.error("ZIP压缩失败", e);
            throw new BizException("ZIP压缩失败: " + e.getMessage());
        }
    }

    /**
     * 解析目录实体
     */
    private DirEntity resolveDirEntity(Long rootDirId, String path) {
        path = StrUtil.trim(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        if (StrUtil.isBlank(path)) {
            // 根目录
            return dirRepository.selectById(rootDirId);
        }
        
        List<String> parts = Arrays.stream(path.split("/"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        
        Long currentDirId = rootDirId;
        
        for (String dirName : parts) {
            DirEntity dir = dirRepository.selectOne(new LambdaQueryWrapper<DirEntity>()
                    .eq(DirEntity::getParentId, currentDirId)
                    .eq(DirEntity::getName, dirName)
                    .eq(DirEntity::getUserId, UserUtil.getUserInfo().getUserId()));
            
            if (dir == null) {
                throw new BizException("云端目录不存在: " + dirName);
            }
            currentDirId = dir.getId();
        }
        
        return dirRepository.selectById(currentDirId);
    }

    /**
     * 递归添加目录内容到 ZIP
     */
    private void addDirToZip(java.util.zip.ZipOutputStream zos, DirEntity dir, String parentPath) throws IOException {
        String currentPath = parentPath.isEmpty() ? dir.getName() : parentPath + "/" + dir.getName();
        
        // 添加当前目录下的文件
        List<ContentEntity> contents = contentRepository.selectList(new LambdaQueryWrapper<ContentEntity>()
                .eq(ContentEntity::getDirId, dir.getId()));
        
        for (ContentEntity content : contents) {
            // 使用 type 作为扩展名，如果 type 为空则默认为 txt
            String extension = content.getType();
            if (StrUtil.isBlank(extension)) {
                extension = "txt";
            }
            
            // 构建文件名：如果 title 已包含扩展名则直接使用，否则添加扩展名
            String fileName = content.getTitle();
            if (!fileName.contains(".")) {
                fileName = fileName + "." + extension;
            }
            
            String entryName = currentPath + "/" + fileName;
            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(entryName);
            zos.putNextEntry(zipEntry);
            
            // 读取文件内容并写入 ZIP
            try {
                byte[] fileContent = localFileStorage.readContent(content.getFilePath()).getBytes(StandardCharsets.UTF_8);
                zos.write(fileContent);
            } catch (Exception e) {
                log.error("读取文件失败: {}", content.getTitle(), e);
            }
            zos.closeEntry();
        }
        
        // 递归处理子目录
        List<DirEntity> children = dirRepository.selectList(new LambdaQueryWrapper<DirEntity>()
                .eq(DirEntity::getParentId, dir.getId()));
        
        for (DirEntity child : children) {
            addDirToZip(zos, child, currentPath);
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