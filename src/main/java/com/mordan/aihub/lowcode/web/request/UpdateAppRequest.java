package com.mordan.aihub.lowcode.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新应用请求
 */
@Data
public class UpdateAppRequest {

    /** 应用名称 */
    @Size(max = 50, message = "应用名称最长50个字符")
    private String name;

    /** 应用描述 */
    @Size(max = 500, message = "应用描述最长500个字符")
    private String description;
}
