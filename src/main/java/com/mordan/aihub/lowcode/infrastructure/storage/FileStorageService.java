package com.mordan.aihub.lowcode.infrastructure.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.config.StorageProperties;
import com.mordan.aihub.lowcode.domain.entity.GeneratedVersion;
import com.mordan.aihub.lowcode.mapper.GeneratedVersionMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件存储服务
 * 负责生成代码的文件写入、读取、打包操作
 */
@Slf4j
@Component
public class FileStorageService {

    @Resource
    private StorageProperties storageProperties;
    @Resource
    private GeneratedVersionMapper generatedVersionMapper;
    @Resource
    private ObjectMapper objectMapper;

    private Path rootPath;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.rootPath = Paths.get(storageProperties.getRoot()).toAbsolutePath().normalize();
        // 确保根目录存在
        if (!Files.exists(rootPath)) {
            try {
                Files.createDirectories(rootPath);
                log.info("File storage root directory created: {}", rootPath);
            } catch (IOException e) {
                log.error("Failed to create storage root directory", e);
            }
        }
    }

    /**
     * 写入版本代码到文件系统
     * @param userId 用户ID
     * @param appId 应用ID
     * @param filesJson 代码文件JSON，格式：{"files":[{"path":"src/App.jsx","content":"..."}]}
     * @return 版本目录的绝对路径
     * @throws RuntimeException 写入失败时抛出
     */
    public String writeVersion(Long userId, Long appId, String filesJson) {
        Path versionDir = getVersionDir(userId, appId);
        try {
            // 如果目录已存在，先删除旧文件
            if (Files.exists(versionDir)) {
                deleteVersion(versionDir.toString());
            }
            Files.createDirectories(versionDir);
            JsonNode root = objectMapper.readTree(filesJson);
            JsonNode filesNode = root.get("files");
            if (filesNode == null || !filesNode.isArray()) {
                throw new IllegalArgumentException("Invalid filesJson format: missing 'files' array");
            }

            for (JsonNode fileNode : filesNode) {
                String filePath = fileNode.get("path").asText();
                String content = fileNode.get("content").asText();
                Path resolvedPath = resolvePath(versionDir, filePath);
                // 确保父目录存在
                if (resolvedPath.getParent() != null && !Files.exists(resolvedPath.getParent())) {
                    Files.createDirectories(resolvedPath.getParent());
                }
                Files.writeString(resolvedPath, content);
            }

            log.info("Wrote {} files to {}", filesNode.size(), versionDir);
            return versionDir.toString();
        } catch (IOException e) {
            log.error("Failed to write version files", e);
            throw new RuntimeException("Failed to write version files", e);
        }
    }

    /**
     * 根据版本ID和相对路径解析文件路径
     * @param versionId 版本ID
     * @param relativePath 相对路径
     * @return 解析后的完整路径，null表示版本不存在
     */
    public Path resolveFilePath(Long versionId, String relativePath) {
        GeneratedVersion version = generatedVersionMapper.selectById(versionId);
        if (version == null || version.getCodeStoragePath() == null) {
            return null;
        }
        Path versionRoot = Paths.get(version.getCodeStoragePath()).toAbsolutePath().normalize();
        Path resolved = versionRoot.resolve(relativePath).normalize();
        // 路径穿越防护
        if (!resolved.startsWith(versionRoot)) {
            log.warn("Path traversal attempt detected: versionId={}, relativePath={}", versionId, relativePath);
            return null;
        }
        return resolved;
    }

    /**
     * 获取版本根目录
     * @param versionId 版本ID
     * @return 版本根目录Path，null表示版本不存在
     */
    public Path getVersionRoot(Long versionId) {
        GeneratedVersion version = generatedVersionMapper.selectById(versionId);
        if (version == null || version.getCodeStoragePath() == null) {
            return null;
        }
        return Paths.get(version.getCodeStoragePath()).toAbsolutePath().normalize();
    }

    /**
     * 将指定版本目录打包为ZIP
     * @param storagePath 版本存储目录路径
     * @return ZIP字节数组包装为ByteArrayResource
     */
    public ByteArrayResource packageAsZip(String storagePath) throws IOException {
        Path sourceDir = Paths.get(storagePath).toAbsolutePath().normalize();
        if (!Files.exists(sourceDir)) {
            throw new IllegalArgumentException("Source directory does not exist: " + storagePath);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Files.walk(sourceDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String entryName = sourceDir.relativize(path).toString().replace('\\', '/');
                            ZipEntry entry = new ZipEntry(entryName);
                            zos.putNextEntry(entry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to add entry to ZIP: " + path, e);
                        }
                    });
        }

        byte[] zipBytes = baos.toByteArray();
        log.info("Packaged {} bytes to ZIP from {}", zipBytes.length, storagePath);
        return new ByteArrayResource(zipBytes);
    }

    /**
     * 递归删除版本目录
     * @param storagePath 版本存储目录路径
     */
    public void deleteVersion(String storagePath) {
        Path versionDir = Paths.get(storagePath).toAbsolutePath().normalize();
        if (!Files.exists(versionDir)) {
            return;
        }
        try {
            Files.walk(versionDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete file: {}", path, e);
                        }
                    });
            log.info("Deleted version directory: {}", versionDir);
        } catch (IOException e) {
            log.error("Failed to delete version directory", e);
        }
    }

    /**
     * 获取版本目录路径（每个 appId 对应一个固定目录）
     */
    private Path getVersionDir(Long userId, Long appId) {
        return rootPath.resolve(userId.toString()).resolve(appId.toString()).normalize();
    }

    /**
     * 解析路径并做安全校验
     */
    private Path resolvePath(Path parent, String relativePath) {
        Path resolved = parent.resolve(relativePath).normalize();
        Assert.state(resolved.startsWith(parent), "Path traversal detected: " + relativePath);
        return resolved;
    }
}
