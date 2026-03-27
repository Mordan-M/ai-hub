package com.mordan.aihub.lowcode.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mordan.aihub.lowcode.domain.entity.GenerationTask;
import com.mordan.aihub.lowcode.web.request.GenerateRequest;
import com.mordan.aihub.lowcode.web.vo.TaskVO;

import java.util.List;

/**
 * 生成任务服务接口
 */
public interface GenerationTaskService extends IService<GenerationTask> {

    /**
     * 提交代码生成任务（异步执行）
     * @param userId 用户ID
     * @param appId 应用ID
     * @param req 生成请求
     * @return 任务VO
     */
    TaskVO submitGenerateTask(Long userId, Long appId, GenerateRequest req);

    /**
     * 获取任务状态（带鉴权）
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 任务VO
     */
    TaskVO getTaskStatus(Long userId, Long taskId);

    /**
     * 查询应用下所有任务（带鉴权）
     * @param userId 用户ID
     * @param appId 应用ID
     * @return 任务VO列表
     */
    List<TaskVO> listTasksByApp(Long userId, Long appId);
}
