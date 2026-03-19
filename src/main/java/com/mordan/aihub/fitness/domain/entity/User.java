package com.mordan.aihub.fitness.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体类
 *
 * @author fitness
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class User {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** BCrypt加密后的密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatarUrl;

    /** 用户状态：1=正常 0=禁用 */
    private Byte status;

    /** 逻辑删除：0=未删除 1=已删除 */
    @TableLogic
    private Byte deleted;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    /** 更新时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;
}