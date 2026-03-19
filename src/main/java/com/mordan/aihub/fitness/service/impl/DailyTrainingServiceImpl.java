package com.mordan.aihub.fitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.fitness.domain.entity.DailyTraining;
import com.mordan.aihub.fitness.mapper.DailyTrainingMapper;
import com.mordan.aihub.fitness.service.DailyTrainingService;
import org.springframework.stereotype.Service;

/**
 * 每日训练 Service 实现类
 *
 * @author fitness
 */
@Service
public class DailyTrainingServiceImpl extends ServiceImpl<DailyTrainingMapper, DailyTraining> implements DailyTrainingService {

}
