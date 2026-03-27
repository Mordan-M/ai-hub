package com.mordan.aihub.lowcode.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.lowcode.domain.entity.Application;
import com.mordan.aihub.lowcode.domain.enums.AppStatus;
import com.mordan.aihub.lowcode.mapper.ApplicationMapper;
import com.mordan.aihub.lowcode.domain.service.ApplicationService;
import com.mordan.aihub.lowcode.web.request.CreateAppRequest;
import com.mordan.aihub.lowcode.web.request.UpdateAppRequest;
import com.mordan.aihub.lowcode.web.vo.AppVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用服务实现
 */
@Service
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application>
        implements ApplicationService {

    @Override
    public AppVO createApp(Long userId, CreateAppRequest req) {
        Application app = Application.builder()
                .userId(userId)
                .name(req.getName())
                .description(req.getDescription())
                .status(AppStatus.ACTIVE)
                .build();
        save(app);
        return toVO(app);
    }

    @Override
    public List<AppVO> listApps(Long userId) {
        List<Application> apps = this.lambdaQuery()
                .eq(Application::getUserId, userId)
                .eq(Application::getStatus, AppStatus.ACTIVE)
                .orderByDesc(Application::getUpdatedAt)
                .list();
        return apps.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public AppVO getAppDetail(Long userId, Long appId) {
        Application app = this.lambdaQuery()
                .eq(Application::getId, appId)
                .eq(Application::getUserId, userId)
                .one();
        if (app == null) {
            throw new RuntimeException("应用不存在或无权访问");
        }
        return toVO(app);
    }

    @Override
    public AppVO updateApp(Long userId, Long appId, UpdateAppRequest req) {
        // 鉴权
        getAppDetail(userId, appId);

        this.lambdaUpdate()
                .eq(Application::getId, appId)
                .eq(Application::getUserId, userId)
                .set(StringUtils.hasText(req.getName()), Application::getName, req.getName())
                .set(req.getDescription() != null, Application::getDescription, req.getDescription())
                .set(Application::getUpdatedAt, System.currentTimeMillis())
                .update();

        Application updated = getById(appId);
        return toVO(updated);
    }

    @Override
    public void deleteApp(Long userId, Long appId) {
        // 鉴权
        getAppDetail(userId, appId);

        this.lambdaUpdate()
                .eq(Application::getId, appId)
                .eq(Application::getUserId, userId)
                .set(Application::getStatus, AppStatus.ARCHIVED)
                .set(Application::getUpdatedAt, System.currentTimeMillis())
                .update();
    }

    /**
     * 转换为VO
     */
    private AppVO toVO(Application app) {
        return AppVO.builder()
                .id(app.getId())
                .userId(app.getUserId())
                .name(app.getName())
                .description(app.getDescription())
                .status(app.getStatus())
                .thumbnailUrl(app.getThumbnailUrl())
                .latestVersionId(app.getLatestVersionId())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
