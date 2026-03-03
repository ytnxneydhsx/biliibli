package com.bilibili.storage;

import com.bilibili.config.properties.StorageProperties;
import com.bilibili.tool.StringTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocalVideoUploadStorageService implements VideoUploadStorageService {

    private final StorageProperties storageProperties;

    @Autowired
    public LocalVideoUploadStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public long getMaxVideoSize() {
        return storageProperties.getVideoMaxSize();
    }

    @Override
    public boolean isAllowedVideoType(String contentType) {
        String normalizedType = StringTool.normalizeOptional(contentType);
        if (normalizedType == null) {
            return true;
        }
        return getAllowedVideoTypes().contains(normalizedType);
    }

    @Override
    public String buildTempRelativeDir(Long uid, String uploadId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }
        String normalizedUploadId = StringTool.normalizeRequired(uploadId, "uploadId");
        return String.format("tmp/%s/%d/%s", storageProperties.getVideoSubDir(), uid, normalizedUploadId);
    }

    @Override
    public String buildFinalVideoRelativePath(String originalFileName) {
        LocalDate today = LocalDate.now();
        String extension = resolveVideoExtension(originalFileName);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        return String.format("%s/%d/%02d/%02d/%s",
                storageProperties.getVideoSubDir(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                fileName);
    }

    @Override
    public String buildPublicUrl(String relativePath) {
        String baseUrl = StringTool.trimTrailingSlash(storageProperties.getPublicBaseUrl());
        String normalizedRelativePath = StringTool.normalizeRequired(relativePath, "relativePath")
                .replace("\\", "/")
                .replaceFirst("^/+", "");
        return baseUrl + "/" + normalizedRelativePath;
    }

    @Override
    public void createDirectory(String relativeDir) {
        Path directory = resolveFromRoot(relativeDir);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("create directory failed", e);
        }
    }

    @Override
    public void saveChunk(String tempRelativeDir, int index, MultipartFile file, long expectedChunkSize) {
        if (index < 0) {
            throw new IllegalArgumentException("chunk index is invalid");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("chunk file is empty");
        }
        if (expectedChunkSize <= 0) {
            throw new IllegalArgumentException("expected chunk size is invalid");
        }
        if (file.getSize() != expectedChunkSize) {
            throw new IllegalArgumentException("chunk size mismatch");
        }

        Path tempDir = resolveFromRoot(tempRelativeDir);
        Path chunkPath = tempDir.resolve(index + ".part").normalize();
        Path tmpChunkPath = tempDir.resolve(index + ".part.tmp").normalize();
        if (!chunkPath.startsWith(tempDir) || !tmpChunkPath.startsWith(tempDir)) {
            throw new IllegalArgumentException("invalid chunk target path");
        }

        try {
            Files.createDirectories(tempDir);
            if (Files.exists(chunkPath) && Files.size(chunkPath) == expectedChunkSize) {
                return;
            }
            Files.deleteIfExists(chunkPath);
            Files.deleteIfExists(tmpChunkPath);
            file.transferTo(tmpChunkPath.toFile());
            Files.move(tmpChunkPath, chunkPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("save chunk failed", e);
        }
    }

    @Override
    public List<Integer> listUploadedChunkIndexes(String tempRelativeDir) {
        Path tempDir = resolveFromRoot(tempRelativeDir);
        if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
            return Collections.emptyList();
        }

        List<Integer> indexes = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir, "*.part")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                int dotIndex = name.indexOf('.');
                if (dotIndex <= 0) {
                    continue;
                }
                String prefix = name.substring(0, dotIndex);
                try {
                    indexes.add(Integer.parseInt(prefix));
                } catch (NumberFormatException ignore) {
                    // ignore invalid part file name
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("read uploaded chunks failed", e);
        }
        Collections.sort(indexes);
        return indexes;
    }

    @Override
    public void mergeChunks(String tempRelativeDir, int totalChunks, String finalRelativePath) {
        if (totalChunks <= 0) {
            throw new IllegalArgumentException("total chunks is invalid");
        }
        Path tempDir = resolveFromRoot(tempRelativeDir);
        Path finalPath = resolveFromRoot(finalRelativePath);
        try {
            Files.createDirectories(finalPath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(
                    finalPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunkPath = tempDir.resolve(i + ".part").normalize();
                    if (!chunkPath.startsWith(tempDir) || !Files.exists(chunkPath)) {
                        throw new IllegalArgumentException("chunks are not complete");
                    }
                    Files.copy(chunkPath, outputStream);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("merge chunks failed", e);
        }
    }

    @Override
    public void cleanupTempDir(String tempRelativeDir) {
        Path tempDir = resolveFromRoot(tempRelativeDir);
        if (!Files.exists(tempDir)) {
            return;
        }
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignore) {
                            // ignore cleanup exception
                        }
                    });
        } catch (IOException ignore) {
            // ignore cleanup exception
        }
    }

    @Override
    public void deleteByRelativePath(String relativePath) {
        String normalizedPath = StringTool.normalizeOptional(relativePath);
        if (normalizedPath == null) {
            return;
        }
        Path filePath = resolveFromRoot(normalizedPath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignore) {
            // ignore cleanup exception
        }
    }

    private Path resolveFromRoot(String relativePath) {
        String normalizedRelativePath = StringTool.normalizeRequired(relativePath, "relativePath");
        Path root = Paths.get(storageProperties.getRootDir()).toAbsolutePath().normalize();
        Path resolved = root.resolve(normalizedRelativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("invalid storage path");
        }
        return resolved;
    }

    private List<String> getAllowedVideoTypes() {
        String csv = storageProperties.getAllowedVideoTypes();
        if (StringTool.isBlank(csv)) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    private static String resolveVideoExtension(String originalFileName) {
        if (originalFileName != null) {
            int index = originalFileName.lastIndexOf('.');
            if (index > 0 && index < originalFileName.length() - 1) {
                String ext = originalFileName.substring(index).toLowerCase();
                if (ext.matches("\\.[a-z0-9]{1,10}")) {
                    return ext;
                }
            }
        }
        return ".mp4";
    }
}

