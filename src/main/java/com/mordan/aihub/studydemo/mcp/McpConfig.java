package com.mordan.aihub.studydemo.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import jakarta.annotation.Resource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(McpProperties.class)
public class McpConfig {

    @Resource
    private McpProperties props;

    @Bean
    public McpToolProvider filesystemMcpToolProvider() {
        ensureRootDirsExist();

        // 多目录拼入命令行参数
        List<String> commandArgs = new ArrayList<>();
        commandArgs.add("@modelcontextprotocol/server-filesystem@" + props.getServerVersion());
        commandArgs.addAll(props.getRootDirs());

        McpTransport transport = new StdioMcpTransport.Builder()
                .command(buildNpxCommand(commandArgs.toArray(new String[0])))
                .logEvents(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .initializationTimeout(Duration.ofSeconds(props.getInitializationTimeout()))
                .toolExecutionTimeout(Duration.ofSeconds(props.getToolExecutionTimeout()))
                .build();

        return McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();
    }

    private void ensureRootDirsExist() {
        props.getRootDirs().forEach(dir -> {
            Path path = Path.of(dir);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    throw new RuntimeException("创建 MCP 根目录失败: " + dir, e);
                }
            }
        });
    }

    private List<String> buildNpxCommand(String... args) {
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().contains("win");
        List<String> command = new ArrayList<>();
        if (isWindows) {
            command.add("cmd");
            command.add("/c");
        }
        command.add("npx");
        command.add("-y");
        command.addAll(List.of(args));
        return command;
    }
}
