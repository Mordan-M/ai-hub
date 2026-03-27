package com.mordan.aihub.lowcode.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建应用请求
 */
@Data
public class CreateAppRequest {

    /** 应用名称 */
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 50, message = "应用名称最长50个字符")
    private String name;

    /** 应用描述 */
    @Size(max = 500, message = "应用描述最长500个字符")
    private String description;
}
