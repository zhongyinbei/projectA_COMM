package com.lab.service.impl;

import com.lab.common.BizException;
import com.lab.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    @Value("${lab.upload-dir}")
    private String uploadDir;

    @Override
    public String upload(MultipartFile file) {
        // DEMO-SIMPLIFIED: Demo 将图片保存在本地磁盘，仅接受常见图片后缀；生产环境应接入对象存储。
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(400, "文件大小不能超过 5MB");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException(400, "仅支持 png/jpg/jpeg/gif/webp 图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException(400, "上传文件必须是图片");
        }

        try {
            Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) {
                throw new BizException(400, "文件名不合法");
            }
            file.transferTo(target.toFile());
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new BizException(500, "图片保存失败");
        }
    }
}
