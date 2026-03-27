package com.mordan.aihub.lowcode.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mordan.aihub.lowcode.domain.entity.GeneratedVersion;
import com.mordan.aihub.lowcode.web.vo.DeployResultVO;
import com.mordan.aihub.lowcode.web.vo.DeployStatusVO;
import com.mordan.aihub.lowcode.web.vo.TaskVO;
import com.mordan.aihub.lowcode.web.vo.VersionVO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 生成版本服务接口
 */
public interface GeneratedVersionService extends IService<GeneratedVersion> {

    /**
     * 查询应用版本列表（带鉴权）
     * @param userId 用户ID
     * @param appId 应用ID
     * @return 版本VO列表
     */
    List<VersionVO> listVersions(Long userId, Long appId);

    /**
     * 获取版本详情（带鉴权）
     * @param userId 用户ID
     * @param appId 应用ID
     * @param versionId 版本ID
     * @return 版本VO
     */
    VersionVO getVersionDetail(Long userId, Long appId, Long versionId);

    /**
     * 回滚到指定版本，以此为基础重新提交生成任务
     * @param userId 用户ID
     * @param appId 应用ID
     * @param versionId 目标版本ID
     * @return 新生成任务VO
     */
    TaskVO rollbackToVersion(Long userId, Long appId, Long versionId);

    /**
     * 下载版本ZIP包（带鉴权）
     * @param userId 用户ID
     * @param appId 应用ID
     * @param versionId 版本ID
     * @return ZIP包响应
     */
    ResponseEntity<Resource> downloadVersion(Long userId, Long appId, Long versionId);

    /**
     * 部署版本（带鉴权）
     * @param userId 用户ID
     * @param appId 应用ID
     * @param versionId 版本ID
     * @return 部署结果
     */
    DeployResultVO deployVersion(Long userId, Long appId, Long versionId);

    /**
     * 下线版本（带鉴权）
     * @param userId 用户ID
     * @param appId 应用ID
     * @param versionId 版本ID
     */
    void undeployVersion(Long userId, Long appId, Long versionId);

    /**
     * 获取部署状态（带鉴权）
     * @param userId 用户ID
     * @param appId 应用ID
     * @param versionId 版本ID
     * @return 部署状态
     */
    DeployStatusVO getDeployStatus(Long userId, Long appId, Long versionId);
}
