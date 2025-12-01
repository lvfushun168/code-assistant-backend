package com.lfs.codeassistantbackend.service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import com.lfs.codeassistantbackend.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class LocalFileStorage {

    @Value("${app.file.storage-path}")
    private String storageRootPath;

    @Value("${app.file.aes-key}")
    private String aesKeyStr;

    private AES aes;

    @PostConstruct
    public void init() {
        byte[] keyBytes = DigestUtil.sha256(aesKeyStr);
        this.aes = new AES(Mode.ECB, Padding.PKCS5Padding, keyBytes);
        if (!FileUtil.exist(storageRootPath)) {
            FileUtil.mkdir(storageRootPath);
        }
    }


    /**
     * 流式保存并加密
     */
    public String saveStream(InputStream inputStream) {
        // 生成路径
        String datePath = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
        String fileName = IdUtil.fastSimpleUUID() + ".lfs";
        String relativePath = datePath + File.separator + fileName;
        File targetFile = FileUtil.file(storageRootPath, relativePath);

        // 确保父目录存在
        FileUtil.touch(targetFile);

        // 流式加密写入
        try (OutputStream out = FileUtil.getOutputStream(targetFile)) {
            aes.encrypt(inputStream, out, true);
        } catch (Exception e) {
            log.error("文件写入失败!", e);
            throw new BizException("文件写入失败");
        }

        return relativePath;
    }

    /**
     * 保存内容
     * @param content 明文内容
     * @return 相对路径 (如: 2025/11/21/uuid.lfs)
     */
    public String saveContent(String content) {
        // 生成相对路径策略：yyyy/MM/dd/UUID.lfs
        String datePath = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
        String fileName = IdUtil.fastSimpleUUID() + ".lfs";
        String relativePath = datePath + File.separator + fileName;
        
        // 拼接绝对路径
        File targetFile = FileUtil.file(storageRootPath, relativePath);

        // 加密并写入文件
        FileUtil.writeBytes(aes.encrypt(content), targetFile);

        return relativePath;
    }

    /**
     * 读取内容
     * @param relativePath 数据库存的相对路径
     * @return 明文内容
     */
    public String readContent(String relativePath) {
        File file = FileUtil.file(storageRootPath, relativePath);
        if (!FileUtil.exist(file)) {
            throw new BizException("文件不存在: " + relativePath);
        }
        // 读取字节并解密
        return aes.decryptStr(FileUtil.readBytes(file));
    }

    /**
     * 删除文件
     */
    public void deleteFile(String relativePath) {
        FileUtil.del(FileUtil.file(storageRootPath, relativePath));
    }
}