package com.mordan.aihub.lowcode.constant;

/**
 * 应用常量
 */
public interface AppConstant {

    String CODE_OUTPUT_PREFIX = "lowcode-output-";

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 预览地址前缀
     */
    String PREVIEW_URL_PREFIX = "/lowcode/preview";

    /**
     * 部署地址前缀
     */
    String DEPLOY_URL_PREFIX = "/lowcode/deploy";

}