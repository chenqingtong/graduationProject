package com.laigeoffer.pmhub.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 项目任务流程服务实现类
 *
 * @author chenqingtong
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
    public void resetTaskStatus(String extraId) {
        ProjectTask task = projectTaskMapper.selectById(extraId);
        if (task != null) {
            task.setStatus(0); // 未开始
            task.setTaskProcess(new BigDecimal("0.00"));
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

    @Override
    public Integer getTaskStatus(String taskId) {
        ProjectTask task = projectTaskMapper.selectById(taskId);
        if (task != null) {
            return task.getStatus();
        }
        return null;
    }

    @Override
    public WfTaskProcess getByInstanceId(String instanceId) {
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTaskProcess::getInstanceId, instanceId);
        ProjectTaskProcess projectTaskProcess = projectTaskProcessMapper.selectOne(queryWrapper);
        return convert(projectTaskProcess);
    }

    @Override
    public WfTaskProcess getByInstanceIdAndType(String instanceId, String type) {
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTaskProcess::getInstanceId, instanceId)
                .eq(ProjectTaskProcess::getType, type);
        ProjectTaskProcess projectTaskProcess = projectTaskProcessMapper.selectOne(queryWrapper);
        return convert(projectTaskProcess);
    }

    @Override
    public List<WfTaskProcess> listByType(String type) {
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTaskProcess::getType, type);
        List<ProjectTaskProcess> list = projectTaskProcessMapper.selectList(queryWrapper);
        return convert(list);
    }

    @Override
    public List<WfTaskProcess> listByExtraIds(List<String> extraIds) {
        if (extraIds == null || extraIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ProjectTaskProcess::getExtraId, extraIds);
        List<ProjectTaskProcess> list = projectTaskProcessMapper.selectList(queryWrapper);
        return convert(list);
    }

    @Override
    public List<WfTaskProcess> listByExtraIdsAndType(List<String> extraIds, String type) {
        if (extraIds == null || extraIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ProjectTaskProcess::getExtraId, extraIds)
                .eq(ProjectTaskProcess::getType, type);
        List<ProjectTaskProcess> list = projectTaskProcessMapper.selectList(queryWrapper);
        return convert(list);
    }

    @Override
    public boolean deleteById(String id) {
        return projectTaskProcessMapper.deleteById(id) > 0;
    }

    @Override
    public WfTaskProcess clearAssociation(String extraId, String type, boolean clearUrl) {
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTaskProcess::getExtraId, extraId)
                .eq(ProjectTaskProcess::getType, type);
        ProjectTaskProcess projectTaskProcess = projectTaskProcessMapper.selectOne(queryWrapper);
        if (projectTaskProcess == null) {
            return null;
        }
        projectTaskProcess.setInstanceId(null);
        projectTaskProcess.setTaskId(null);
        if (clearUrl) {
            projectTaskProcess.setUrl(null);
        }
        projectTaskProcess.setUpdatedBy(SecurityUtils.getUsername());
        projectTaskProcess.setUpdatedTime(new Date());
        projectTaskProcessMapper.updateById(projectTaskProcess);
        return convert(projectTaskProcess);
    }

    @Override
    public void updateDefinitionByPrefix(String definitionIdPrefix, String newDefinitionId, String newDeploymentId) {
        LambdaUpdateWrapper<ProjectTaskProcess> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.likeRight(ProjectTaskProcess::getDefinitionId, definitionIdPrefix)
                .eq(ProjectTaskProcess::getApproved, "0")
                .isNull(ProjectTaskProcess::getInstanceId)
                .set(ProjectTaskProcess::getDefinitionId, newDefinitionId)
                .set(ProjectTaskProcess::getDeploymentId, newDeploymentId)
                .set(ProjectTaskProcess::getUpdatedBy, SecurityUtils.getUsername())
                .set(ProjectTaskProcess::getUpdatedTime, new Date());
        projectTaskProcessMapper.update(null, updateWrapper);
    }

    private WfTaskProcess convert(ProjectTaskProcess projectTaskProcess) {
        if (projectTaskProcess == null) {
            return null;
        }
        WfTaskProcess wfTaskProcess = new WfTaskProcess();
        BeanUtils.copyProperties(projectTaskProcess, wfTaskProcess);
        return wfTaskProcess;
    }

    private List<WfTaskProcess> convert(List<ProjectTaskProcess> list) {
        if (list == null || list.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(this::convert)
                .collect(Collectors.toList());
    }
}

