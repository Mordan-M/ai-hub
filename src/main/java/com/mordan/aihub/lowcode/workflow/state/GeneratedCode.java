package com.mordan.aihub.lowcode.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 生成的代码结果
 * 包含多个代码文件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 7891234560123456789L;

    /**
     * 文件列表
     */
    private List<CodeFile> files;
}
