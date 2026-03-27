package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.ValidateCodeAiService;
import com.mordan.aihub.lowcode.workflow.state.CodeFile;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 代码校验节点
 * 进行Java规则校验 + LLM增强校验
 */
@Slf4j
@Component
public class ValidateCodeNode implements NodeAction<WorkflowState> {

    @Resource
    private ValidateCodeAiService validateCodeAiService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        List<String> errors = new ArrayList<>();
        GeneratedCode generatedCode = ctx.getGeneratedCode();

        // === 一级校验：规则强制性校验 ===
        try {
            List<CodeFile> filesNode = generatedCode.getFiles();
            if (filesNode == null) {
                errors.add("格式错误：缺少'files'文件列表");
            } else {
                // 检查必要文件是否存在
                boolean hasPackageJson = false;
                boolean hasIndexHtml = false;
                boolean hasMainJsx = false;
                boolean hasAppJsx = false;

                for (CodeFile file : filesNode) {
                    String path = file.getPath();
                    String content = file.getContent();

                    if (path.equals("package.json")) hasPackageJson = true;
                    if (path.equals("index.html")) hasIndexHtml = true;
                    if (path.equals("src/main.jsx")) hasMainJsx = true;
                    if (path.equals("src/App.jsx")) hasAppJsx = true;

                    // 禁止词检测
                    if (content.contains("require('fs')") || content.contains("require('fs')"))
                        errors.add("包含禁止的Node.js fs模块");
                    if (content.contains("require('express')")) errors.add("包含禁止的express模块");
                    if (content.contains("require('http')")) errors.add("包含禁止的http模块");
                    if (content.contains("process.env")) errors.add("包含禁止的process.env引用");

                    // 本地图片路径检测
                    if (content.contains("src=\"./assets") || content.contains("src='/assets"))
                        errors.add("使用了本地相对图片路径，请使用公开CDN链接");
                    if (content.contains("src=\"/images") || content.contains("src='/images"))
                        errors.add("使用了本地图片路径，请使用公开CDN链接");
                    if (content.contains("placeholder.com") || content.contains("via.placeholder"))
                        errors.add("使用了禁止的placeholder占位图片，请使用Picsum或Unsplash");

                    // 基础语法检查
                    if (path.endsWith(".jsx") || path.endsWith(".js")) {
                        if (!content.contains("import React") && !content.contains("from 'react'")) {
                            errors.add(path + ": 缺少React导入");
                        }
                    }
                }

                if (!hasPackageJson) errors.add("缺少必要文件：package.json");
                if (!hasIndexHtml) errors.add("缺少必要文件：index.html");
                if (!hasMainJsx) errors.add("缺少必要文件：src/main.jsx");
                if (!hasAppJsx) errors.add("缺少必要文件：src/App.jsx");

                // API文档二级校验
                String apiDocText = ctx.getApiDocText();
                if (apiDocText != null && !apiDocText.isEmpty() && filesNode != null) {
                    boolean foundApiCall = false;
                    for (CodeFile file : filesNode) {
                        String content = file.getContent();
                        // 简单检查是否包含API路径关键词
                        for (String pathWord : apiDocText.split("\\s+")) {
                            if (pathWord.length() > 5 && pathWord.startsWith("/") && content.contains(pathWord)) {
                                foundApiCall = true;
                                break;
                            }
                        }
                        if (foundApiCall) break;
                    }
                    if (!foundApiCall) {
                        errors.add("未检测到对提供的API文档中接口的调用代码");
                    }
                }
            }
        } catch (Exception e) {
            errors.add("代码解析失败：" + e.getMessage());
        }

        // === 如果规则校验通过，进行LLM增强校验 ===
        String llmSuggestions = "";
        if (errors.isEmpty()) {
            // 序列化为JSON字符串传给AI服务
            try {
                String generatedCodeJson = objectMapper.writeValueAsString(generatedCode);
                llmSuggestions = validateCodeAiService.validateCode(generatedCodeJson).trim();
                if (!llmSuggestions.equals("无")) {
                    // 将LLM建议拆分为列表
                    for (String line : llmSuggestions.split("\\n")) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.equals("-") && !trimmed.equals("*")) {
                            errors.add("LLM建议: " + trimmed.replaceAll("^[-*]\\s*", ""));
                        }
                    }
                }
            } catch (Exception e) {
                errors.add("JSON序列化失败：" + e.getMessage());
            }
        }

        // 更新上下文
        ctx.setValidationErrors(errors);
        ctx.setLlmSuggestions(llmSuggestions);

        log.info("Code validation completed, found {} errors", errors.size());
        return WorkflowState.saveContext(ctx);
    }
}
