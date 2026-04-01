//package com.mordan.aihub.lowcode.infrastructure.deploy;
//
//import com.mordan.aihub.lowcode.domain.entity.GeneratedVersion;
//import com.mordan.aihub.lowcode.mapper.GeneratedVersionMapper;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.security.SecureRandom;
//
///**
// * Slug生成器
// * 生成8位随机字符串作为部署路径唯一标识
// */
//@Slf4j
//@Component
//public class SlugGenerator {
//
//    private static final int SLUG_LENGTH = 8;
//    private static final int MAX_RETRIES = 5;
//    private static final String CHAR_POOL = "abcdefghijklmnopqrstuvwxyz0123456789";
//
//    @Resource
//    private GeneratedVersionMapper generatedVersionMapper;
//    private final SecureRandom random;
//
//    public SlugGenerator() {
//        this.random = new SecureRandom();
//    }
//
//    /**
//     * 生成唯一的slug
//     * @return 8位随机字符串
//     * @throws RuntimeException 生成失败（多次冲突）时抛出
//     */
//    public String generateUnique() {
//        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
//            String slug = generateSlug();
//            // 检查是否已存在
//            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GeneratedVersion> wrapper =
//                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
//            wrapper.eq(GeneratedVersion::getDeploySlug, slug);
//            Long count = generatedVersionMapper.selectCount(wrapper);
//            if (count == 0) {
//                return slug;
//            }
//            log.warn("Slug conflict detected, retrying... attempt={}", attempt + 1);
//        }
//        throw new RuntimeException("slug生成失败，请重试");
//    }
//
//    /**
//     * 生成单次随机slug
//     */
//    private String generateSlug() {
//        StringBuilder sb = new StringBuilder(SLUG_LENGTH);
//        for (int i = 0; i < SLUG_LENGTH; i++) {
//            int index = random.nextInt(CHAR_POOL.length());
//            sb.append(CHAR_POOL.charAt(index));
//        }
//        return sb.toString();
//    }
//}
