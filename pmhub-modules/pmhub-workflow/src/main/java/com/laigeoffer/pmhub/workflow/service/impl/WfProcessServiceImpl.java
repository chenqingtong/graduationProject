package com.laigeoffer.pmhub.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laigeoffer.pmhub.base.core.core.domain.PageQuery;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysDept;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysRole;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.page.Table2DataInfo;
import com.laigeoffer.pmhub.base.core.enums.ProjectStatusEnum;
import com.laigeoffer.pmhub.base.core.enums.ProjectTaskStatusEnum;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.base.core.utils.JsonUtils;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.base.core.utils.StringUtils;
import com.laigeoffer.pmhub.workflow.common.constant.TaskConstants;
import com.laigeoffer.pmhub.workflow.core.FormConf;
import com.laigeoffer.pmhub.workflow.core.domain.ProcessQuery;
import com.laigeoffer.pmhub.workflow.domain.WfDeployForm;
import com.laigeoffer.pmhub.workflow.domain.WfMaterialsScrappedProcess;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.workflow.domain.vo.*;
import com.laigeoffer.pmhub.workflow.factory.FlowServiceFactory;
import com.laigeoffer.pmhub.workflow.mapper.WfCopyMapper;
import com.laigeoffer.pmhub.workflow.domain.WfCopy;
import com.laigeoffer.pmhub.api.project.ProjectTaskProcessFeignService;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.workflow.mapper.WfDeployFormMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfMaterialsScrappedProcessMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfApprovalTaskMapper;
import com.laigeoffer.pmhub.workflow.domain.WfApprovalTask;
import com.laigeoffer.pmhub.workflow.service.IWfDeployService;
import com.laigeoffer.pmhub.workflow.service.IWfProcessService;
import com.laigeoffer.pmhub.workflow.service.IWfTaskService;
import com.laigeoffer.pmhub.workflow.utils.ProcessFormUtils;
import com.laigeoffer.pmhub.workflow.utils.ProcessUtils;
import com.laigeoffer.pmhub.workflow.utils.TaskUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chenqingtong
 * @createTime 2024/3/24 18:57
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WfProcessServiceImpl extends FlowServiceFactory implements IWfProcessService {

    private final IWfTaskService wfTaskService;
    private final WfCopyMapper wfCopyMapper;
    private final WfDeployFormMapper deployFormMapper;
    private final IWfDeployService deployService;
    private final WfMaterialsScrappedProcessMapper wfMaterialsScrappedProcessMapper;
    private final WfApprovalTaskMapper wfApprovalTaskMapper;
    private final ProjectTaskProcessFeignService projectTaskProcessFeignService;
//    private final MaterialsChangeRecordsMapper materialsChangeRecordsMapper;
//    private final MaterialsUselessMapper materialsUselessMapper;

    private final List<String> ts = Arrays.asList("PURCHASE_INTO", "PURCHASE_OUT", "OTHER_INTO", "OTHER_OUT", "RETURN_INTO", "SUPPLIER_APPROVAL", "task");
    /**
     * 流程定义列表
     *
     * @param pageQuery 分页参数
     * @return 流程定义分页列表数据
     */
    @Override
    public Table2DataInfo<WfDefinitionVo> selectPageStartProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        if (repositoryService == null) {
            throw new ServiceException("Flowable 服务未启用，无法查询流程定义列表");
        }
        Page<WfDefinitionVo> page = new Page<>();
        // 流程定义列表数据查询
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
            .latestVersion()
            .active()
            .orderByProcessDefinitionKey()
            .desc();
        // 构建搜索条件
        ProcessUtils.buildProcessSearch(processDefinitionQuery, processQuery);
        long pageTotal = processDefinitionQuery.count();
        if (pageTotal <= 0) {
            return Table2DataInfo.build();
        }
        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        List<ProcessDefinition> definitionList = processDefinitionQuery.listPage(offset, pageQuery.getPageSize());

        List<WfDefinitionVo> definitionVoList = new ArrayList<>();
        for (ProcessDefinition processDefinition : definitionList) {
            String deploymentId = processDefinition.getDeploymentId();
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
            WfDefinitionVo vo = new WfDefinitionVo();
            vo.setDefinitionId(processDefinition.getId());
            vo.setProcessKey(processDefinition.getKey());
            vo.setProcessName(processDefinition.getName());
            vo.setVersion(processDefinition.getVersion());
            vo.setDeploymentId(processDefinition.getDeploymentId());
            vo.setSuspended(processDefinition.isSuspended());
            // 流程定义时间
            vo.setCategory(deployment.getCategory());
            vo.setDeploymentTime(deployment.getDeploymentTime());
            definitionVoList.add(vo);
        }
        definitionVoList.sort(Comparator.comparing(WfDefinitionVo::getDeploymentTime).reversed());
        page.setRecords(definitionVoList);
        page.setTotal(pageTotal);
        return Table2DataInfo.build(page);
    }

    @Override
    public List<WfDefinitionVo> selectStartProcessList(ProcessQuery processQuery) {
        // 流程定义列表数据查询
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .active()
                .orderByProcessDefinitionKey()
                .asc();
        // 构建搜索条件
        ProcessUtils.buildProcessSearch(processDefinitionQuery, processQuery);

        List<ProcessDefinition> definitionList = processDefinitionQuery.list();

        List<WfDefinitionVo> definitionVoList = new ArrayList<>();
        for (ProcessDefinition processDefinition : definitionList) {
            String deploymentId = processDefinition.getDeploymentId();
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
            WfDefinitionVo vo = new WfDefinitionVo();
            vo.setDefinitionId(processDefinition.getId());
            vo.setProcessKey(processDefinition.getKey());
            vo.setProcessName(processDefinition.getName());
            vo.setVersion(processDefinition.getVersion());
            vo.setDeploymentId(processDefinition.getDeploymentId());
            vo.setSuspended(processDefinition.isSuspended());
            // 流程定义时间
            vo.setCategory(deployment.getCategory());
            vo.setDeploymentTime(deployment.getDeploymentTime());
            definitionVoList.add(vo);
        }
        return definitionVoList;
    }

    @Override
    public Table2DataInfo<WfTaskVo> selectPageTodoProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        Page<WfTaskVo> page = new Page<>();
        List<WfTaskVo> flowList = new ArrayList<>();
        
        // 1. 查询 Flowable 流程引擎的待办任务（如果 Flowable 服务可用）
        if (taskService != null && repositoryService != null && historyService != null) {
            TaskQuery taskQuery = taskService.createTaskQuery()
            .active()
            .includeProcessVariables()
            .or()
                .taskCandidateOrAssigned(TaskUtils.getUserId())
                .taskCandidateGroupIn(TaskUtils.getCandidateGroup())
            .endOr()
            .orderByTaskCreateTime().desc();
        // 构建搜索条件
        ProcessUtils.buildProcessSearch(taskQuery, processQuery);
        List<Task> taskList = taskQuery.list();
        for (Task task : taskList) {
            WfTaskVo flowTask = new WfTaskVo();
            // 当前流程信息
            flowTask.setTaskId(task.getId());
            flowTask.setTaskDefKey(task.getTaskDefinitionKey());
            flowTask.setCreateTime(task.getCreateTime());
            flowTask.setProcDefId(task.getProcessDefinitionId());
            flowTask.setTaskName(task.getName());
            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
            if (pd != null) {
                flowTask.setDeployId(pd.getDeploymentId());
                flowTask.setProcDefName(pd.getName());
                flowTask.setProcDefVersion(pd.getVersion());
            }
            flowTask.setProcInsId(task.getProcessInstanceId());

            // 流程发起人信息
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
            if (historicProcessInstance != null && StringUtils.isNotBlank(historicProcessInstance.getStartUserId())) {
                SysUser startUser = wfCopyMapper.selectUserById(Long.parseLong(historicProcessInstance.getStartUserId()));
                if (startUser != null) {
                    flowTask.setStartUserId(startUser.getNickName());
                    flowTask.setStartUserName(startUser.getNickName());
                    SysDept sysDept = wfCopyMapper.selectDeptById(startUser.getDeptId());
                    if (sysDept != null) {
                        flowTask.setStartDeptName(sysDept.getDeptName());
                    }
                }
            }

            // 流程变量
            flowTask.setProcVars(this.getProcessVariables(task.getId()));

            flowList.add(flowTask);
        }
        }
        
        // 2. 查询简化审批任务的待办列表
        Long currentUserId = SecurityUtils.getUserId();
        String userIdStr = String.valueOf(currentUserId);
        
        // 获取当前用户的角色ID列表和部门ID列表
        List<Long> roleIds = null;
        List<Long> deptIds = null;
        com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null && loginUser.getUser() != null) {
            if (CollUtil.isNotEmpty(loginUser.getUser().getRoles())) {
                roleIds = loginUser.getUser().getRoles().stream()
                    .map(com.laigeoffer.pmhub.base.core.core.domain.entity.SysRole::getRoleId)
                    .collect(Collectors.toList());
            }
            if (loginUser.getDeptId() != null) {
                deptIds = new ArrayList<>();
                deptIds.add(loginUser.getDeptId());
            }
        }
        
        // 查询简化审批任务的待办列表
        List<WfApprovalTask> approvalTasks = wfApprovalTaskMapper.selectTodoListByUserId(userIdStr, roleIds, deptIds);
        
        // 将简化审批任务转换为 WfTaskVo
        for (WfApprovalTask approvalTask : approvalTasks) {
            WfTaskVo flowTask = new WfTaskVo();
            // 使用审批任务ID作为任务ID
            flowTask.setTaskId(approvalTask.getId());
            String nodeDisplayName = approvalTask.getTitle();
            String taskDisplayName = null;
            if (ProjectStatusEnum.TASK.getStatusName().equalsIgnoreCase(approvalTask.getType())
                    && StringUtils.isNotBlank(approvalTask.getExtraId())) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("调用项目服务查询任务名称, approvalTaskId:{}, extraId:{}", approvalTask.getId(), approvalTask.getExtraId());
                    }
                    R<String> taskNameResult = projectTaskProcessFeignService.getTaskNameById(approvalTask.getExtraId(), SecurityConstants.INNER);
                    if (taskNameResult != null && R.isSuccess(taskNameResult)) {
                        String taskName = StringUtils.isNotBlank(taskNameResult.getData())
                            ? taskNameResult.getData()
                            : taskNameResult.getMsg();
                        if (StringUtils.isNotBlank(taskName)) {
                            if (log.isInfoEnabled()) {
                                log.info("查询项目任务名称成功, approvalTaskId:{}, extraId:{}, taskName:{}", approvalTask.getId(), approvalTask.getExtraId(), taskName);
                            }
                            taskDisplayName = taskName;
                        } else {
                            log.warn("getTaskNameById 成功但返回内容为空, extraId:{}, code:{}, msg:{}", approvalTask.getExtraId(), taskNameResult.getCode(), taskNameResult.getMsg());
                        }
                    } else if (taskNameResult != null) {
                        log.warn("getTaskNameById failed, extraId:{}, code:{}, msg:{}", approvalTask.getExtraId(), taskNameResult.getCode(), taskNameResult.getMsg());
                    } else {
                        log.warn("getTaskNameById 返回为空, approvalTaskId:{}, extraId:{}", approvalTask.getId(), approvalTask.getExtraId());
                    }
                } catch (Exception ex) {
                    log.error("调用项目服务查询任务名称异常, extraId:{}", approvalTask.getExtraId(), ex);
                }
            }
            if (StringUtils.isBlank(taskDisplayName)) {
                taskDisplayName = nodeDisplayName;
            }
            flowTask.setTaskName(nodeDisplayName);
            flowTask.setCreateTime(approvalTask.getCreatedTime());
            // 简化审批任务没有流程定义，设置默认值
            flowTask.setProcDefName(taskDisplayName);
            flowTask.setProcDefVersion(1);
            // 使用 extraId 作为流程实例ID（用于标识业务对象）
            flowTask.setProcInsId(approvalTask.getExtraId());
            flowTask.setProcDefId("simplified-approval-" + approvalTask.getType());
            
            // 发起人信息
            if (StringUtils.isNotBlank(approvalTask.getInitiatorId())) {
                try {
                    SysUser startUser = wfCopyMapper.selectUserById(Long.parseLong(approvalTask.getInitiatorId()));
                    if (startUser != null) {
                        flowTask.setStartUserId(startUser.getNickName());
                        flowTask.setStartUserName(startUser.getNickName());
                        SysDept sysDept = wfCopyMapper.selectDeptById(startUser.getDeptId());
                        if (sysDept != null) {
                            flowTask.setStartDeptName(sysDept.getDeptName());
                        }
                    } else {
                        // 如果查询不到用户，使用审批任务中保存的发起人姓名
                        flowTask.setStartUserId(approvalTask.getInitiatorName());
                        flowTask.setStartUserName(approvalTask.getInitiatorName());
                    }
                } catch (NumberFormatException e) {
                    flowTask.setStartUserId(approvalTask.getInitiatorName());
                    flowTask.setStartUserName(approvalTask.getInitiatorName());
                }
            }
            
            // 设置流程变量，包含审批任务的URL等信息
            Map<String, Object> procVars = new HashMap<>();
            procVars.put("url", approvalTask.getUrl());
            procVars.put("extraId", approvalTask.getExtraId());
            procVars.put("type", approvalTask.getType());
            procVars.put("approvalTaskId", approvalTask.getId());
            flowTask.setProcVars(procVars);
            
            flowList.add(flowTask);
        }
        
        // 3. 合并结果并按创建时间排序
        flowList.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) {
                return 0;
            }
            if (a.getCreateTime() == null) {
                return 1;
            }
            if (b.getCreateTime() == null) {
                return -1;
            }
            return b.getCreateTime().compareTo(a.getCreateTime()); // 降序
        });
        
        // 4. 分页处理
        int total = flowList.size();
        page.setTotal(total);
        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        int end = Math.min(offset + pageQuery.getPageSize(), total);
        List<WfTaskVo> pagedList = offset < total ? flowList.subList(offset, end) : new ArrayList<>();
        page.setRecords(pagedList);
        
        return Table2DataInfo.build(page);
    }

    @Override
    public List<WfTaskVo> selectTodoProcessList(ProcessQuery processQuery) {
        TaskQuery taskQuery = taskService.createTaskQuery()
                .active()
                .includeProcessVariables()
                .or()
                    .taskCandidateOrAssigned(TaskUtils.getUserId())
                    .taskCandidateGroupIn(TaskUtils.getCandidateGroup())
                .endOr()
                .orderByTaskCreateTime().desc();
        // 构建搜索条件
        ProcessUtils.buildProcessSearch(taskQuery, processQuery);
        List<Task> taskList = taskQuery.list();
        List<WfTaskVo> taskVoList = new ArrayList<>();
        for (Task task : taskList) {
            WfTaskVo taskVo = new WfTaskVo();
            // 当前流程信息
            taskVo.setTaskId(task.getId());
            taskVo.setTaskDefKey(task.getTaskDefinitionKey());
            taskVo.setCreateTime(task.getCreateTime());
            taskVo.setProcDefId(task.getProcessDefinitionId());
            taskVo.setTaskName(task.getName());
            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            taskVo.setDeployId(pd.getDeploymentId());
            taskVo.setProcDefName(pd.getName());
            taskVo.setProcDefVersion(pd.getVersion());
            taskVo.setProcInsId(task.getProcessInstanceId());

            // 流程发起人信息
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            SysUser startUser = wfCopyMapper.selectUserById(Long.parseLong(historicProcessInstance.getStartUserId()));
            taskVo.setStartUserId(startUser.getNickName());
            taskVo.setStartUserName(startUser.getNickName());
            SysDept sysDept = wfCopyMapper.selectDeptById(startUser.getDeptId());
            if (sysDept != null) {
                taskVo.setStartDeptName(sysDept.getDeptName());
            }
            // 流程变量
            taskVo.setProcVars(this.getProcessVariables(task.getId()));

            taskVoList.add(taskVo);
        }
        return taskVoList;
    }

    @Override
    public Table2DataInfo<WfTaskVo> selectPageFinishedProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        Page<WfTaskVo> page = new Page<>();
        List<WfTaskVo> allFinishedTasks = new ArrayList<>();
        
        // 1. 查询 Flowable 历史任务（如果 Flowable 服务可用）
        if (historyService != null && repositoryService != null) {
            HistoricTaskInstanceQuery taskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
            .includeProcessVariables()
            .finished()
            .taskAssignee(TaskUtils.getUserId())
            .orderByHistoricTaskInstanceEndTime()
            .desc();
        // 构建搜索条件
        ProcessUtils.buildProcessSearch(taskInstanceQuery, processQuery);
        List<HistoricTaskInstance> historicTaskInstanceList = taskInstanceQuery.list();
        for (HistoricTaskInstance histTask : historicTaskInstanceList) {
            WfTaskVo flowTask = new WfTaskVo();
            // 当前流程信息
            flowTask.setTaskId(histTask.getId());
            // 审批人员信息
            flowTask.setCreateTime(histTask.getCreateTime());
            flowTask.setFinishTime(histTask.getEndTime());
            flowTask.setDuration(DateUtil.formatBetween(histTask.getDurationInMillis(), BetweenFormatter.Level.SECOND));
            flowTask.setProcDefId(histTask.getProcessDefinitionId());
            flowTask.setTaskDefKey(histTask.getTaskDefinitionKey());
            flowTask.setTaskName(histTask.getName());

            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(histTask.getProcessDefinitionId())
                .singleResult();
            if (pd != null) {
                flowTask.setDeployId(pd.getDeploymentId());
                flowTask.setProcDefName(pd.getName());
                flowTask.setProcDefVersion(pd.getVersion());
            }
            flowTask.setProcInsId(histTask.getProcessInstanceId());
            flowTask.setHisProcInsId(histTask.getProcessInstanceId());

            // 流程发起人信息
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(histTask.getProcessInstanceId())
                .singleResult();
            if (historicProcessInstance != null && StringUtils.isNotBlank(historicProcessInstance.getStartUserId())) {
                SysUser startUser = wfCopyMapper.selectUserById(Long.parseLong(historicProcessInstance.getStartUserId()));
                if (startUser != null) {
                    flowTask.setStartUserId(startUser.getNickName());
                    flowTask.setStartUserName(startUser.getNickName());
                    SysDept sysDept = wfCopyMapper.selectDeptById(startUser.getDeptId());
                    if (sysDept != null) {
                        flowTask.setStartDeptName(sysDept.getDeptName());
                    }
                }
            }

            // 流程变量
            flowTask.setProcVars(this.getProcessVariables(histTask.getId()));

            allFinishedTasks.add(flowTask);
        }
        }
        
        // 2. 查询简化审批任务的已办列表
        Long currentUserId = SecurityUtils.getUserId();
        String userIdStr = String.valueOf(currentUserId);
        
        // 获取当前用户的角色ID列表和部门ID列表
        List<Long> roleIds = null;
        List<Long> deptIds = null;
        com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null && loginUser.getUser() != null) {
            if (CollUtil.isNotEmpty(loginUser.getUser().getRoles())) {
                roleIds = loginUser.getUser().getRoles().stream()
                    .map(SysRole::getRoleId)
                    .collect(Collectors.toList());
            }
            if (loginUser.getDeptId() != null) {
                deptIds = new ArrayList<>();
                deptIds.add(loginUser.getDeptId());
            }
        }
        
        // 查询简化审批任务的已办列表
        List<WfApprovalTask> finishedApprovalTasks = wfApprovalTaskMapper.selectFinishedListByUserId(userIdStr, roleIds, deptIds);
        
        // 批量查询任务名称（根据 extra_id）
        Map<String, String> taskNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(finishedApprovalTasks)) {
            for (WfApprovalTask approvalTask : finishedApprovalTasks) {
                String extraId = approvalTask.getExtraId();
                if (StringUtils.isNotBlank(extraId) && !taskNameMap.containsKey(extraId)) {
                    try {
                        R<String> response = projectTaskProcessFeignService.getTaskNameById(extraId, SecurityConstants.INNER);
                        if (response != null && response.getCode() == 200) {
                            String taskName = response.getData();
                            if (StringUtils.isBlank(taskName)) {
                                // 如果 data 为空，尝试从 msg 中获取（兼容旧版本）
                                taskName = response.getMsg();
                            }
                            taskNameMap.put(extraId, StringUtils.isNotBlank(taskName) ? taskName : "");
                        } else {
                            log.warn("查询任务名称失败，extraId={}，响应={}", extraId, response);
                            taskNameMap.put(extraId, "");
                        }
                    } catch (Exception e) {
                        log.error("查询任务名称异常，extraId={}", extraId, e);
                        taskNameMap.put(extraId, "");
                    }
                }
            }
        }
        
        // 将简化审批任务转换为 WfTaskVo
        for (WfApprovalTask approvalTask : finishedApprovalTasks) {
            WfTaskVo flowTask = convertApprovalTaskToWfTaskVo(approvalTask, taskNameMap);
            allFinishedTasks.add(flowTask);
        }
        
        // 3. 合并结果并按完成时间排序
        allFinishedTasks.sort((a, b) -> {
            Date aTime = a.getFinishTime();
            Date bTime = b.getFinishTime();
            if (aTime == null && bTime == null) {
                return 0;
            }
            if (aTime == null) {
                return 1;
            }
            if (bTime == null) {
                return -1;
            }
            return bTime.compareTo(aTime); // 降序
        });
        
        // 4. 手动分页
        int total = allFinishedTasks.size();
        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        int endIndex = Math.min(offset + pageQuery.getPageSize(), total);
        List<WfTaskVo> pagedList = offset < total ? allFinishedTasks.subList(offset, endIndex) : new ArrayList<>();
        
        page.setTotal(total);
        page.setRecords(pagedList);
        return Table2DataInfo.build(page);
    }

    /**
     * 将简化审批任务转换为 WfTaskVo
     * 
     * @param approvalTask 简化审批任务
     * @param taskNameMap 任务名称映射表（extraId -> taskName）
     * @return WfTaskVo
     */
    private WfTaskVo convertApprovalTaskToWfTaskVo(WfApprovalTask approvalTask, Map<String, String> taskNameMap) {
        WfTaskVo flowTask = new WfTaskVo();
        // 使用审批任务ID作为任务ID
        flowTask.setTaskId(approvalTask.getId());
        flowTask.setTaskName(approvalTask.getTitle());
        flowTask.setCreateTime(approvalTask.getCreatedTime());
        flowTask.setFinishTime(approvalTask.getApprovalTime());
        
        // 计算耗时
        if (approvalTask.getCreatedTime() != null && approvalTask.getApprovalTime() != null) {
            long duration = approvalTask.getApprovalTime().getTime() - approvalTask.getCreatedTime().getTime();
            flowTask.setDuration(DateUtil.formatBetween(duration, BetweenFormatter.Level.SECOND));
        }
        
        // 根据 extra_id 查询任务名称，如果查询到则使用任务名称，否则使用默认值
        String extraId = approvalTask.getExtraId();
        String taskName = null;
        if (StringUtils.isNotBlank(extraId) && taskNameMap != null) {
            taskName = taskNameMap.get(extraId);
        }
        // 如果查询到任务名称，使用任务名称；否则使用默认值"简化审批流程"
        flowTask.setProcDefName(StringUtils.isNotBlank(taskName) ? taskName : "简化审批流程");
        flowTask.setProcDefVersion(1);
        // 使用 extraId 作为流程实例ID（用于标识业务对象）
        flowTask.setProcInsId(approvalTask.getExtraId());
        flowTask.setProcDefId("simplified-approval-" + approvalTask.getType());
        
        // 发起人信息
        if (StringUtils.isNotBlank(approvalTask.getInitiatorId())) {
            try {
                SysUser startUser = wfCopyMapper.selectUserById(Long.parseLong(approvalTask.getInitiatorId()));
                if (startUser != null) {
                    flowTask.setStartUserId(startUser.getNickName());
                    flowTask.setStartUserName(startUser.getNickName());
                    SysDept sysDept = wfCopyMapper.selectDeptById(startUser.getDeptId());
                    if (sysDept != null) {
                        flowTask.setStartDeptName(sysDept.getDeptName());
                    }
                } else {
                    // 如果查询不到用户，使用审批任务中保存的发起人姓名
                    flowTask.setStartUserId(approvalTask.getInitiatorName());
                    flowTask.setStartUserName(approvalTask.getInitiatorName());
                }
            } catch (NumberFormatException e) {
                flowTask.setStartUserId(approvalTask.getInitiatorName());
                flowTask.setStartUserName(approvalTask.getInitiatorName());
            }
        }
        
        // 设置流程变量，包含审批任务的URL等信息
        Map<String, Object> procVars = new HashMap<>();
        procVars.put("url", approvalTask.getUrl());
        procVars.put("extraId", approvalTask.getExtraId());
        procVars.put("type", approvalTask.getType());
        procVars.put("approvalTaskId", approvalTask.getId());
        flowTask.setProcVars(procVars);
        
        return flowTask;
    }

    @Override
    public List<WfTaskVo> selectFinishedProcessList(ProcessQuery processQuery) {
        List<WfTaskVo> allFinishedTasks = new ArrayList<>();
        
        // 1. 查询 Flowable 历史任务
        HistoricTaskInstanceQuery taskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
                .includeProcessVariables()
                .finished()
                .taskAssignee(TaskUtils.getUserId())
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        // 构建搜索条件
        ProcessUtils.buildProcessSearch(taskInstanceQuery, processQuery);
        List<HistoricTaskInstance> historicTaskInstanceList = taskInstanceQuery.list();
        for (HistoricTaskInstance histTask : historicTaskInstanceList) {
            WfTaskVo flowTask = new WfTaskVo();
            // 当前流程信息
            flowTask.setTaskId(histTask.getId());
            // 审批人员信息
            flowTask.setCreateTime(histTask.getCreateTime());
            flowTask.setFinishTime(histTask.getEndTime());
            flowTask.setDuration(DateUtil.formatBetween(histTask.getDurationInMillis(), BetweenFormatter.Level.SECOND));
            flowTask.setProcDefId(histTask.getProcessDefinitionId());
            flowTask.setTaskDefKey(histTask.getTaskDefinitionKey());
            flowTask.setTaskName(histTask.getName());

            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(histTask.getProcessDefinitionId())
                    .singleResult();
            if (pd != null) {
                flowTask.setDeployId(pd.getDeploymentId());
                flowTask.setProcDefName(pd.getName());
                flowTask.setProcDefVersion(pd.getVersion());
            }
            flowTask.setProcInsId(histTask.getProcessInstanceId());
            flowTask.setHisProcInsId(histTask.getProcessInstanceId());

            // 流程发起人信息
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(histTask.getProcessInstanceId())
                    .singleResult();
            if (historicProcessInstance != null && StringUtils.isNotBlank(historicProcessInstance.getStartUserId())) {
                SysUser startUser = wfCopyMapper.selectUserById(Long.parseLong(historicProcessInstance.getStartUserId()));
                if (startUser != null) {
                    flowTask.setStartUserId(startUser.getNickName());
                    flowTask.setStartUserName(startUser.getNickName());
                    SysDept sysDept = wfCopyMapper.selectDeptById(startUser.getDeptId());
                    if (sysDept != null) {
                        flowTask.setStartDeptName(sysDept.getDeptName());
                    }
                }
            }

            // 流程变量
            flowTask.setProcVars(this.getProcessVariables(histTask.getId()));

            allFinishedTasks.add(flowTask);
        }
        
        // 2. 查询简化审批任务的已办列表
        Long currentUserId = SecurityUtils.getUserId();
        String userIdStr = String.valueOf(currentUserId);
        
        // 获取当前用户的角色ID列表和部门ID列表
        List<Long> roleIds = null;
        List<Long> deptIds = null;
        com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null && loginUser.getUser() != null) {
            if (CollUtil.isNotEmpty(loginUser.getUser().getRoles())) {
                roleIds = loginUser.getUser().getRoles().stream()
                    .map(SysRole::getRoleId)
                    .collect(Collectors.toList());
            }
            if (loginUser.getDeptId() != null) {
                deptIds = new ArrayList<>();
                deptIds.add(loginUser.getDeptId());
            }
        }
        
        // 查询简化审批任务的已办列表
        List<WfApprovalTask> finishedApprovalTasks = wfApprovalTaskMapper.selectFinishedListByUserId(userIdStr, roleIds, deptIds);
        
        // 批量查询任务名称（根据 extra_id）
        Map<String, String> taskNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(finishedApprovalTasks)) {
            for (WfApprovalTask approvalTask : finishedApprovalTasks) {
                String extraId = approvalTask.getExtraId();
                if (StringUtils.isNotBlank(extraId) && !taskNameMap.containsKey(extraId)) {
                    try {
                        R<String> response = projectTaskProcessFeignService.getTaskNameById(extraId, SecurityConstants.INNER);
                        if (response != null && response.getCode() == 200) {
                            String taskName = response.getData();
                            if (StringUtils.isBlank(taskName)) {
                                // 如果 data 为空，尝试从 msg 中获取（兼容旧版本）
                                taskName = response.getMsg();
                            }
                            taskNameMap.put(extraId, StringUtils.isNotBlank(taskName) ? taskName : "");
                        } else {
                            log.warn("查询任务名称失败，extraId={}，响应={}", extraId, response);
                            taskNameMap.put(extraId, "");
                        }
                    } catch (Exception e) {
                        log.error("查询任务名称异常，extraId={}", extraId, e);
                        taskNameMap.put(extraId, "");
                    }
                }
            }
        }
        
        // 将简化审批任务转换为 WfTaskVo
        for (WfApprovalTask approvalTask : finishedApprovalTasks) {
            WfTaskVo flowTask = convertApprovalTaskToWfTaskVo(approvalTask, taskNameMap);
            allFinishedTasks.add(flowTask);
        }
        
        // 3. 合并结果并按完成时间排序
        allFinishedTasks.sort((a, b) -> {
            Date aTime = a.getFinishTime();
            Date bTime = b.getFinishTime();
            if (aTime == null && bTime == null) {
                return 0;
            }
            if (aTime == null) {
                return 1;
            }
            if (bTime == null) {
                return -1;
            }
            return bTime.compareTo(aTime); // 降序
        });
        
        return allFinishedTasks;
    }

    /**
     * 根据流程定义ID启动任务审批流程实例
     * 如果 procDefId 为空，则从审批设置中获取
     *
     * @param taskId 任务ID
     * @param procDefId 流程定义Id（可选，如果为空则从审批设置中获取）
     * @param url 详情页URL
     * @param variables 流程变量
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTaskProcessByDefId(String taskId, String procDefId, String url, Map<String, Object> variables) {
        // 如果未提供流程定义ID，从审批设置中获取
        if (StringUtils.isBlank(procDefId)) {
            MaterialsApprovalSetVO approvalSet = deployService.queryApprovalSet(ProcessUtils.TASK_APPROVAL_TYPE, taskId);
            if (approvalSet != null && StringUtils.isNotBlank(approvalSet.getDefinitionId())) {
                procDefId = approvalSet.getDefinitionId();
            }
        }
        
        // 如果有流程定义ID，使用流程引擎启动流程
        if (StringUtils.isNotBlank(procDefId)) {
            ProcessDefinition processDefinition = getProcessDefinition(procDefId);
            if (processDefinition != null) {
                startTaskProcess(taskId, processDefinition, url, variables);
                return;
            }
        }
        
        // 如果没有流程定义，直接创建审批记录（不启动流程引擎）
        startTaskProcessWithoutFlow(taskId, url, variables);
    }

    /**
     * 根据流程定义ID启动项目发布流程实例
     *
     * @param procDefId 流程定义Id
     * @param variables 流程变量
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int startProjectProcessByDefId(String projectId, String procDefId, String url, Map<String, Object> variables) {
        ProcessDefinition processDefinition = getProcessDefinition(procDefId);
        startProjectProcess(projectId, processDefinition, url, variables);
        return 1;
    }

    /**
     * 根据流程定义ID启动采购入库审批流程实例
     * @param inboundId
     * @param procDefId
     * @param url
     * @param variables
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startInboundProcessByDefId(String inboundId, String procDefId, String url, Map<String, Object> variables) {
        ProcessDefinition processDefinition = getProcessDefinition(procDefId);
        startInboundProcess(inboundId, processDefinition, url, variables);
    }

    /**
     * 根据流程定义ID启动采购退货出库流程实例
     * @param outboundId
     * @param procDefId
     * @param url
     * @param variables
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startOutboundProcessByDefId(String outboundId, String procDefId, String url, Map<String, Object> variables) {
        ProcessDefinition processDefinition = getProcessDefinition(procDefId);
        startOutboundProcess(outboundId, processDefinition, url, variables);
    }

    @Override
    public void startProviderProcessByDefId(String providerId, String procDefId, String url, Map<String, Object> variables) {
        ProcessDefinition processDefinition = getProcessDefinition(procDefId);
        startProviderProcess(providerId, processDefinition, url, variables);
    }


    /**
     * 根据流程定义ID启动其他入库流程实例
     * @param otherIntoId
     * @param procDefId
     * @param url
     * @param variables
     */
    @Override
    public void startOtherIntoProcessByDefId(String otherIntoId, String procDefId, String url, Map<String, Object> variables) {
        ProcessDefinition processDefinition = getProcessDefinition(procDefId);
        startOtherIntoProcess(otherIntoId, processDefinition, url, variables);
    }

    /**
     * 根据流程定义ID启动其他出库流程实例
     * @param otherOutId
     * @param procDefId
     * @param url
     * @param variables
     */
    @Override
    public void startOtherOutProcessByDefId(String otherOutId, String procDefId, String url, Map<String, Object> variables) {
        ProcessDefinition processDefinition = getProcessDefinition(procDefId);
        startOtherOutProcess(otherOutId, processDefinition, url, variables);
    }

    /**
     * 根据流程定义ID启动归还入库流程实例
     * @param returnIntoId
     * @param procDefId
     * @param url
     * @param variables
     */
    @Override
    public void startReturnIntoProcessByDefId(String returnIntoId, String procDefId, String url, Map<String, Object> variables) {
        ProcessDefinition processDefinition = getProcessDefinition(procDefId);
        startReturnIntoProcess(returnIntoId, processDefinition, url, variables);
    }

    /**
     * 根据流程定义ID启动物料报废流程实例
     * @param materialsIds
     * @param procDefId
     * @param url
     * @param variables
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startScrappedProcessByDefId(String materialsIds, String procDefId, String url, Map<String, Object> variables) {

        List<String> ids = Arrays.asList(materialsIds.split(","));
        List<String> errorIds = new ArrayList<>(10);
        List<String> eIds = new ArrayList<>(10);
//        ids.forEach(id -> {
//            MaterialsUseless mus = materialsUselessMapper.selectById(id);
//            if (mus == null) {
//                errorIds.add(id);
//            } else {
//                if (!USELESS.equals(mus.getResolution()) || DAI_DING.equals(mus.getDangerous())) {
//                    errorIds.add(id);
//                }
//                if (mus.getApproved() != 0) {
//                    eIds.add(id);
//                }
//            }
//        });
        if (CollectionUtils.isNotEmpty(errorIds)) {
            throw new ServiceException("[" + String.join(",", errorIds) + "]" + "处理意见不是报废且危废待定，请重新选择之后发起审批");
        }
        if (CollectionUtils.isNotEmpty(eIds)) {
            throw new ServiceException("[" + String.join(",", eIds) + "]" + "不是未审核状态，请重新选择之后发起审批");
        }
        // 判断责任人是否为空
//        List<MaterialsChangeRecords> materialsChangeRecords = materialsChangeRecordsMapper.selectBatchIds(ids);
//        if (CollectionUtils.isNotEmpty(materialsChangeRecords)) {
//            StringBuilder msg = new StringBuilder();
//            for (MaterialsChangeRecords mcr : materialsChangeRecords) {
//                if (mcr.getPrincipalId() == null) {
//                    msg.append(mcr.getId()).append(",");
//                }
//            }
//            if (StringUtils.isNotBlank(msg.toString())) {
//                throw new ServiceException("[" + msg.substring(0, msg.toString().length() - 1) + "]" + "未设置责任人，请重新选择之后发起审批");
//            }
//        }
        ProcessDefinition processDefinition = getProcessDefinition(procDefId);
        startScrappedProcess(ids, processDefinition, url, variables);
    }

    /**
     * 获取流程定义
     * @param procDefId
     * @return
     */
    private ProcessDefinition getProcessDefinition(String procDefId) {
        return repositoryService.createProcessDefinitionQuery().processDefinitionId(procDefId).singleResult();
    }

    /**
     * 根据类型判断是否有进行中的流程 有则不能发起审批 无则可以并返回 WfTaskProcess
     * @param ids
     * @return
     */
    private List<WfMaterialsScrappedProcess> getScrappedProcess(List<String> ids) {
        LambdaQueryWrapper<WfMaterialsScrappedProcess> qw = new LambdaQueryWrapper<>();
        qw.in(WfMaterialsScrappedProcess::getMaterialId, ids);
        List<WfMaterialsScrappedProcess> scrappedProcessList = wfMaterialsScrappedProcessMapper.selectList(qw);
        if (CollectionUtils.isNotEmpty(scrappedProcessList)) {
            scrappedProcessList.forEach(a -> {
                if (StringUtils.isNotBlank(a.getInstanceId())) {
                    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                            .processInstanceId(a.getInstanceId())
                            .singleResult();
                    if (historicProcessInstance != null) {
                        if (StringUtils.isBlank(historicProcessInstance.getEndActivityId())) {
                            // 流程未结束不允许重新发起流程
                            throw new ServiceException("存在已发起的审批流程，请重新选择之后再发起");
                        } else {
                            List<Comment> comments = taskService.getProcessInstanceComments(a.getInstanceId());
                            comments.sort(Comparator.comparing(Comment::getTime).reversed());
                            if ("1".equals(comments.get(0).getType())) {
                                throw new ServiceException("存在已通过的审批流程，请重新选择之后再发起");
                            }
                        }
                    }
                }
            });

        } else {
            scrappedProcessList = deployService.insertScrappedProcess(ids, null);
        }
        // 更新 MaterialsUseless
//        LambdaUpdateChainWrapper<MaterialsUseless> updateWrapper = new LambdaUpdateChainWrapper<>(materialsUselessMapper);
//        updateWrapper.in(MaterialsUseless::getRecordId, ids).set(MaterialsUseless::getApproved, 1);
//        updateWrapper.update();
        return scrappedProcessList;
    }

    /**
     * 根据类型判断是否有进行中的流程 有则不能发起审批 无则可以并返回 WfTaskProcess
     * @param extraId
     * @param type
     * @return
     */
    private WfTaskProcess getWfTaskProcess(String extraId, String type) {
        // 使用 Feign 调用 pmhub-project 服务查询
        R<WfTaskProcess> queryResult = projectTaskProcessFeignService.getByExtraIdAndType(extraId, type, SecurityConstants.INNER);
        WfTaskProcess wfTaskProcess = queryResult.getData();
        MaterialsApprovalSetVO materialsApprovalSetVO;
        if (ProjectStatusEnum.TASK.getStatusName().equals(type)) {
            materialsApprovalSetVO = deployService.queryApprovalSet(type, extraId);
        } else {
            materialsApprovalSetVO = deployService.queryApprovalSet(type, null);
        }

        if (wfTaskProcess != null && StringUtils.isNotBlank(wfTaskProcess.getInstanceId())) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(wfTaskProcess.getInstanceId())
                    .singleResult();
            if (historicProcessInstance != null) {
                if (StringUtils.isBlank(historicProcessInstance.getEndActivityId())) {
                    // 流程未结束不允许重新发起流程
                    throw new ServiceException("该审批流程已发起，无需再次发起");
                } else {
                    List<Comment> list = taskService.getProcessInstanceComments(wfTaskProcess.getInstanceId());
                    list.sort(Comparator.comparing(Comment::getTime).reversed());
                    if ("1".equals(list.get(0).getType())) {
                        throw new ServiceException("该审批流程已通过，无需再次发起");
                    }
                    if ("3".equals(list.get(0).getType())) {
                        // 使用 Feign 调用更新任务状态为进行中
                        R<Void> updateResult = projectTaskProcessFeignService.updateTaskStatus3(extraId, SecurityConstants.INNER);
                        if (updateResult.getCode() != 200) {
                            log.warn("更新任务状态失败: {}", updateResult.getMsg());
                        }
                    }
                }
            }
            if (ts.contains(type)) {
                // 对于任务类型，发起审批时 approved 应该设置为 "0"（已开启）
                if (ProjectStatusEnum.TASK.getStatusName().equals(type)) {
                    wfTaskProcess.setApproved("0");
                } else {
                    wfTaskProcess.setApproved(materialsApprovalSetVO.getApproved());
                }
                wfTaskProcess.setDeploymentId(materialsApprovalSetVO.getDeploymentId());
                wfTaskProcess.setDefinitionId(materialsApprovalSetVO.getDefinitionId());
            }

        } else {
            if (ProjectStatusEnum.PROJECT.getStatusName().equals(type) || ProjectStatusEnum.TASK.getStatusName().equals(type)) {
                // 使用 Feign 调用更新任务状态为进行中
                R<Void> updateResult = projectTaskProcessFeignService.updateTaskStatus3(extraId, SecurityConstants.INNER);
                if (updateResult.getCode() != 200) {
                    log.warn("更新任务状态失败: {}", updateResult.getMsg());
                }
            }
//            if (types.contains(type)) {
//                MaterialsChangeRecords materialsChangeRecords = materialsChangeRecordsMapper.selectById(extraId);
//                if (materialsChangeRecords.getProcessState() != 0) {
//                    throw new ServiceException("[" + extraId + "]" + "不是未审核状态，无需重新发起审批");
//                }
//            }
            // 新增
            // 对于任务类型，发起审批时 approved 应该设置为 "0"（已开启）
            String approvedValue = ProjectStatusEnum.TASK.getStatusName().equals(type) ? "0" : materialsApprovalSetVO.getApproved();
            wfTaskProcess = deployService.insertWfTaskProcess(extraId, type, approvedValue
                    , materialsApprovalSetVO.getDefinitionId(), materialsApprovalSetVO.getDeploymentId());
        }
        return wfTaskProcess;
    }


    /**
     * 启动公共流程
     * @param procDef
     * @param type
     * @param url
     * @param variables
     * @return
     */
    private ProcessInstance startCommonProcess(ProcessDefinition procDef, String type, String url, Map<String, Object> variables) {

        // 详情地址
        variables.put(ProcessUtils.TASK_DETAIL_URL_KEY, url);
        // 审批类型 project task 等等
        variables.put(ProcessUtils.APPROVAL_TYPE, type);
        // 直属上级
        queryLeaderId(variables, SecurityUtils.getUserId());
        if (ObjectUtil.isNotNull(procDef) && procDef.isSuspended()) {
            throw new ServiceException("流程已被挂起，请先激活流程");
        }
        // 设置流程发起人Id到流程中
        String userIdStr = TaskUtils.getUserId();
        identityService.setAuthenticatedUserId(userIdStr);
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, userIdStr);
        // 发起流程实例
        return runtimeService.startProcessInstanceById(procDef.getId(), variables);
        // 第一个用户任务为发起人，则自动完成任务
        // wfTaskService.startFirstTask(processInstance, variables);
    }

    /**
     * 将 instanceId、url 和 taskId 更新
     * @param wfTaskProcess
     * @param processInstance
     * @param url
     */
    private void updateWfTaskProcess(WfTaskProcess wfTaskProcess, ProcessInstance processInstance, String url) {
        wfTaskProcess.setInstanceId(processInstance.getId());
        // 当前所处流程
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
        if (CollUtil.isNotEmpty(taskList)) {
            wfTaskProcess.setTaskId(taskList.get(0).getId());
        } else {
            List<HistoricTaskInstance> historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getProcessInstanceId()).orderByHistoricTaskInstanceEndTime().desc().list();
            if (CollUtil.isNotEmpty(historicTaskInstance)) {
                wfTaskProcess.setTaskId(historicTaskInstance.get(0).getId());
            }
        }
        wfTaskProcess.setUrl(url);
        // 使用 Feign 调用更新
        R<WfTaskProcess> updateResult = projectTaskProcessFeignService.insertOrUpdate(wfTaskProcess, SecurityConstants.INNER);
        if (updateResult.getCode() != 200) {
            log.warn("更新任务流程失败: {}", updateResult.getMsg());
        }
    }

    /**
     * 将 instanceId、url 和 taskId 更新
     * @param scrappedProcess
     * @param processInstance
     * @param url
     */
    private void updateScrappedProcess(List<WfMaterialsScrappedProcess> scrappedProcess, ProcessInstance processInstance, String url) {
        MaterialsApprovalSetVO materialsApprovalSetVO = deployService.queryApprovalSet(ProcessUtils.SCRAPPED_OUT_APPROVAL_TYPE, null);
        if (CollectionUtils.isNotEmpty(scrappedProcess)) {
            String taskId = null;
            // 当前所处流程
            List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
            if (CollUtil.isNotEmpty(taskList)) {
                taskId = taskList.get(0).getId();
            } else {
                List<HistoricTaskInstance> historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getProcessInstanceId()).orderByHistoricTaskInstanceEndTime().desc().list();
                if (CollUtil.isNotEmpty(historicTaskInstance)) {
                    taskId = historicTaskInstance.get(0).getId();
                }
            }
            List<String> ids = scrappedProcess.stream().map(WfMaterialsScrappedProcess::getMaterialId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(ids)) {
                LambdaUpdateChainWrapper<WfMaterialsScrappedProcess> updateWrapper = new LambdaUpdateChainWrapper<>(wfMaterialsScrappedProcessMapper);
                updateWrapper.in(WfMaterialsScrappedProcess::getMaterialId, ids)
                        .set(WfMaterialsScrappedProcess::getApproved, materialsApprovalSetVO.getApproved())
                        .set(WfMaterialsScrappedProcess::getType, materialsApprovalSetVO.getType())
                        .set(WfMaterialsScrappedProcess::getDefinitionId, materialsApprovalSetVO.getDefinitionId())
                        .set(WfMaterialsScrappedProcess::getDeploymentId, materialsApprovalSetVO.getDeploymentId())
                        .set(WfMaterialsScrappedProcess::getInstanceId, processInstance.getId())
                        .set(WfMaterialsScrappedProcess::getTaskId, taskId)
                        .set(WfMaterialsScrappedProcess::getUrl, url);
                updateWrapper.update();
            }
        }

    }

    /**
     * 查询直属上级id
     * @param variables
     * @param userId
     */
    private void queryLeaderId(Map<String, Object> variables, Long userId) {
        SysUser sysUser = wfCopyMapper.selectUserById(userId);
        if (StringUtils.isNotBlank(sysUser.getLeaderId())) {
            // 直属上级
            variables.put(ProcessUtils.LEADER_LIST, Arrays.asList(sysUser.getLeaderId().split(",")));
        }
    }

    /**
     * 通过DefinitionKey启动流程
     * @param procDefKey 流程定义Key
     * @param variables 扩展参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProcessByDefKey(String procDefKey, Map<String, Object> variables) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(procDefKey).latestVersion().singleResult();
            startProcess(processDefinition, variables);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("流程启动错误");
        }
    }

    /**
     * 读取xml文件
     * @param processDefId 流程定义ID
     */
    @Override
    public String queryBpmnXmlById(String processDefId) {
        if (StringUtils.isBlank(processDefId)) {
            throw new ServiceException("未设置流程定义，请联系管理员");
        }
        InputStream inputStream = repositoryService.getProcessModel(processDefId);
        try {
            return IoUtil.readUtf8(inputStream);
        } catch (IORuntimeException exception) {
            throw new RuntimeException("加载xml文件异常");
        }
    }

    /**
     * 流程详情信息
     *
     * @param procInsId 流程实例ID
     * @param deployId 流程部署ID
     * @param taskId 任务ID
     * @return
     */
    @Override
    public WfDetailVo queryProcessDetail(String procInsId, String deployId, String taskId) {
        WfDetailVo detailVo = new WfDetailVo();
        
        // 判断是否是简化审批流程：如果 taskId 在审批任务表中存在，就是简化审批流程
        WfApprovalTask approvalTask = null;
        if (StringUtils.isNotBlank(taskId)) {
            approvalTask = wfApprovalTaskMapper.selectById(taskId);
        }
        
        if (approvalTask != null) {
            String extraId = approvalTask.getExtraId();
            String type = approvalTask.getType();
            detailVo.setTaskFormData(null);
            detailVo.setHistoryProcNodeList(historyProcNodeListForSimplifiedApproval(extraId, type));
            return detailVo;
        }

        if (StringUtils.isBlank(procInsId)) {
            throw new ServiceException("未发布审批，不存在审批进度");
        }

        HistoricTaskInstance taskIns = historyService.createHistoricTaskInstanceQuery()
            .taskId(taskId)
            .includeIdentityLinks()
            .includeProcessVariables()
            .includeTaskLocalVariables()
            .singleResult();
        if (taskIns == null) {
            throw new ServiceException("没有可办理的任务！");
        }
        detailVo.setTaskFormData(currTaskFormData(deployId, taskIns));
        detailVo.setHistoryProcNodeList(historyProcNodeList(procInsId));
        return detailVo;
    }

    /**
     * 启动流程实例
     */
    private void startProcess(ProcessDefinition procDef, Map<String, Object> variables) {
        if (ObjectUtil.isNotNull(procDef) && procDef.isSuspended()) {
            throw new ServiceException("流程已被挂起，请先激活流程");
        }
        // 设置流程发起人Id到流程中
        String userIdStr = TaskUtils.getUserId();
        identityService.setAuthenticatedUserId(userIdStr);
        variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, userIdStr);
        // 发起流程实例
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDef.getId(), variables);
        // 第一个用户任务为发起人，则自动完成任务
        wfTaskService.startFirstTask(processInstance, variables);
    }

    /**
     * 启动任务发布实例
     * @param taskId
     * @param procDef
     * @param url
     * @param variables
     */
    private void startTaskProcess(String taskId, ProcessDefinition procDef, String url, Map<String, Object> variables) {
        // 任务审批相关逻辑
        // 使用 Feign 调用查询任务执行状态
        R<Integer> statusResult = projectTaskProcessFeignService.getTaskExecuteStatus(taskId, SecurityConstants.INNER);
        if (statusResult.getCode() != 200 || statusResult.getData() == null) {
            throw new ServiceException("查询任务执行状态失败: " + (statusResult.getMsg() != null ? statusResult.getMsg() : "未知错误"));
        }
        Integer status = statusResult.getData();
        if (!ProjectTaskStatusEnum.FINISHED.getStatus().equals(status)) {
            throw new ServiceException("执行状态为已完成才能发起审批");
        }

        WfTaskProcess wfTaskProcess = getWfTaskProcess(taskId, ProcessUtils.TASK_APPROVAL_TYPE);
        ProcessInstance processInstance = startCommonProcess(procDef, ProcessUtils.TASK_APPROVAL_TYPE, url, variables);
        updateWfTaskProcess(wfTaskProcess, processInstance, url);
        
        // 从审批设置中读取审批人信息并应用到任务
        applyApprovalInfoToTask(taskId, processInstance);
    }
    
    /**
     * 启动任务审批（无流程定义，不使用流程引擎）
     * @param taskId 任务ID
     * @param url 详情页URL
     * @param variables 流程变量
     */
    private void startTaskProcessWithoutFlow(String taskId, String url, Map<String, Object> variables) {
        // 任务审批相关逻辑
        // 使用 Feign 调用查询任务执行状态
        R<Integer> statusResult = projectTaskProcessFeignService.getTaskExecuteStatus(taskId, SecurityConstants.INNER);
        if (statusResult.getCode() != 200 || statusResult.getData() == null) {
            throw new ServiceException("查询任务执行状态失败: " + (statusResult.getMsg() != null ? statusResult.getMsg() : "未知错误"));
        }
        Integer status = statusResult.getData();
        if (!ProjectTaskStatusEnum.FINISHED.getStatus().equals(status)) {
            throw new ServiceException("执行状态为已完成才能发起审批");
        }

        // 获取或创建审批记录
        WfTaskProcess wfTaskProcess = getWfTaskProcessWithoutFlow(taskId, ProcessUtils.TASK_APPROVAL_TYPE);
        
        // 设置URL，不启动流程引擎，所以instanceId为空
        wfTaskProcess.setUrl(url);
        // 确保 approved 为 "0"（已开启审批）
        wfTaskProcess.setApproved("0");
        
        // 创建简化审批任务
        createSimplifiedApprovalTasks(taskId, ProcessUtils.TASK_APPROVAL_TYPE, url);
        
        // 使用 Feign 调用更新任务流程
        R<WfTaskProcess> updateResult = projectTaskProcessFeignService.insertOrUpdate(wfTaskProcess, SecurityConstants.INNER);
        if (updateResult.getCode() != 200) {
            log.warn("更新任务流程失败: {}", updateResult.getMsg());
            throw new ServiceException("更新任务流程失败: " + updateResult.getMsg());
        }
    }
    
    /**
     * 创建简化审批任务（不依赖Flowable流程定义）
     * @param extraId 业务ID（如任务ID）
     * @param type 审批类型
     * @param url 详情页URL
     * @return 第一个审批任务的ID
     */
    private String createSimplifiedApprovalTasks(String extraId, String type, String url) {
        // 查询审批设置
        MaterialsApprovalSetVO approvalSet = deployService.queryApprovalSet(type, extraId);
        if (approvalSet == null || StringUtils.isBlank(approvalSet.getApprovalInfo())) {
            throw new ServiceException("未设置审批人信息，请先设置审批人");
        }
        
        // 解析审批人信息
        @SuppressWarnings("unchecked")
        Map<String, Object> approvalInfo = (Map<String, Object>) JsonUtils.parseObject(approvalSet.getApprovalInfo(), Map.class);
        if (approvalInfo == null) {
            throw new ServiceException("审批人信息格式错误");
        }
        
        // 获取发起人信息
        Long initiatorId = SecurityUtils.getUserId();
        SysUser initiator = wfCopyMapper.selectUserById(initiatorId);
        String initiatorName = initiator != null ? initiator.getNickName() : "";
        
        // 获取任务标题（可以从业务对象中获取，这里简化处理）
        String title = "任务审批：" + extraId;
        
        // 根据审批人类型创建审批任务
        String dataType = (String) approvalInfo.get("dataType");
        List<WfApprovalTask> approvalTasks = new ArrayList<>();
        String firstTaskId = null;
        
        if ("USERS".equals(dataType)) {
            // 指定用户
            String assignee = (String) approvalInfo.get("assignee");
            String candidateUsers = (String) approvalInfo.get("candidateUsers");
            
            if (StringUtils.isNotBlank(assignee)) {
                // 单个审批人
                WfApprovalTask task = createApprovalTask(extraId, type, title, url, assignee, "user", assignee, initiatorId.toString(), initiatorName);
                // 查询审批人姓名
                try {
                    SysUser user = wfCopyMapper.selectUserById(Long.parseLong(assignee));
                    if (user != null) {
                        task.setApproverName(user.getNickName());
                    }
                } catch (Exception e) {
                    log.warn("查询审批人信息失败，approverId: {}", assignee, e);
                }
                approvalTasks.add(task);
                firstTaskId = task.getId();
            } else if (StringUtils.isNotBlank(candidateUsers)) {
                // 多个审批人
                String[] userIds = candidateUsers.split(",");
                for (String userId : userIds) {
                    if (StringUtils.isNotBlank(userId)) {
                        userId = userId.trim();
                        SysUser user = wfCopyMapper.selectUserById(Long.parseLong(userId));
                        String userName = user != null ? user.getNickName() : "";
                        WfApprovalTask task = createApprovalTask(extraId, type, title, url, userId, "user", candidateUsers, initiatorId.toString(), initiatorName);
                        task.setApproverName(userName);
                        approvalTasks.add(task);
                        if (firstTaskId == null) {
                            firstTaskId = task.getId();
                        }
                    }
                }
            }
        } else if ("ROLES".equals(dataType)) {
            // 角色
            String candidateGroups = (String) approvalInfo.get("candidateGroups");
            if (StringUtils.isNotBlank(candidateGroups)) {
                String[] roleIds = candidateGroups.split(",");
                for (String roleId : roleIds) {
                    if (StringUtils.isNotBlank(roleId)) {
                        String rawRoleId = roleId.trim();
                        String roleIdForDb = rawRoleId.startsWith(TaskConstants.ROLE_GROUP_PREFIX)
                                ? StringUtils.removeStart(rawRoleId, TaskConstants.ROLE_GROUP_PREFIX)
                                : rawRoleId;
                        if (StringUtils.isBlank(roleIdForDb)) {
                            continue;
                        }
                        // 查询该角色下的所有用户
                        List<Long> userIds = wfCopyMapper.selectUserIdsByRoleId(Long.parseLong(roleIdForDb));
                        for (Long userId : userIds) {
                            SysUser user = wfCopyMapper.selectUserById(userId);
                            if (user != null) {
                                WfApprovalTask task = createApprovalTask(extraId, type, title, url, userId.toString(), "role", roleIdForDb, initiatorId.toString(), initiatorName);
                                task.setApproverName(user.getNickName());
                                approvalTasks.add(task);
                                if (firstTaskId == null) {
                                    firstTaskId = task.getId();
                                }
                            }
                        }
                    }
                }
            }
        } else if ("DEPTS".equals(dataType)) {
            // 部门
            String candidateGroups = (String) approvalInfo.get("candidateGroups");
            if (StringUtils.isNotBlank(candidateGroups)) {
                String[] deptIds = candidateGroups.split(",");
                List<String> deptIdList = new ArrayList<>();
                for (String deptId : deptIds) {
                    if (StringUtils.isNotBlank(deptId)) {
                        deptId = deptId.trim().startsWith("DEPT") ? deptId.trim().substring(4) : deptId.trim();
                        deptIdList.add(deptId);
                    }
                }
                // 查询这些部门下的所有用户
                List<Long> userIds = wfCopyMapper.selectUserIds(deptIdList);
                // 将处理后的部门ID列表用逗号连接，用于存储到approver_value（去掉DEPT前缀）
                String processedDeptIds = String.join(",", deptIdList);
                for (Long userId : userIds) {
                    SysUser user = wfCopyMapper.selectUserById(userId);
                    if (user != null) {
                        // 使用处理后的部门ID列表（去掉DEPT前缀），这样查询时才能正确匹配
                        WfApprovalTask task = createApprovalTask(extraId, type, title, url, userId.toString(), "dept", processedDeptIds, initiatorId.toString(), initiatorName);
                        task.setApproverName(user.getNickName());
                        approvalTasks.add(task);
                        if (firstTaskId == null) {
                            firstTaskId = task.getId();
                        }
                    }
                }
            }
        } else if ("INITIATOR".equals(dataType)) {
            // 发起人（按业务含义：发起人的直属上级）
            SysUser initiatorUser = wfCopyMapper.selectUserById(initiatorId);
            String leaderIdsStr = initiatorUser != null ? initiatorUser.getLeaderId() : null;
            if (StringUtils.isNotBlank(leaderIdsStr)) {
                String[] leaderIds = leaderIdsStr.split(",");
                for (String leaderId : leaderIds) {
                    if (StringUtils.isBlank(leaderId)) {
                        continue;
                    }
                    leaderId = leaderId.trim();
                    SysUser leader = wfCopyMapper.selectUserById(Long.parseLong(leaderId));
                    String leaderName = leader != null ? leader.getNickName() : "";
                    WfApprovalTask task = createApprovalTask(extraId, type, title, url, leaderId, "user", leaderId, initiatorId.toString(), initiatorName);
                    task.setApproverName(leaderName);
                    approvalTasks.add(task);
                    if (firstTaskId == null) {
                        firstTaskId = task.getId();
                    }
                }
            } else {
                // 无直属上级配置时，回退为由发起人自审
                WfApprovalTask task = createApprovalTask(extraId, type, title, url, initiatorId.toString(), "user", initiatorId.toString(), initiatorId.toString(), initiatorName);
                task.setApproverName(initiatorName);
                approvalTasks.add(task);
                firstTaskId = task.getId();
            }
        }
        
        // 批量保存审批任务
        if (CollUtil.isNotEmpty(approvalTasks)) {
            for (WfApprovalTask task : approvalTasks) {
                wfApprovalTaskMapper.insert(task);
            }
        } else {
            throw new ServiceException("未找到有效的审批人");
        }
        
        return firstTaskId;
    }
    
    /**
     * 创建单个审批任务对象
     */
    private WfApprovalTask createApprovalTask(String extraId, String type, String title, String url, 
                                               String approverId, String approverType, String approverValue,
                                               String initiatorId, String initiatorName) {
        WfApprovalTask task = new WfApprovalTask();
        task.setExtraId(extraId);
        task.setType(type);
        task.setTitle(title);
        task.setUrl(url);
        task.setStatus("pending");
        task.setApproverId(approverId);
        task.setApproverType(approverType);
        task.setApproverValue(approverValue);
        task.setInitiatorId(initiatorId);
        task.setInitiatorName(initiatorName);
        task.setCreatedBy(SecurityUtils.getUsername());
        task.setCreatedTime(new Date());
        return task;
    }
    
    /**
     * 获取或创建任务审批记录（无流程定义版本）
     * @param extraId 任务ID
     * @param type 审批类型
     * @return WfTaskProcess
     */
    private WfTaskProcess getWfTaskProcessWithoutFlow(String extraId, String type) {
        // 使用 Feign 调用 pmhub-project 服务查询
        R<WfTaskProcess> queryResult = projectTaskProcessFeignService.getByExtraIdAndType(extraId, type, SecurityConstants.INNER);
        WfTaskProcess wfTaskProcess = queryResult.getData();

        if (wfTaskProcess != null) {
            // 如果已有审批记录
            if (StringUtils.isNotBlank(wfTaskProcess.getInstanceId())) {
                // 如果有流程实例ID，检查是否已完成
                HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(wfTaskProcess.getInstanceId())
                        .singleResult();
                if (historicProcessInstance != null) {
                    if (StringUtils.isBlank(historicProcessInstance.getEndActivityId())) {
                        // 流程未结束不允许重新发起流程
                        throw new ServiceException("该审批流程已发起，无需再次发起");
                    } else {
                        List<Comment> list = taskService.getProcessInstanceComments(wfTaskProcess.getInstanceId());
                        list.sort(Comparator.comparing(Comment::getTime).reversed());
                        if ("1".equals(list.get(0).getType())) {
                            throw new ServiceException("该审批流程已通过，无需再次发起");
                        }
                        if ("3".equals(list.get(0).getType())) {
                            // 使用 Feign 调用更新任务状态为进行中
                            R<Void> updateResult = projectTaskProcessFeignService.updateTaskStatus3(extraId, SecurityConstants.INNER);
                            if (updateResult.getCode() != 200) {
                                log.warn("更新任务状态失败: {}", updateResult.getMsg());
                            }
                        }
                    }
                }
            }
            // 如果没有流程实例ID，说明是简化版审批（无流程定义），直接返回现有记录
            return wfTaskProcess;
        } else {
            // 如果没有审批记录，创建新的
            if (ProjectStatusEnum.PROJECT.getStatusName().equals(type) || ProjectStatusEnum.TASK.getStatusName().equals(type)) {
                // 使用 Feign 调用更新任务状态为进行中
                R<Void> updateResult = projectTaskProcessFeignService.updateTaskStatus3(extraId, SecurityConstants.INNER);
                if (updateResult.getCode() != 200) {
                    log.warn("更新任务状态失败: {}", updateResult.getMsg());
                }
            }
            // 新增审批记录（无流程定义，definitionId 和 deploymentId 为空）
            // 发起审批时，approved 应该设置为 "0"（已开启），而不是从审批设置中读取
            wfTaskProcess = deployService.insertWfTaskProcess(extraId, type, 
                    "0", // 发起审批时，approved 设置为 "0" 表示已开启
                    null, // definitionId 为空
                    null); // deploymentId 为空
            return wfTaskProcess;
        }
    }
    
    /**
     * 从审批设置中读取审批人信息并应用到流程任务
     * @param taskId 任务ID
     * @param processInstance 流程实例
     */
    private void applyApprovalInfoToTask(String taskId, ProcessInstance processInstance) {
        try {
            // 查询审批设置
            MaterialsApprovalSetVO approvalSet = deployService.queryApprovalSet(ProcessUtils.TASK_APPROVAL_TYPE, taskId);
            if (approvalSet == null || StringUtils.isBlank(approvalSet.getApprovalInfo())) {
                return;
            }
            
            // 解析审批人信息
            @SuppressWarnings("unchecked")
            Map<String, Object> approvalInfo = (Map<String, Object>) JsonUtils.parseObject(approvalSet.getApprovalInfo(), Map.class);
            if (approvalInfo == null) {
                return;
            }
            
            // 获取流程中的第一个任务
            List<Task> taskList = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .list();
            if (CollUtil.isEmpty(taskList)) {
                return;
            }
            
            Task firstTask = taskList.get(0);
            String dataType = (String) approvalInfo.get("dataType");
            
            // 根据审批人类型设置任务审批人
            if ("USERS".equals(dataType)) {
                // 指定用户
                String assignee = (String) approvalInfo.get("assignee");
                String candidateUsers = (String) approvalInfo.get("candidateUsers");
                if (StringUtils.isNotBlank(assignee)) {
                    // 单个审批人，直接设置 assignee
                    taskService.setAssignee(firstTask.getId(), assignee);
                } else if (StringUtils.isNotBlank(candidateUsers)) {
                    // 多个审批人，设置为候选用户
                    String[] userIds = candidateUsers.split(",");
                    for (String userId : userIds) {
                        if (StringUtils.isNotBlank(userId)) {
                            taskService.addCandidateUser(firstTask.getId(), userId.trim());
                        }
                    }
                }
            } else if ("ROLES".equals(dataType)) {
                // 角色
                String candidateGroups = (String) approvalInfo.get("candidateGroups");
                if (StringUtils.isNotBlank(candidateGroups)) {
                    String[] roleIds = candidateGroups.split(",");
                    for (String roleId : roleIds) {
                        if (StringUtils.isNotBlank(roleId)) {
                            // 移除 ROLE 前缀
                            String roleIdStr = roleId.trim().startsWith("ROLE") 
                                    ? roleId.trim().substring(4) 
                                    : roleId.trim();
                            taskService.addCandidateGroup(firstTask.getId(), roleIdStr);
                        }
                    }
                }
            } else if ("DEPTS".equals(dataType)) {
                // 部门
                String candidateGroups = (String) approvalInfo.get("candidateGroups");
                if (StringUtils.isNotBlank(candidateGroups)) {
                    String[] deptIds = candidateGroups.split(",");
                    for (String deptId : deptIds) {
                        if (StringUtils.isNotBlank(deptId)) {
                            // 移除 DEPT 前缀
                            String deptIdStr = deptId.trim().startsWith("DEPT") 
                                    ? deptId.trim().substring(4) 
                                    : deptId.trim();
                            taskService.addCandidateGroup(firstTask.getId(), deptIdStr);
                        }
                    }
                }
            } else if ("INITIATOR".equals(dataType)) {
                // 发起人 - 已经在流程变量中设置，不需要额外处理
                String assignee = (String) approvalInfo.get("assignee");
                if (StringUtils.isNotBlank(assignee) && assignee.contains("initiator")) {
                    // 从流程变量中获取发起人ID
                    String initiatorId = (String) processInstance.getProcessVariables()
                            .get(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR);
                    if (StringUtils.isNotBlank(initiatorId)) {
                        taskService.setAssignee(firstTask.getId(), initiatorId);
                    }
                }
            }
        } catch (Exception e) {
            // 如果设置审批人失败，记录日志但不影响流程启动
            e.printStackTrace();
        }
    }


    /**
     * 启动项目发布实例
     * @param projectId
     * @param procDef
     * @param url
     * @param variables
     */
    private void startProjectProcess(String projectId, ProcessDefinition procDef, String url, Map<String, Object> variables) {

        WfTaskProcess wfTaskProcess = getWfTaskProcess(projectId, ProcessUtils.PROJECT_APPROVAL_TYPE);
        ProcessInstance processInstance = startCommonProcess(procDef, ProcessUtils.PROJECT_APPROVAL_TYPE, url, variables);
        updateWfTaskProcess(wfTaskProcess, processInstance, url);
    }


    /**
     * 启动采购入库审批实例
     * @param inboundId
     * @param procDef
     * @param url
     * @param variables
     */
    private void startInboundProcess(String inboundId, ProcessDefinition procDef, String url, Map<String, Object> variables) {
        WfTaskProcess wfTaskProcess = getWfTaskProcess(inboundId, ProcessUtils.PURCHASE_INTO_APPROVAL_TYPE);
        ProcessInstance processInstance = startCommonProcess(procDef, ProcessUtils.PURCHASE_INTO_APPROVAL_TYPE, url, variables);
        updateWfTaskProcess(wfTaskProcess, processInstance, url);
    }

    /**
     * 启动采购退货出库实例
     * @param outboundId
     * @param procDef
     * @param url
     * @param variables
     */
    private void startOutboundProcess(String outboundId, ProcessDefinition procDef, String url, Map<String, Object> variables) {
        WfTaskProcess wfTaskProcess = getWfTaskProcess(outboundId, ProcessUtils.PURCHASE_OUT_APPROVAL_TYPE);
        ProcessInstance processInstance = startCommonProcess(procDef, ProcessUtils.PURCHASE_OUT_APPROVAL_TYPE, url, variables);
        updateWfTaskProcess(wfTaskProcess, processInstance, url);
    }

    private void startProviderProcess(String providerId, ProcessDefinition procDef, String url, Map<String, Object> variables) {
        WfTaskProcess wfTaskProcess = getWfTaskProcess(providerId, "SUPPLIER_APPROVAL");
        ProcessInstance processInstance = startCommonProcess(procDef, "SUPPLIER_APPROVAL", url, variables);
        updateWfTaskProcess(wfTaskProcess, processInstance, url);
    }


    /**
     * 启动其他入库实例
     * @param otherIntoId
     * @param procDef
     * @param url
     * @param variables
     */
    private void startOtherIntoProcess(String otherIntoId, ProcessDefinition procDef, String url, Map<String, Object> variables) {
        WfTaskProcess wfTaskProcess = getWfTaskProcess(otherIntoId, ProcessUtils.OTHER_INTO_APPROVAL_TYPE);
        ProcessInstance processInstance = startCommonProcess(procDef, ProcessUtils.OTHER_INTO_APPROVAL_TYPE, url, variables);
        updateWfTaskProcess(wfTaskProcess, processInstance, url);
    }

    /**
     * 启动其他出库实例
     * @param otherOutId
     * @param procDef
     * @param url
     * @param variables
     */
    private void startOtherOutProcess(String otherOutId, ProcessDefinition procDef, String url, Map<String, Object> variables) {
        WfTaskProcess wfTaskProcess = getWfTaskProcess(otherOutId, ProcessUtils.OTHER_OUT_APPROVAL_TYPE);
        ProcessInstance processInstance = startCommonProcess(procDef, ProcessUtils.OTHER_OUT_APPROVAL_TYPE, url, variables);
        updateWfTaskProcess(wfTaskProcess, processInstance, url);
    }

    /**
     * 启动归还入库实例
     * @param returnIntoId
     * @param procDef
     * @param url
     * @param variables
     */
    private void startReturnIntoProcess(String returnIntoId, ProcessDefinition procDef, String url, Map<String, Object> variables) {
        WfTaskProcess wfTaskProcess = getWfTaskProcess(returnIntoId, ProcessUtils.RETURN_INTO_APPROVAL_TYPE);
        ProcessInstance processInstance = startCommonProcess(procDef, ProcessUtils.RETURN_INTO_APPROVAL_TYPE, url, variables);
        updateWfTaskProcess(wfTaskProcess, processInstance, url);
    }
    /**
     * 启动报废出库实例
     * @param ids
     * @param procDef
     * @param url
     * @param variables
     */
    private void startScrappedProcess(List<String> ids, ProcessDefinition procDef, String url, Map<String, Object> variables) {
        // ProcessUtils.SCRAPPED_OUT_APPROVAL_TYPE
        List<WfMaterialsScrappedProcess> scrappedProcess = getScrappedProcess(ids);
        ProcessInstance processInstance = startCommonProcess(procDef, ProcessUtils.SCRAPPED_OUT_APPROVAL_TYPE, url, variables);
        updateScrappedProcess(scrappedProcess, processInstance, url);
    }
    /**
     * 获取流程变量
     *
     * @param taskId 任务ID
     * @return 流程变量
     */
    private Map<String, Object> getProcessVariables(String taskId) {
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
            .includeProcessVariables()
            .finished()
            .taskId(taskId)
            .singleResult();
        if (Objects.nonNull(historicTaskInstance)) {
            return historicTaskInstance.getProcessVariables();
        }
        return taskService.getVariables(taskId);
    }

    /**
     * 获取当前任务流程表单信息
     */
    private FormConf currTaskFormData(String deployId, HistoricTaskInstance taskIns) {
        WfDeployFormVo deployFormVo = deployFormMapper.selectVoOne(new LambdaQueryWrapper<WfDeployForm>()
            .eq(WfDeployForm::getDeployId, deployId)
            .eq(WfDeployForm::getFormKey, taskIns.getFormKey())
            .eq(WfDeployForm::getNodeKey, taskIns.getTaskDefinitionKey()));
        if (ObjectUtil.isNotEmpty(deployFormVo)) {
            FormConf currTaskFormData = JsonUtils.parseObject(deployFormVo.getContent(), FormConf.class);
            if (null != currTaskFormData) {
                currTaskFormData.setFormBtns(false);
                ProcessFormUtils.fillFormData(currTaskFormData, taskIns.getTaskLocalVariables());
                return currTaskFormData;
            }
        }
        return null;
    }

    /**
     * 获取历史任务信息列表
     */
    private List<WfProcNodeVo> historyProcNodeList(String procInsId) {
        List<HistoricActivityInstance> historicActivityInstanceList =  historyService.createHistoricActivityInstanceQuery()
            .processInstanceId(procInsId)
            .activityTypes(CollUtil.newHashSet(BpmnXMLConstants.ELEMENT_EVENT_START, BpmnXMLConstants.ELEMENT_EVENT_END, BpmnXMLConstants.ELEMENT_TASK_USER))
            .orderByHistoricActivityInstanceStartTime().desc()
            .orderByHistoricActivityInstanceEndTime().desc()
            .list();

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(procInsId)
            .singleResult();

        List<Comment> commentList = taskService.getProcessInstanceComments(procInsId);

        List<WfProcNodeVo> elementVoList = new ArrayList<>();
        for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
            WfProcNodeVo elementVo = new WfProcNodeVo();
            elementVo.setProcDefId(activityInstance.getProcessDefinitionId());
            elementVo.setActivityId(activityInstance.getActivityId());
            elementVo.setActivityName(activityInstance.getActivityName());
            elementVo.setActivityType(activityInstance.getActivityType());
            elementVo.setCreateTime(activityInstance.getStartTime());
            elementVo.setEndTime(activityInstance.getEndTime());
            if (ObjectUtil.isNotNull(activityInstance.getDurationInMillis())) {
                elementVo.setDuration(DateUtil.formatBetween(activityInstance.getDurationInMillis(), BetweenFormatter.Level.SECOND));
            }

            if (BpmnXMLConstants.ELEMENT_EVENT_START.equals(activityInstance.getActivityType())) {
                if (ObjectUtil.isNotNull(historicProcessInstance)) {
                    Long userId = Long.parseLong(historicProcessInstance.getStartUserId());
                    SysUser user = wfCopyMapper.selectUserById(userId);
                    if (user != null) {
                        elementVo.setAssigneeId(user.getUserId());
                        elementVo.setAssigneeName(user.getNickName());
                    }
                }
            } else if (BpmnXMLConstants.ELEMENT_TASK_USER.equals(activityInstance.getActivityType())) {
                if (StringUtils.isNotBlank(activityInstance.getAssignee())) {
                    SysUser user = wfCopyMapper.selectUserById(Long.parseLong(activityInstance.getAssignee()));
                    elementVo.setAssigneeId(user.getUserId());
                    elementVo.setAssigneeName(user.getNickName());
                }
                // 展示审批人员
                List<HistoricIdentityLink> linksForTask = historyService.getHistoricIdentityLinksForTask(activityInstance.getTaskId());
                StringBuilder stringBuilder = new StringBuilder();
                for (HistoricIdentityLink identityLink : linksForTask) {
                    if ("candidate".equals(identityLink.getType())) {
                        if (StringUtils.isNotBlank(identityLink.getUserId())) {
                            SysUser user = wfCopyMapper.selectUserById(Long.parseLong(identityLink.getUserId()));
                            stringBuilder.append(user.getNickName()).append(",");
                        }
                        if (StringUtils.isNotBlank(identityLink.getGroupId())) {
                            if (identityLink.getGroupId().startsWith(TaskConstants.ROLE_GROUP_PREFIX)) {
                                Long roleId = Long.parseLong(StringUtils.stripStart(identityLink.getGroupId(), TaskConstants.ROLE_GROUP_PREFIX));
                                SysRole role = wfCopyMapper.selectRoleById(roleId);
                                stringBuilder.append(role.getRoleName()).append(",");
                            } else if (identityLink.getGroupId().startsWith(TaskConstants.DEPT_GROUP_PREFIX)) {
                                Long deptId = Long.parseLong(StringUtils.stripStart(identityLink.getGroupId(), TaskConstants.DEPT_GROUP_PREFIX));
                                SysDept dept = wfCopyMapper.selectDeptById(deptId);
                                stringBuilder.append(dept.getDeptName()).append(",");
                            }
                        }
                    }
                }
                if (StringUtils.isNotBlank(stringBuilder)) {
                    elementVo.setCandidate(stringBuilder.substring(0, stringBuilder.length() - 1));
                }
                // 获取意见评论内容
                if (CollUtil.isNotEmpty(commentList)) {
                    List<CommentVO> comments = new ArrayList<>();
                    for (Comment comment : commentList) {
                        if (comment.getTaskId().equals(activityInstance.getTaskId())) {
                            CommentVO commentVO = new CommentVO();
                            BeanUtils.copyProperties(comment, commentVO);
                            comments.add(commentVO);
                        }
                    }
                    elementVo.setCommentList(comments);
                }
            }
            elementVoList.add(elementVo);
        }
        return elementVoList;
    }

    /**
     * 从审批任务表构建简化审批流程的流转记录
     * 
     * @param extraId 业务ID
     * @param type 审批类型
     * @return 流转记录列表
     */
    private List<WfProcNodeVo> historyProcNodeListForSimplifiedApproval(String extraId, String type) {
        List<WfApprovalTask> approvalTaskList = wfApprovalTaskMapper.selectByExtraIdAndType(extraId, type);
        if (CollUtil.isEmpty(approvalTaskList)) {
            return new ArrayList<>();
        }
        
        // 按创建时间排序
        approvalTaskList.sort((a, b) -> {
            if (a.getCreatedTime() == null && b.getCreatedTime() == null) {
                return 0;
            }
            if (a.getCreatedTime() == null) {
                return 1;
            }
            if (b.getCreatedTime() == null) {
                return -1;
            }
            return a.getCreatedTime().compareTo(b.getCreatedTime());
        });
        
        List<WfProcNodeVo> elementVoList = new ArrayList<>();
        
        // 1. 发起流程节点（startEvent）
        WfApprovalTask firstTask = approvalTaskList.get(0);
        WfProcNodeVo startNode = new WfProcNodeVo();
        startNode.setProcDefId("simplified-approval-" + type);
        startNode.setActivityId("startEvent");
        startNode.setActivityName("发起流程");
        startNode.setActivityType(BpmnXMLConstants.ELEMENT_EVENT_START);
        startNode.setCreateTime(firstTask.getCreatedTime());
        startNode.setEndTime(firstTask.getCreatedTime());
        
        // 设置发起人信息
        if (StringUtils.isNotBlank(firstTask.getInitiatorId())) {
            try {
                SysUser user = wfCopyMapper.selectUserById(Long.parseLong(firstTask.getInitiatorId()));
                if (user != null) {
                    startNode.setAssigneeId(user.getUserId());
                    startNode.setAssigneeName(user.getNickName());
                } else {
                    startNode.setAssigneeName(firstTask.getInitiatorName());
                }
            } catch (NumberFormatException e) {
                startNode.setAssigneeName(firstTask.getInitiatorName());
            }
        }
        elementVoList.add(startNode);
        
        // 2. 审批任务节点（userTask）- 每个审批任务一个节点
        for (WfApprovalTask task : approvalTaskList) {
            WfProcNodeVo taskNode = new WfProcNodeVo();
            taskNode.setProcDefId("simplified-approval-" + type);
            taskNode.setActivityId("userTask_" + task.getId());
            taskNode.setActivityName("审批任务");
            taskNode.setActivityType(BpmnXMLConstants.ELEMENT_TASK_USER);
            taskNode.setCreateTime(task.getCreatedTime());
            taskNode.setEndTime(task.getApprovalTime());
            
            // 计算耗时
            if (task.getCreatedTime() != null && task.getApprovalTime() != null) {
                long duration = task.getApprovalTime().getTime() - task.getCreatedTime().getTime();
                taskNode.setDuration(DateUtil.formatBetween(duration, BetweenFormatter.Level.SECOND));
            }
            
            // 设置审批人信息
            if (StringUtils.isNotBlank(task.getApproverId())) {
                try {
                    if ("user".equals(task.getApproverType()) || StringUtils.isBlank(task.getApproverType())) {
                        SysUser user = wfCopyMapper.selectUserById(Long.parseLong(task.getApproverId()));
                        if (user != null) {
                            taskNode.setAssigneeId(user.getUserId());
                            taskNode.setAssigneeName(user.getNickName());
                        } else {
                            taskNode.setAssigneeName(task.getApproverName());
                        }
                    } else {
                        taskNode.setAssigneeName(task.getApproverName());
                    }
                } catch (NumberFormatException e) {
                    taskNode.setAssigneeName(task.getApproverName());
                }
            }
            
            // 设置候选审批人（如果有多个审批人）
            if (StringUtils.isNotBlank(task.getApproverValue())) {
                StringBuilder candidateBuilder = new StringBuilder();
                String[] approverValues = task.getApproverValue().split(",");
                for (String approverValue : approverValues) {
                    if (StringUtils.isBlank(approverValue)) {
                        continue;
                    }
                    try {
                        if ("user".equals(task.getApproverType()) || StringUtils.isBlank(task.getApproverType())) {
                            SysUser user = wfCopyMapper.selectUserById(Long.parseLong(approverValue.trim()));
                            if (user != null) {
                                candidateBuilder.append(user.getNickName()).append(",");
                            }
                        } else if ("role".equals(task.getApproverType())) {
                            SysRole role = wfCopyMapper.selectRoleById(Long.parseLong(approverValue.trim()));
                            if (role != null) {
                                candidateBuilder.append(role.getRoleName()).append(",");
                            }
                        } else if ("dept".equals(task.getApproverType())) {
                            SysDept dept = wfCopyMapper.selectDeptById(Long.parseLong(approverValue.trim()));
                            if (dept != null) {
                                candidateBuilder.append(dept.getDeptName()).append(",");
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析审批人信息失败: {}", approverValue, e);
                    }
                }
                if (candidateBuilder.length() > 0) {
                    taskNode.setCandidate(candidateBuilder.substring(0, candidateBuilder.length() - 1));
                }
            }
            
            // 设置审批意见
            if (StringUtils.isNotBlank(task.getApprovalComment())) {
                List<CommentVO> comments = new ArrayList<>();
                CommentVO commentVO = new CommentVO();
                commentVO.setId(task.getId());
                commentVO.setTaskId(task.getId());
                commentVO.setProcessInstanceId(extraId);
                commentVO.setType("comment");
                commentVO.setFullMessage(task.getApprovalComment());
                commentVO.setTime(task.getApprovalTime() != null ? task.getApprovalTime() : task.getCreatedTime());
                if (StringUtils.isNotBlank(task.getApproverId())) {
                    commentVO.setUserId(task.getApproverId());
                }
                comments.add(commentVO);
                taskNode.setCommentList(comments);
            }
            
            elementVoList.add(taskNode);
        }
        
        // 3. 结束流程节点（endEvent）
        WfApprovalTask lastTask = approvalTaskList.get(approvalTaskList.size() - 1);
        WfProcNodeVo endNode = new WfProcNodeVo();
        endNode.setProcDefId("simplified-approval-" + type);
        endNode.setActivityId("endEvent");
        endNode.setActivityName("结束流程");
        endNode.setActivityType(BpmnXMLConstants.ELEMENT_EVENT_END);
        
        // 判断流程是否已结束（所有审批任务都已处理）
        boolean allFinished = approvalTaskList.stream()
            .allMatch(task -> !"pending".equals(task.getStatus()));
        
        if (allFinished) {
            // 找到最后一个审批时间作为结束时间
            Date endTime = approvalTaskList.stream()
                .filter(task -> task.getApprovalTime() != null)
                .map(WfApprovalTask::getApprovalTime)
                .max(Date::compareTo)
                .orElse(lastTask.getCreatedTime());
            endNode.setCreateTime(endTime);
            endNode.setEndTime(endTime);
        } else {
            endNode.setCreateTime(null);
            endNode.setEndTime(null);
        }
        
        elementVoList.add(endNode);
        
        return elementVoList;
    }

    /**
     * 完成简化审批任务
     * 
     * @param dto 审批完成DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeSimplifiedApproval(com.laigeoffer.pmhub.workflow.domain.dto.ApprovalCompleteDTO dto) {
        // 1. 查询审批任务
        WfApprovalTask task = wfApprovalTaskMapper.selectById(dto.getApprovalTaskId());
        if (task == null || !"pending".equals(task.getStatus())) {
            throw new ServiceException("审批任务不存在或已处理");
        }
        
        // 2. 获取当前审批人ID
        Long currentUserId = SecurityUtils.getUserId();
        String approverId = String.valueOf(currentUserId);
        
        // 3. 更新审批任务状态
        String status = dto.getApproved() ? "approved" : "rejected";
        wfApprovalTaskMapper.updateStatus(task.getId(), status, dto.getComment(), approverId);
        
        // 3.5. 处理抄送人
        if (StringUtils.isNotBlank(dto.getCopyUserIds())) {
            createSimplifiedApprovalCopy(task, dto.getCopyUserIds(), currentUserId);
        }
        
        // 4. 如果是拒绝，取消其他待审批任务
        if ("rejected".equals(status)) {
            wfApprovalTaskMapper.cancelPendingTasks(task.getExtraId(), task.getType());
            // 更新业务对象状态（任务状态改为未开始）
            if (ProcessUtils.TASK_APPROVAL_TYPE.equals(task.getType())) {
                R<Void> resetResult = projectTaskProcessFeignService.resetTaskStatus(task.getExtraId(), SecurityConstants.INNER);
                if (resetResult.getCode() != 200) {
                    log.warn("重置项目任务状态失败: {}", resetResult.getMsg());
                }
            }
            // 更新 pmhub_project_task_process 的 approved 字段为 "2"（已拒绝）
            updateWfTaskProcessApproved(task.getExtraId(), task.getType(), "2");
        } else {
            // 5. 如果是通过，检查是否所有审批人都已通过
            int pendingCount = wfApprovalTaskMapper.countPendingByExtraIdAndType(
                task.getExtraId(), task.getType());
            if (pendingCount == 0) {
                // 所有审批人都已通过，更新业务对象状态（任务状态改为已完成）
                if (ProcessUtils.TASK_APPROVAL_TYPE.equals(task.getType())) {
                    R<Void> updateResult = projectTaskProcessFeignService.updateTaskStatus(task.getExtraId(), SecurityConstants.INNER);
                    if (updateResult.getCode() != 200) {
                        log.warn("更新项目任务状态失败: {}", updateResult.getMsg());
                    }
                }
                // 更新 pmhub_project_task_process 的 approved 字段为 "1"（已通过）
                updateWfTaskProcessApproved(task.getExtraId(), task.getType(), "1");
            } else {
                // 还有待审批任务，只更新 approved 字段为 "0"（审批中）
                updateWfTaskProcessApproved(task.getExtraId(), task.getType(), "0");
            }
        }
    }
    
    /**
     * 创建简化审批的抄送记录
     * 
     * @param task 审批任务
     * @param copyUserIds 抄送人ID（多个用逗号分隔）
     * @param originatorId 发起人ID（当前审批人）
     */
    private void createSimplifiedApprovalCopy(WfApprovalTask task, String copyUserIds, Long originatorId) {
        if (StringUtils.isBlank(copyUserIds)) {
            return;
        }
        
        // 获取发起人信息
        SysUser originator = wfCopyMapper.selectUserById(originatorId);
        String originatorName = originator != null ? originator.getNickName() : "";
        
        // 构建抄送标题
        String title = task.getTitle() != null ? task.getTitle() : "简化审批流程";
        
        // 解析抄送人ID
        String[] userIds = copyUserIds.split(",");
        List<WfCopy> copyList = new ArrayList<>(userIds.length);
        
        // 获取当前用户和时间信息
        String currentUsername = SecurityUtils.getUsername();
        Date currentTime = new Date();
        
        for (String userIdStr : userIds) {
            if (StringUtils.isBlank(userIdStr)) {
                continue;
            }
            try {
                Long userId = Long.valueOf(userIdStr.trim());
                WfCopy copy = new WfCopy();
                copy.setTitle(title);
                // 简化审批的流程ID使用固定格式
                copy.setProcessId("simplified-approval-" + task.getType());
                copy.setProcessName("简化审批流程");
                // 简化审批没有部署ID，设置为空
                copy.setDeploymentId(null);
                // 使用 extraId 作为流程实例ID
                copy.setInstanceId(task.getExtraId());
                // 使用审批任务ID作为任务ID
                copy.setTaskId(task.getId());
                copy.setUserId(userId);
                copy.setOriginatorId(originatorId);
                copy.setOriginatorName(originatorName);
                // 设置创建时间和更新时间
                copy.setCreateTime(currentTime);
                copy.setUpdateTime(currentTime);
                copy.setCreateBy(currentUsername);
                copy.setUpdateBy(currentUsername);
                copyList.add(copy);
            } catch (NumberFormatException e) {
                log.warn("无效的抄送人ID: {}", userIdStr);
            }
        }
        
        // 批量插入抄送记录
        if (!copyList.isEmpty()) {
            wfCopyMapper.insertBatch(copyList);
        }
    }
    
    /**
     * 更新 pmhub_project_task_process 的 approved 字段
     * 
     * @param extraId 业务ID
     * @param type 审批类型
     * @param approved 审批状态："0"-审批中，"1"-已通过，"2"-已拒绝
     */
    private void updateWfTaskProcessApproved(String extraId, String type, String approved) {
        // 使用 Feign 调用更新 approved 字段
        R<Void> updateResult = projectTaskProcessFeignService.updateApproved(extraId, type, approved, SecurityConstants.INNER);
        if (updateResult.getCode() != 200) {
            log.warn("更新任务流程 approved 字段失败: {}", updateResult.getMsg());
        }
    }

    /**
     * 查询审批任务列表（用于显示审批进度）
     * 
     * @param extraId 业务ID（如任务ID）
     * @param type 审批类型（如task）
     * @return 审批任务列表
     */
    @Override
    public List<com.laigeoffer.pmhub.workflow.domain.WfApprovalTask> getApprovalTaskList(String extraId, String type) {
        List<WfApprovalTask> taskList = wfApprovalTaskMapper.selectByExtraIdAndType(extraId, type);
        // 补充审批人姓名（处理旧数据或数据不完整的情况）
        if (CollUtil.isNotEmpty(taskList)) {
            for (WfApprovalTask task : taskList) {
                // 如果审批人姓名为空，但审批人ID不为空，则查询用户信息补充
                if (StringUtils.isBlank(task.getApproverName()) && StringUtils.isNotBlank(task.getApproverId())) {
                    try {
                        // 只有审批人类型为user时才查询用户信息
                        if ("user".equals(task.getApproverType()) || StringUtils.isBlank(task.getApproverType())) {
                            SysUser user = wfCopyMapper.selectUserById(Long.parseLong(task.getApproverId()));
                            if (user != null) {
                                task.setApproverName(user.getNickName());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("查询审批人信息失败，approverId: {}", task.getApproverId(), e);
                    }
                }
            }
        }
        return taskList;
    }
}
