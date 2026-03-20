package com.mordan.aihub.fitness.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 异步配置
 * 启用 @Async 并配置自定义线程池
 * 使用 JDK 21 虚拟线程提升 AI 调用并发性能
 *
 * @author fitness
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 健身计划异步生成线程池
     * 使用虚拟线程：每个 AI 调用占用一个虚拟线程，阻塞等待网络响应时不占用平台线程
     * 相比固定大小线程池，支持更高并发，内存开销更小
     */
    @Bean("fitnessPlanExecutor")
    public Executor fitnessPlanExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
