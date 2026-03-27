package com.mordan.aihub.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 自动填充创建时间和更新时间（使用时间戳，单位毫秒）
 *
 * @author fitness
 */
@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        long now = System.currentTimeMillis();
        this.strictInsertFill(metaObject, "createdAt", Long.class, now);
        this.strictInsertFill(metaObject, "updatedAt", Long.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        long now = System.currentTimeMillis();
        this.strictUpdateFill(metaObject, "updatedAt", Long.class, now);
    }
}