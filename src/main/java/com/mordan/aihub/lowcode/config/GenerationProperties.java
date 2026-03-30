package com.mordan.aihub.lowcode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 代码生成配置属性
 */
@Data
@ConfigurationProperties(prefix = "app.generation")
@Component
public class GenerationProperties {

    /** 每个用户最大并发任务数，默认 2 */
    private int maxConcurrentTasksPerUser = 2;

    /** 最大重试次数，默认 3 */
    private int maxRetry = 3;

    /** 持久化构建输出的基础路径，默认使用系统临时目录 */
    private String persistOutputBasePath;
}
