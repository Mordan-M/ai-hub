package com.mordan.aihub.lowcode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 部署配置属性
 */
@Data
@ConfigurationProperties(prefix = "app.deploy")
@Component
public class DeployProperties {

    /** 部署基础URL，例：http://localhost:4567 */
    private String baseUrl;

    /** 部署端口，默认 4567 */
    private int port = 4567;
}
