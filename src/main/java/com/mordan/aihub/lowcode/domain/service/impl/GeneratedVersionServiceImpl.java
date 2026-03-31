package com.mordan.aihub.lowcode.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.lowcode.domain.entity.GeneratedVersion;
import com.mordan.aihub.lowcode.mapper.GeneratedVersionMapper;
import com.mordan.aihub.lowcode.domain.service.ApplicationService;
import com.mordan.aihub.lowcode.domain.service.GenerationTaskService;
import com.mordan.aihub.lowcode.domain.service.GeneratedVersionService;
import com.mordan.aihub.lowcode.infrastructure.storage.FileStorageService;
import com.mordan.aihub.lowcode.web.request.GenerateRequest;
import com.mordan.aihub.lowcode.web.vo.TaskVO;
import com.mordan.aihub.lowcode.web.vo.VersionVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    @Override
    public List<VersionVO> listVersions(Long userId, Long appId) {
        // 鉴权
        applicationService.getAppDetail(userId, appId);

        List<GeneratedVersion> versions = this.lambdaQuery()
                .eq(GeneratedVersion::getAppId, appId)
                .orderByDesc(GeneratedVersion::getUpdatedAt)
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
                .previewUrl(version.getPreviewUrl())
                .downloadUrl(version.getDownloadUrl())
                .projectSummary(version.getProjectSummary())
                .fileSize(version.getFileSize())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .build();
    }
}
