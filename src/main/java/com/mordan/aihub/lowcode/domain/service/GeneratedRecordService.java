package com.mordan.aihub.lowcode.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mordan.aihub.lowcode.domain.entity.GeneratedRecord;
import com.mordan.aihub.lowcode.web.vo.GenerateRecordVO;

/**
 * 生成版本服务接口
 */
public interface GeneratedRecordService extends IService<GeneratedRecord> {


    /**
     * 获取版本详情（带鉴权）
     * @param appId 应用ID
     * @return 版本VO
     */
    GenerateRecordVO getGeneratedRecord(Long appId);

}
