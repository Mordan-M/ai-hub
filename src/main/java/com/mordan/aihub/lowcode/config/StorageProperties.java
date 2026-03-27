package com.mordan.aihub.lowcode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置属性
 */
@Data
@ConfigurationProperties(prefix = "app.storage")
@Component
public class StorageProperties {

    /** 代码存储根目录，默认 ./storage */
    private String root = "./storage";
}
