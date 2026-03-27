package com.mordan.aihub.lowcode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mordan.aihub.lowcode.domain.entity.Application;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用 Mapper
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {
}
