package com.bilibili.storage;

import com.bilibili.config.properties.StorageProperties;
import com.bilibili.tool.StringTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocalStorageService implements StorageService {

    private final StorageProperties storageProperties;

    @Autowired
    public LocalStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public StoredFile saveAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("avatar file is empty");
        }
        if (file.getSize() > storageProperties.getAvatarMaxSize()) {
            throw new IllegalArgumentException("avatar file is too large");
        }

        String contentType = StringTool.normalizeOptional(file.getContentType());
        if (contentType == null || !getAllowedImageTypes().contains(contentType)) {
            throw new IllegalArgumentException("avatar content type is not allowed");
        }

        LocalDate today = LocalDate.now();
        String relativeDir = String.format("%s/%d/%02d/%02d",
                storageProperties.getAvatarSubDir(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth());
        String extension = resolveFileExtension(file.getOriginalFilename(), contentType);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        Path rootPath = Paths.get(storageProperties.getRootDir()).toAbsolutePath().normalize();
        Path targetDir = rootPath.resolve(relativeDir).normalize();
        Path targetFile = targetDir.resolve(fileName).normalize();
        if (!targetFile.startsWith(rootPath)) {
            throw new IllegalArgumentException("invalid target path");
        }

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile.toFile());
            String publicUrl = buildPublicUrl(relativeDir + "/" + fileName);
            return new StoredFile(publicUrl);
        } catch (IOException e) {
            throw new RuntimeException("save avatar file failed", e);
        }
    }

    @Override
    public void deleteByPublicUrl(String publicUrl) {
        String normalizedBaseUrl = StringTool.trimTrailingSlash(storageProperties.getPublicBaseUrl());
        if (StringTool.isBlank(publicUrl) || StringTool.isBlank(normalizedBaseUrl) || !publicUrl.startsWith(normalizedBaseUrl)) {
            return;
        }
        String relativePath = publicUrl.substring(normalizedBaseUrl.length()).replaceFirst("^/+", "");
        if (relativePath.isEmpty()) {
            return;
        }
        Path rootPath = Paths.get(storageProperties.getRootDir()).toAbsolutePath().normalize();
        Path filePath = rootPath.resolve(relativePath).normalize();
        if (!filePath.startsWith(rootPath)) {
            return;
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignore) {
            // ignore cleanup exception
        }
    }

    private String buildPublicUrl(String relativePath) {
        String baseUrl = StringTool.trimTrailingSlash(storageProperties.getPublicBaseUrl());
        String normalizedPath = relativePath.replace("\\", "/").replaceFirst("^/+", "");
        return baseUrl + "/" + normalizedPath;
    }

    private List<String> getAllowedImageTypes() {
        String csv = storageProperties.getAllowedImageTypes();
        if (StringTool.isBlank(csv)) {
            return java.util.Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    private String resolveFileExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int index = originalFilename.lastIndexOf('.');
            if (index >= 0 && index < originalFilename.length() - 1) {
                String ext = originalFilename.substring(index).toLowerCase();
                if (".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext)) {
                    return ext;
                }
            }
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

}
