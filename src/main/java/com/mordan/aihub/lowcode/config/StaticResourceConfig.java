package com.mordan.aihub.lowcode.config;

import com.mordan.aihub.lowcode.constant.AppConstant;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @className: StaticResourceConfig
 * @description: 静态资源配置
 * @author: 91002183
 * @date: 2026/3/30
 **/
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Resource
    private LowCodeProperties lowCodeProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 访问 /preview/** 时映射到 dist/ 目录
        registry.addResourceHandler(AppConstant.PREVIEW_URL_PREFIX + "/**")
                .addResourceLocations("file:" + lowCodeProperties.getCodeOutputRootDir() + "/")
                .setCacheControl(CacheControl.noCache());
        registry.addResourceHandler(AppConstant.DEPLOY_URL_PREFIX + "/**")
                .addResourceLocations("file:" + lowCodeProperties.getCodeDeployRootDir() + "/")
                .setCacheControl(CacheControl.noCache());
    }
}