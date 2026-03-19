package com.mordan.aihub.fitness.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置
 * 启用 @Async 并配置自定义线程池
 *
 * @author fitness
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 健身计划异步生成线程池
     * 核心线程数 = 2，最大线程数 = 5，队列容量 = 20
     */
    @Bean("fitnessPlanExecutor")
    public Executor fitnessPlanExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("fitness-plan-");
        executor.initialize();
        return executor;
    }
}
