package com.mordan.aihub.lowcode.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单个代码文件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeFile implements Serializable {

    @Serial
    private static final long serialVersionUID = 4561237890123456789L;

    /**
     * 文件路径
     */
    private String path;

    /**
     * 文件内容
     */
    private String content;
}
