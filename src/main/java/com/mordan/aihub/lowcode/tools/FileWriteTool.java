package com.mordan.aihub.lowcode.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {


    @Tool("""
            写入文件到指定路径。
            content 参数必须是字符串。如果写入 JSON 文件，请传入合法的 JSON 字符串，例如：
            '{"name":"my-app","version":"1.0.0"}'
            不要传递未经引号包裹的对象。
            """)
    public String writeFile(
            @P("文件的相对路径，例如：package.json、src/main.js") String relativeFilePath,
            @P("文件的完整内容，必须是字符串") String content,
            @ToolMemoryId String appId
    ) {
        try {
            // 修复可能出现的 Map.toString() 格式 {key=value, ...}
            content = fixMapString(content);

            Path projectRoot = CurrentBuildContext.getProjectRoot(appId);
            Path path = projectRoot.resolve(relativeFilePath);
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path.toAbsolutePath());
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        return String.format("""
                        [工具调用] %s %s
                        ```%s
                        %s
                        ```
                        """, getDisplayName(), relativeFilePath, suffix, content);
    }

    /**
     * 将 Java Map.toString() 产生的字符串（如 {name=app, version=1.0}）
     * 转换为标准 JSON 字符串。
     * 如果输入不是这种格式，原样返回。
     */
    private String fixMapString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // 检测是否以 { 开头，包含 =，且没有 "（即不是标准 JSON）
        if (input.startsWith("{") && input.contains("=") && !input.contains("\"")) {
            log.warn("检测到非标准 Map 字符串格式，尝试转换为 JSON: {}", input);
            try {
                // 使用正则将 key=value 转换为 "key":"value"
                // 注意：value 可能包含嵌套的 {key=value}，需要递归处理
                String json = convertMapStringToJson(input);
                log.info("转换后的 JSON: {}", json);
                return json;
            } catch (Exception e) {
                log.error("转换失败，使用原始内容", e);
                return input;
            }
        }
        return input;
    }

    /**
     * 将类似 {name=app, scripts={dev=vite}} 的字符串转换为 JSON
     * 使用简单的栈解析，支持嵌套
     */
    private String convertMapStringToJson(String mapStr) {
        // 去掉最外层的 { 和 }
        String inner = mapStr.substring(1, mapStr.length() - 1);
        StringBuilder json = new StringBuilder("{");
        int i = 0;
        int len = inner.length();
        while (i < len) {
            // 查找 key
            int eqIdx = findNextUnquoted(inner, '=', i);
            if (eqIdx == -1) break;
            String key = inner.substring(i, eqIdx).trim();
            // 查找 value，可能嵌套 {}
            int valueStart = eqIdx + 1;
            int valueEnd = findValueEnd(inner, valueStart);
            String value = inner.substring(valueStart, valueEnd).trim();
            // 处理 value：如果 value 以 { 开头，递归转换
            if (value.startsWith("{") && value.endsWith("}")) {
                value = convertMapStringToJson(value);
            } else {
                // 转义双引号
                value = "\"" + escapeJsonString(value) + "\"";
            }
            json.append("\"").append(escapeJsonString(key)).append("\":").append(value);
            // 查找逗号
            i = valueEnd;
            if (i < len && inner.charAt(i) == ',') {
                json.append(",");
                i++;
                // 跳过空格
                while (i < len && Character.isWhitespace(inner.charAt(i))) i++;
            } else {
                break;
            }
        }
        json.append("}");
        return json.toString();
    }

    private int findNextUnquoted(String s, char ch, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == ch) return i;
            if (s.charAt(i) == '{') {
                // 跳过嵌套结构
                i = findMatchingBrace(s, i);
            }
        }
        return -1;
    }

    private int findValueEnd(String s, int start) {
        if (s.charAt(start) == '{') {
            return findMatchingBrace(s, start) + 1;
        } else {
            int i = start;
            while (i < s.length() && s.charAt(i) != ',' && s.charAt(i) != '}') {
                i++;
            }
            return i;
        }
    }

    private int findMatchingBrace(String s, int openIdx) {
        int depth = 1;
        for (int i = openIdx + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth == 0) return i;
        }
        return s.length() - 1;
    }

    private String escapeJsonString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
