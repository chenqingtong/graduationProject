package com.laigeoffer.pmhub.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.project.domain.ProjectTask;
import com.laigeoffer.pmhub.project.domain.ProjectTaskProcess;
import com.laigeoffer.pmhub.project.mapper.ProjectTaskMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectTaskProcessMapper;
import com.laigeoffer.pmhub.project.service.ProjectTaskProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目任务流程服务实现类
 *
 * @author canghe
 */
@Service
@RequiredArgsConstructor
public class ProjectTaskProcessServiceImpl extends ServiceImpl<ProjectTaskProcessMapper, ProjectTaskProcess> implements ProjectTaskProcessService {

    private final ProjectTaskProcessMapper projectTaskProcessMapper;
    private final ProjectTaskMapper projectTaskMapper;

    @Override
    public WfTaskProcess getByExtraIdAndType(String extraId, String type) {
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTaskProcess::getExtraId, extraId)
                .eq(ProjectTaskProcess::getType, type);
        ProjectTaskProcess projectTaskProcess = projectTaskProcessMapper.selectOne(queryWrapper);
        
        if (projectTaskProcess == null) {
            return null;
        }
        
        WfTaskProcess wfTaskProcess = new WfTaskProcess();
        BeanUtils.copyProperties(projectTaskProcess, wfTaskProcess);
        return wfTaskProcess;
    }

    @Override
    public WfTaskProcess insertOrUpdate(WfTaskProcess wfTaskProcess) {
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTaskProcess::getExtraId, wfTaskProcess.getExtraId())
                .eq(ProjectTaskProcess::getType, wfTaskProcess.getType());
        ProjectTaskProcess projectTaskProcess = projectTaskProcessMapper.selectOne(queryWrapper);
        
        if (projectTaskProcess == null) {
            // 新增
            projectTaskProcess = new ProjectTaskProcess();
            BeanUtils.copyProperties(wfTaskProcess, projectTaskProcess);
            projectTaskProcess.setCreatedBy(SecurityUtils.getUsername());
            projectTaskProcess.setCreatedTime(new Date());
            projectTaskProcess.setUpdatedBy(SecurityUtils.getUsername());
            projectTaskProcess.setUpdatedTime(new Date());
            projectTaskProcessMapper.insert(projectTaskProcess);
        } else {
            // 更新
            BeanUtils.copyProperties(wfTaskProcess, projectTaskProcess, "id", "createdBy", "createdTime");
            projectTaskProcess.setUpdatedBy(SecurityUtils.getUsername());
            projectTaskProcess.setUpdatedTime(new Date());
            projectTaskProcessMapper.updateById(projectTaskProcess);
        }
        
        WfTaskProcess result = new WfTaskProcess();
        BeanUtils.copyProperties(projectTaskProcess, result);
        return result;
    }

    @Override
    public void updateTaskStatus3(String extraId) {
        ProjectTask task = projectTaskMapper.selectById(extraId);
        if (task != null) {
            task.setStatus(1); // 进行中
            projectTaskMapper.updateById(task);
        }
    }

    @Override
    public void updateTaskStatus(String extraId) {
        ProjectTask task = projectTaskMapper.selectById(extraId);
        if (task != null) {
            task.setStatus(2); // 已完成
            task.setTaskProcess(new BigDecimal("100.00"));
            projectTaskMapper.updateById(task);
        }
    }

    @Override
    public void updateApproved(String extraId, String type, String approved) {
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTaskProcess::getExtraId, extraId)
                .eq(ProjectTaskProcess::getType, type);
        ProjectTaskProcess projectTaskProcess = projectTaskProcessMapper.selectOne(queryWrapper);
        if (projectTaskProcess != null) {
            projectTaskProcess.setApproved(approved);
            projectTaskProcess.setUpdatedBy(SecurityUtils.getUsername());
            projectTaskProcess.setUpdatedTime(new Date());
            projectTaskProcessMapper.updateById(projectTaskProcess);
        }
    }

    @Override
    public Integer getTaskExecuteStatus(String taskId) {
        ProjectTask task = projectTaskMapper.selectById(taskId);
        if (task != null) {
            return task.getExecuteStatus();
        }
        return null;
    }
}

