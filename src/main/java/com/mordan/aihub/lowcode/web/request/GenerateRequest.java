package com.mordan.aihub.lowcode.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交代码生成请求
 */
@Data
public class GenerateRequest {

    /** 用户提示词 */
    @NotBlank(message = "提示词不能为空")
    @Size(max = 2000, message = "提示词最长2000个字符")
    private String prompt;

    /** 用户粘贴的API文档文本，可为空 */
    @Size(max = 20000, message = "API文档最长20000个字符")
    private String apiDocText;

    /** 基于哪个版本迭代，可为空 */
    private Long baseVersionId;
}
