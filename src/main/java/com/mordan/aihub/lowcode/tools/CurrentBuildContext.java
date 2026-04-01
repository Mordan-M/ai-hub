package com.mordan.aihub.lowcode.tools;

import com.mordan.aihub.lowcode.constant.AppConstant;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 当前构建上下文
 * 维护 appId → 当前构建目录前缀 的映射，供文件工具获取正确的构建路径
 */
public class CurrentBuildContext {

    /**
     * appId → 当前构建目录前缀（8位随机串）
     * 同一 appId 同一时间只会有一个构建任务，所以可以这样缓存
     */
    private static final Map<Long, String> CURRENT_PREFIX = new ConcurrentHashMap<>();

    /**
     * 设置当前 appId 的构建前缀
     */
    public static void setPrefix(Long appId, String prefix) {
        CURRENT_PREFIX.put(appId, prefix);
    }

    /**
     * 获取当前 appId 的构建前缀
     */
    public static String getPrefix(Long appId) {
        return CURRENT_PREFIX.get(appId);
    }

    /**
     * 清除当前 appId 的构建前缀（任务完成后调用）
     */
    public static void remove(Long appId) {
        CURRENT_PREFIX.remove(appId);
    }

    /**
     * 获取当前构建项目根路径
     */
    public static Path getProjectRoot(Long appId) {
        String prefix = CURRENT_PREFIX.get(appId);
        String dirName = AppConstant.CODE_OUTPUT_PREFIX + prefix;
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName);
    }

    public static Path getProjectRoot(String appId) {
        return getProjectRoot(Long.parseLong(appId));
    }
}
