package com.mordan.aihub.lowcode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mordan.aihub.lowcode.domain.entity.GenerationTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生成任务 Mapper
 */
@Mapper
public interface GenerationTaskMapper extends BaseMapper<GenerationTask> {
}
