package com.mordan.aihub.lowcode.config;

import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangGraph4j 配置类
 */
@Configuration
public class LangGraphConfig {

    /**
     * 内存检查点保存器
     * 用于保存工作流状态快照
     */
    @Bean
    public MemorySaver memorySaver() {
        return new MemorySaver();
    }
}
