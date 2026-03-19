package com.mordan.aihub.fitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.fitness.domain.entity.UserPreference;
import com.mordan.aihub.fitness.mapper.UserPreferenceMapper;
import com.mordan.aihub.fitness.service.UserPreferenceService;
import org.springframework.stereotype.Service;

/**
 * 用户偏好 Service 实现类
 *
 * @author fitness
 */
@Service
public class UserPreferenceServiceImpl extends ServiceImpl<UserPreferenceMapper, UserPreference> implements UserPreferenceService {

}
