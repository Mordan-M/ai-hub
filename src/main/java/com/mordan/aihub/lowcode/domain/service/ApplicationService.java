package com.mordan.aihub.lowcode.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mordan.aihub.lowcode.domain.entity.Application;
import com.mordan.aihub.lowcode.web.request.CreateAppRequest;
import com.mordan.aihub.lowcode.web.request.UpdateAppRequest;
import com.mordan.aihub.lowcode.web.vo.AppVO;

import java.util.List;

/**
 * 应用服务接口
 */
public interface ApplicationService extends IService<Application> {

    /**
     * 创建新应用
     * @param userId 用户ID
     * @param req 创建请求
     * @return 应用VO
     */
    AppVO createApp(Long userId, CreateAppRequest req);

    /**
     * 查询用户的所有活跃应用
     * @param userId 用户ID
     * @return 应用VO列表
     */
    List<AppVO> listApps(Long userId);

    /**
     * 获取应用详情（带鉴权）
     * @param userId 用户ID
     * @param appId 应用ID
     * @return 应用VO
     * @throws RuntimeException 应用不存在或无权限时抛出
     */
    AppVO getAppDetail(Long userId, Long appId);

    /**
     * 更新应用信息（带鉴权）
     * @param userId 用户ID
     * @param appId 应用ID
     * @param req 更新请求
     * @return 更新后的应用VO
     */
    AppVO updateApp(Long userId, Long appId, UpdateAppRequest req);

    /**
     * 删除应用（逻辑删除，设置为ARCHIVED）
     * @param userId 用户ID
     * @param appId 应用ID
     */
    void deleteApp(Long userId, Long appId);
}
