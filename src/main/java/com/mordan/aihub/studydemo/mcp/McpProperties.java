package com.mordan.aihub.studydemo.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @className: McpProperties
 * @description: TODO 类描述
 * @author: 91002183
 * @date: 2026/3/16
 **/
@ConfigurationProperties(prefix = "mcp.filesystem")
@Data
public class McpProperties {

    private List<String> rootDirs = new ArrayList<>();
    private String serverVersion = "0.6.2";
    private int initializationTimeout = 30;
    private int toolExecutionTimeout = 60;

}
