package com.mordan.aihub.lowcode.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.lowcode.config.DeployProperties;
import com.mordan.aihub.lowcode.domain.entity.GeneratedVersion;
import com.mordan.aihub.lowcode.domain.enums.VersionDeployStatus;
import com.mordan.aihub.lowcode.mapper.GeneratedVersionMapper;
import com.mordan.aihub.lowcode.domain.service.ApplicationService;
import com.mordan.aihub.lowcode.domain.service.GenerationTaskService;
import com.mordan.aihub.lowcode.domain.service.GeneratedVersionService;
import com.mordan.aihub.lowcode.infrastructure.deploy.SlugGenerator;
import com.mordan.aihub.lowcode.infrastructure.deploy.SlugRoutingTable;
import com.mordan.aihub.lowcode.infrastructure.storage.FileStorageService;
import com.mordan.aihub.lowcode.web.request.GenerateRequest;
import com.mordan.aihub.lowcode.web.vo.DeployResultVO;
import com.mordan.aihub.lowcode.web.vo.DeployStatusVO;
import com.mordan.aihub.lowcode.web.vo.TaskVO;
import com.mordan.aihub.lowcode.web.vo.VersionVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 生成版本服务实现
 */
@Slf4j
@Service
public class GeneratedVersionServiceImpl extends ServiceImpl<GeneratedVersionMapper, GeneratedVersion>
        implements GeneratedVersionService {

    @Resource
    private ApplicationService applicationService;
    @Resource
    private GenerationTaskService generationTaskService;
    @Resource
    private FileStorageService fileStorageService;
    @Resource
    private SlugGenerator slugGenerator;
    @Resource
    private SlugRoutingTable slugRoutingTable;
    @Resource
    private DeployProperties deployProperties;

    @Override
    public List<VersionVO> listVersions(Long userId, Long appId) {
        // 鉴权
        applicationService.getAppDetail(userId, appId);

        List<GeneratedVersion> versions = this.lambdaQuery()
                .eq(GeneratedVersion::getAppId, appId)
                .orderByDesc(GeneratedVersion::getVersionNumber)
                .list();
        return versions.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public VersionVO getVersionDetail(Long userId, Long appId, Long versionId) {
        GeneratedVersion version = checkAndGetVersion(userId, appId, versionId);
        return toVO(version);
    }

    @Override
    public TaskVO rollbackToVersion(Long userId, Long appId, Long versionId) {
        // 鉴权
        GeneratedVersion version = checkAndGetVersion(userId, appId, versionId);

        // 构造生成请求，基于此版本重新生成
        GenerateRequest request = new GenerateRequest();
        request.setPrompt(version.getPromptSnapshot());
        request.setBaseVersionId(versionId);

        // 提交新生成任务
        return generationTaskService.submitGenerateTask(userId, appId, request);
    }

    @Override
    public ResponseEntity<org.springframework.core.io.Resource> downloadVersion(Long userId, Long appId, Long versionId) {
        GeneratedVersion version = checkAndGetVersion(userId, appId, versionId);

        try {
            org.springframework.core.io.ByteArrayResource zipResource = fileStorageService.packageAsZip(version.getCodeStoragePath());
            String filename = String.format("app-v%d.zip", version.getVersionNumber());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipResource.contentLength())
                    .body(zipResource);
        } catch (IOException e) {
            throw new RuntimeException("打包失败：" + e.getMessage(), e);
        }
    }

    @Override
    public DeployResultVO deployVersion(Long userId, Long appId, Long versionId) {
        // 1. 鉴权
        GeneratedVersion version = checkAndGetVersion(userId, appId, versionId);

        // 2. 如果已部署，直接返回已有信息
        if (version.getDeployStatus() == VersionDeployStatus.DEPLOYED) {
            return DeployResultVO.builder()
                    .deploySlug(version.getDeploySlug())
                    .deployUrl(version.getDeployUrl())
                    .deployedAt(version.getDeployedAt())
                    .build();
        }

        // 3. 下线该应用其他已部署版本
        List<GeneratedVersion> deployedVersions = this.lambdaQuery()
                .eq(GeneratedVersion::getAppId, appId)
                .eq(GeneratedVersion::getDeployStatus, VersionDeployStatus.DEPLOYED)
                .list();
        for (GeneratedVersion deployed : deployedVersions) {
            undeployInternal(deployed);
        }

        // 4. 生成唯一slug
        String slug = slugGenerator.generateUnique();

        // 5. 构造部署URL
        String baseUrl = deployProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:" + deployProperties.getPort();
        }
        String deployUrl = baseUrl.endsWith("/")
                ? baseUrl + "site/" + slug
                : baseUrl + "/site/" + slug;

        // 6. 更新数据库
        long now = System.currentTimeMillis();
        this.lambdaUpdate()
                .eq(GeneratedVersion::getId, versionId)
                .set(GeneratedVersion::getDeployStatus, VersionDeployStatus.DEPLOYED)
                .set(GeneratedVersion::getDeploySlug, slug)
                .set(GeneratedVersion::getDeployUrl, deployUrl)
                .set(GeneratedVersion::getDeployedAt, now)
                .update();

        // 7. 注册到路由表
        Path storagePath = Paths.get(version.getCodeStoragePath()).toAbsolutePath().normalize();
        slugRoutingTable.register(slug, storagePath);

        log.info("Version deployed: versionId={}, slug={}, deployUrl={}", versionId, slug, deployUrl);

        // 8. 返回结果
        return DeployResultVO.builder()
                .deploySlug(slug)
                .deployUrl(deployUrl)
                .deployedAt(now)
                .build();
    }

    @Override
    public void undeployVersion(Long userId, Long appId, Long versionId) {
        // 鉴权
        GeneratedVersion version = checkAndGetVersion(userId, appId, versionId);
        undeployInternal(version);
    }

    /**
     * 内部下线方法，不做鉴权（调用者已鉴权）
     */
    public void undeployInternal(GeneratedVersion version) {
        if (version.getDeployStatus() != VersionDeployStatus.DEPLOYED) {
            return;
        }
        // 从路由表移除
        slugRoutingTable.remove(version.getDeploySlug());
        // 更新数据库
        this.lambdaUpdate()
                .eq(GeneratedVersion::getId, version.getId())
                .set(GeneratedVersion::getDeployStatus, VersionDeployStatus.UNDEPLOYED)
                .set(GeneratedVersion::getUndeployedAt, System.currentTimeMillis())
                .update();
        log.info("Version undeployed: versionId={}, slug={}", version.getId(), version.getDeploySlug());
    }

    @Override
    public DeployStatusVO getDeployStatus(Long userId, Long appId, Long versionId) {
        GeneratedVersion version = checkAndGetVersion(userId, appId, versionId);
        return DeployStatusVO.builder()
                .versionId(versionId)
                .deployStatus(version.getDeployStatus())
                .deployUrl(version.getDeployUrl())
                .deployedAt(version.getDeployedAt())
                .undeployedAt(version.getUndeployedAt())
                .build();
    }

    /**
     * 鉴权并获取版本
     */
    private GeneratedVersion checkAndGetVersion(Long userId, Long appId, Long versionId) {
        applicationService.getAppDetail(userId, appId);
        GeneratedVersion version = this.lambdaQuery()
                .eq(GeneratedVersion::getId, versionId)
                .eq(GeneratedVersion::getAppId, appId)
                .one();
        if (version == null) {
            throw new RuntimeException("版本不存在或无权访问");
        }
        return version;
    }

    /**
     * 转换为VO
     */
    private VersionVO toVO(GeneratedVersion version) {
        return VersionVO.builder()
                .id(version.getId())
                .appId(version.getAppId())
                .versionNumber(version.getVersionNumber())
                .previewUrl(version.getPreviewUrl())
                .downloadUrl(version.getDownloadUrl())
                .deployStatus(version.getDeployStatus())
                .deployUrl(version.getDeployUrl())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
