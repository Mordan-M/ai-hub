package com.mordan.aihub.lowcode.config;

import com.mordan.aihub.lowcode.constant.AppConstant;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @className: StaticResourceConfig
 * @description: TODO 类描述
 * @author: 91002183
 * @date: 2026/3/30
 **/
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 访问 /preview/** 时映射到 dist/ 目录
        registry.addResourceHandler("/lowcode/preview/**")
                .addResourceLocations("file:" + AppConstant.CODE_OUTPUT_ROOT_DIR + "/")
                .setCacheControl(CacheControl.noCache());
    }
}