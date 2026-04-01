package com.mordan.aihub.lowcode.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.lowcode.domain.entity.GeneratedRecord;
import com.mordan.aihub.lowcode.domain.service.GeneratedRecordService;
import com.mordan.aihub.lowcode.mapper.GeneratedRecordMapper;
import com.mordan.aihub.lowcode.web.vo.GenerateRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 生成版本服务实现
 */
@Slf4j
@Service
public class GeneratedRecordServiceImpl extends ServiceImpl<GeneratedRecordMapper, GeneratedRecord>
        implements GeneratedRecordService {


    @Override
    public GenerateRecordVO getGeneratedRecord(Long appId) {
        GeneratedRecord version = this.lambdaQuery()
                .eq(GeneratedRecord::getAppId, appId)
                .one();
        if (version == null) {
            throw new RuntimeException("应用生成记录不存在或无权访问");
        }
        return toVO(version);
    }

    /**
     * 转换为VO
     */
    private GenerateRecordVO toVO(GeneratedRecord version) {
        return GenerateRecordVO.builder()
                .id(version.getId())
                .appId(version.getAppId())
                .previewUrl(version.getPreviewUrl())
                .downloadUrl(version.getDownloadUrl())
                .projectSummary(version.getProjectSummary())
                .fileSize(version.getFileSize())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .codeStoragePath(version.getCodeStoragePath())
                .filePrefix(version.getFilePrefix())
                .build();
    }
}
