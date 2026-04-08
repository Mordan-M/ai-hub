package com.mordan.aihub.lowcode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 低代码平台配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.lowcode")
public class LowCodeProperties {

    /**
     * 代码生成输出根目录
     */
    private String codeOutputRootDir = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 代码部署根目录
     */
    private String codeDeployRootDir = System.getProperty("user.dir") + "/tmp/code_deploy";
}
