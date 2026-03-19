package com.mordan.aihub.fitness.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 视频跳转链接构建工具
 * Web 端跳转 B站，移动端跳转 Keep（Deep Link）
 */
public class VideoLinkBuilder {

    /**
     * Web 端：B站搜索跳转
     * https://search.bilibili.com/all?keyword={动作中文名}+健身教程
     */
    public static String buildBilibiliUrl(String nameZh) {
        String keyword = nameZh + " 健身教程";
        String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        return "https://search.bilibili.com/all?keyword=" + encoded;
    }

    /**
     * 移动端预留：Keep App Deep Link
     * keep://search?keyword={动作中文名}
     */
    public static String buildKeepDeepLink(String nameZh) {
        String encoded = URLEncoder.encode(nameZh, StandardCharsets.UTF_8);
        return "keep://search?keyword=" + encoded;
    }
}
