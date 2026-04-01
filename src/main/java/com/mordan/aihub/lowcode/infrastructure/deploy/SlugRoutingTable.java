//package com.mordan.aihub.lowcode.infrastructure.deploy;
//
//import com.mordan.aihub.lowcode.domain.entity.GeneratedVersion;
//import com.mordan.aihub.lowcode.domain.enums.VersionDeployStatus;
//import com.mordan.aihub.lowcode.mapper.GeneratedVersionMapper;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Slug路由表
// * 维护内存中 slug -> 版本代码目录Path 的映射，用于快速响应已部署站点访问
// */
//@Slf4j
//@Component
//public class SlugRoutingTable {
//
//    private final ConcurrentHashMap<String, Path> routingTable = new ConcurrentHashMap<>();
//    @Resource
//    private GeneratedVersionMapper generatedVersionMapper;
//
//    public SlugRoutingTable() {
//    }
//
//    /**
//     * 注册slug路由
//     * @param slug 部署标识
//     * @param path 代码根目录路径
//     */
//    public void register(String slug, Path path) {
//        routingTable.put(slug, path);
//        log.info("Registered route: {} -> {}", slug, path);
//    }
//
//    /**
//     * 移除slug路由
//     * @param slug 部署标识
//     */
//    public void remove(String slug) {
//        routingTable.remove(slug);
//        log.info("Removed route: {}", slug);
//    }
//
//    /**
//     * 根据slug解析路径
//     * @param slug 部署标识
//     * @return 代码根目录Path，不存在返回null
//     */
//    public Path resolve(String slug) {
//        return routingTable.get(slug);
//    }
//
//    /**
//     * 启动时从数据库恢复已部署版本的路由
//     */
//    @PostConstruct
//    public void init() {
//        log.info("Starting SlugRoutingTable initialization from database...");
//        int restoredCount = 0;
//
//        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GeneratedVersion> wrapper =
//                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
//        wrapper.eq(GeneratedVersion::getDeployStatus, VersionDeployStatus.DEPLOYED);
//        var query = generatedVersionMapper.selectList(wrapper);
//
//        for (GeneratedVersion version : query) {
//            String slug = version.getDeploySlug();
//            String storagePath = version.getCodeStoragePath();
//            if (slug == null || storagePath == null) {
//                continue;
//            }
//            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
//            if (Files.exists(path) && Files.isDirectory(path)) {
//                register(slug, path);
//                restoredCount++;
//            } else {
//                log.warn("Skipping slug {}: directory not exists at {}", slug, storagePath);
//            }
//        }
//
//        log.info("SlugRoutingTable initialization completed. Restored {} routes.", restoredCount);
//    }
//}
