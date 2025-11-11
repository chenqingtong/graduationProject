package com.laigeoffer.pmhub.workflow.service.impl;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laigeoffer.pmhub.base.core.core.domain.PageQuery;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.page.Table2DataInfo;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.enums.ProjectStatusEnum;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.base.core.utils.StringUtils;
import com.laigeoffer.pmhub.base.core.utils.JsonUtils;
import com.laigeoffer.pmhub.api.project.ProjectTaskProcessFeignService;
import com.laigeoffer.pmhub.workflow.core.domain.ProcessQuery;
import com.laigeoffer.pmhub.workflow.domain.WfApprovalSet;
import com.laigeoffer.pmhub.workflow.domain.WfDeployForm;
import com.laigeoffer.pmhub.workflow.domain.WfMaterialsScrappedProcess;
import com.laigeoffer.pmhub.base.core.core.domain.entity.WfTaskProcess;
import com.laigeoffer.pmhub.base.core.core.domain.dto.ApprovalSetDTO;
import com.laigeoffer.pmhub.workflow.domain.dto.MaterialsApprovalSetDTO;
import com.laigeoffer.pmhub.workflow.domain.vo.MaterialsApprovalSetVO;
import com.laigeoffer.pmhub.workflow.domain.vo.WfDeployVo;
import com.laigeoffer.pmhub.workflow.factory.FlowServiceFactory;
import com.laigeoffer.pmhub.workflow.mapper.WfApprovalSetMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfDeployFormMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfMaterialsScrappedProcessMapper;
import com.laigeoffer.pmhub.workflow.service.IWfDeployService;
import com.laigeoffer.pmhub.workflow.utils.ProcessUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.task.Comment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author canghe
 * @createTime 2022/6/30 9:04
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class WfDeployServiceImpl extends FlowServiceFactory implements IWfDeployService {

    private final RepositoryService repositoryService;
    private final WfDeployFormMapper deployFormMapper;
    private final WfApprovalSetMapper wfApprovalSetMapper;
    private final WfMaterialsScrappedProcessMapper wfMaterialsScrappedProcessMapper;
    private final ProjectTaskProcessFeignService projectTaskProcessFeignService;

    @Override
    public Table2DataInfo<WfDeployVo> queryPageList(ProcessQuery processQuery, PageQuery pageQuery) {
        // 流程定义列表数据查询
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
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

        List<WfDeployVo> deployVoList = new ArrayList<>(definitionList.size());
        for (ProcessDefinition processDefinition : definitionList) {
            String deploymentId = processDefinition.getDeploymentId();
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
            WfDeployVo vo = new WfDeployVo();
            vo.setDefinitionId(processDefinition.getId());
            vo.setProcessKey(processDefinition.getKey());
            vo.setProcessName(processDefinition.getName());
            vo.setVersion(processDefinition.getVersion());
            vo.setCategory(processDefinition.getCategory());
            vo.setDeploymentId(processDefinition.getDeploymentId());
            vo.setSuspended(processDefinition.isSuspended());
            // 流程部署信息
            vo.setCategory(deployment.getCategory());
            vo.setDeploymentTime(deployment.getDeploymentTime());
            deployVoList.add(vo);
        }
        deployVoList.sort(Comparator.comparing(WfDeployVo::getDeploymentTime).reversed());
        Page<WfDeployVo> page = new Page<>();
        page.setRecords(deployVoList);
        page.setTotal(pageTotal);
        return Table2DataInfo.build(page);
    }

    @Override
    public Table2DataInfo<WfDeployVo> queryPublishList(String processKey, PageQuery pageQuery) {
        // 创建查询条件
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .orderByProcessDefinitionVersion()
                .desc();
        long pageTotal = processDefinitionQuery.count();
        if (pageTotal <= 0) {
            return Table2DataInfo.build();
        }
        // 根据查询条件，查询所有版本
        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        List<ProcessDefinition> processDefinitionList = processDefinitionQuery
                .listPage(offset, pageQuery.getPageSize());
        List<WfDeployVo> deployVoList = processDefinitionList.stream().map(item -> {
            WfDeployVo vo = new WfDeployVo();
            vo.setDefinitionId(item.getId());
            vo.setProcessKey(item.getKey());
            vo.setProcessName(item.getName());
            vo.setVersion(item.getVersion());
            vo.setCategory(item.getCategory());
            vo.setDeploymentId(item.getDeploymentId());
            vo.setSuspended(item.isSuspended());
            return vo;
        }).collect(Collectors.toList());
        Page<WfDeployVo> page = new Page<>();
        page.setRecords(deployVoList);
        page.setTotal(pageTotal);
        return Table2DataInfo.build(page);
    }

    /**
     * 激活或挂起流程
     *
     * @param state        状态
     * @param definitionId 流程定义ID
     */
    @Override
    public void updateState(String definitionId, String state) {
        if (SuspensionState.ACTIVE.toString().equals(state)) {
            // 激活
            repositoryService.activateProcessDefinitionById(definitionId, true, null);
        } else if (SuspensionState.SUSPENDED.toString().equals(state)) {
            // 挂起
            repositoryService.suspendProcessDefinitionById(definitionId, true, null);
        }
    }

    @Override
    public String queryBpmnXmlById(String definitionId) {
        InputStream inputStream = repositoryService.getProcessModel(definitionId);
        try {
            return IoUtil.readUtf8(inputStream);
        } catch (IORuntimeException exception) {
            throw new RuntimeException("加载xml文件异常");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<String> deployIds) {
        for (String deployId : deployIds) {
            repositoryService.deleteDeployment(deployId, true);
            deployFormMapper.delete(new LambdaQueryWrapper<WfDeployForm>().eq(WfDeployForm::getDeployId, deployId));
        }
    }

    /**
     * 新增或更新审批设置
     *
     * @param approvalSetDTO
     * @param type
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvalSet(MaterialsApprovalSetDTO approvalSetDTO, String type) {
        WfApprovalSet mas = getMaterialsApprovalSet(type, null);
        if (mas != null) {
            // 更新
            mas.setApproved(approvalSetDTO.getApproved());
            mas.setDefinitionId(approvalSetDTO.getDefinitionId());
            mas.setDeploymentId(approvalSetDTO.getDeploymentId());
            mas.setUpdatedBy(SecurityUtils.getUsername());
            mas.setUpdatedTime(new Date());
            wfApprovalSetMapper.updateById(mas);
        } else {
            // 新增
            WfApprovalSet wfApprovalSet = new WfApprovalSet();
            wfApprovalSet.setApproved(approvalSetDTO.getApproved());
            wfApprovalSet.setType(type);
            wfApprovalSet.setDefinitionId(approvalSetDTO.getDefinitionId());
            wfApprovalSet.setDeploymentId(approvalSetDTO.getDeploymentId());
            wfApprovalSet.setCreatedBy(SecurityUtils.getUsername());
            wfApprovalSet.setCreatedTime(new Date());
            wfApprovalSet.setUpdatedBy(SecurityUtils.getUsername());
            wfApprovalSet.setUpdatedTime(new Date());
            wfApprovalSetMapper.insert(wfApprovalSet);
        }
    }

    private WfApprovalSet getMaterialsApprovalSet(String type, String taskId) {
        LambdaQueryWrapper<WfApprovalSet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfApprovalSet::getType, type);
        if (StringUtils.isNotBlank(taskId)) {
            queryWrapper.eq(WfApprovalSet::getExtraId, taskId);
        } else {
            if (ProjectStatusEnum.TASK.getStatusName().equals(type)) {
                return null;
            }
        }
        return wfApprovalSetMapper.selectOne(queryWrapper);
    }

    /**
     * 根据类型查询审批设置
     *
     * @param type
     * @return
     */
    @Override
    public MaterialsApprovalSetVO queryApprovalSet(String type, String taskId) {
        log.info("查询审批设置，type: {}, taskId: {}", type, taskId);
        MaterialsApprovalSetVO materialsApprovalSetVO = new MaterialsApprovalSetVO();
        WfApprovalSet wfApprovalSet = getMaterialsApprovalSet(type, taskId);
        log.info("查询到审批设置记录: {}", wfApprovalSet != null ? "存在" : "不存在");
        if (wfApprovalSet != null) {
            materialsApprovalSetVO.setApproved(wfApprovalSet.getApproved());
            materialsApprovalSetVO.setType(type);
            materialsApprovalSetVO.setDeploymentId(wfApprovalSet.getDeploymentId());
            materialsApprovalSetVO.setDefinitionId(wfApprovalSet.getDefinitionId());
            // 返回审批人信息
            materialsApprovalSetVO.setApprovalInfo(wfApprovalSet.getApprovalInfo());
            log.info("返回审批设置数据，approved: {}, approvalInfo: {}", 
                    materialsApprovalSetVO.getApproved(), materialsApprovalSetVO.getApprovalInfo());
        } else {
            log.warn("未查询到审批设置记录，type: {}, taskId: {}", type, taskId);
        }
        return materialsApprovalSetVO;
    }

    @Override
    public boolean updateApprovalSet(ApprovalSetDTO approvalSetDTO, String type) {
        String extraId = null;
        if (ProjectStatusEnum.TASK.getStatusName().equals(type)) {
            extraId = approvalSetDTO.getTaskId();
        }
        if (ProjectStatusEnum.PROJECT.getStatusName().equals(type)) {
            extraId = approvalSetDTO.getProjectId();
        }
        
        // 使用 Feign 调用 pmhub-project 服务查询任务流程
        R<WfTaskProcess> queryResult = projectTaskProcessFeignService.getByExtraIdAndType(
                extraId, 
                type, 
                SecurityConstants.INNER);
        WfTaskProcess pt = queryResult != null ? queryResult.getData() : null;
        
        if (pt != null) {
            if ("1".equals(pt.getApproved())) {
                R<Integer> statusResult = projectTaskProcessFeignService.getTaskStatus(approvalSetDTO.getTaskId(), SecurityConstants.INNER);
                Integer taskStatus = statusResult != null ? statusResult.getData() : null;
                if (!Objects.equals(taskStatus, ProjectStatusEnum.NO_STARTED.getStatus())) {
                    throw new ServiceException("需将任务状态变为未开始才能修改审批设置");
                }
            }
            // 更新
            if (StringUtils.isBlank(pt.getInstanceId())) {
                pt.setApproved(approvalSetDTO.getApproved());
                pt.setDefinitionId(approvalSetDTO.getDefinitionId());
                pt.setDeploymentId(approvalSetDTO.getDeploymentId());
                pt.setUpdatedBy(SecurityUtils.getUsername());
                pt.setUpdatedTime(new Date());
                // 使用 Feign 调用更新任务流程
                R<WfTaskProcess> updateResult = projectTaskProcessFeignService.insertOrUpdate(pt, SecurityConstants.INNER);
                if (updateResult == null || updateResult.getCode() != 200) {
                    throw new ServiceException("更新任务流程失败");
                }
            } else {
                throw new ServiceException("审批中或者已完成的流程不允许修改审批设置");
            }
        } else {
            // 新增
            WfTaskProcess wfTaskProcess = new WfTaskProcess();
            wfTaskProcess.setExtraId(extraId);
            wfTaskProcess.setApproved(approvalSetDTO.getApproved());
            wfTaskProcess.setType(type);
            extracted(approvalSetDTO.getDefinitionId(), approvalSetDTO.getDeploymentId(), wfTaskProcess);
            // 使用 Feign 调用插入任务流程
            R<WfTaskProcess> insertResult = projectTaskProcessFeignService.insertOrUpdate(wfTaskProcess, SecurityConstants.INNER);
            if (insertResult == null || insertResult.getCode() != 200) {
                throw new ServiceException("插入任务流程失败");
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateApprovalSet2(ApprovalSetDTO approvalSetDTO, String type) {
        log.info("开始更新审批设置，taskId: {}, approved: {}, approvalInfo: {}, type: {}", 
                approvalSetDTO.getTaskId(), approvalSetDTO.getApproved(), approvalSetDTO.getApprovalInfo(), type);
        LambdaQueryWrapper<WfApprovalSet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfApprovalSet::getExtraId, approvalSetDTO.getTaskId()).eq(WfApprovalSet::getType, ProjectStatusEnum.TASK.getStatusName());
        WfApprovalSet wfApprovalSet = wfApprovalSetMapper.selectOne(queryWrapper);
        log.info("查询到审批设置记录: {}", wfApprovalSet != null ? "存在" : "不存在");
        
        // 如果审批设置不存在，创建新的记录
        if (wfApprovalSet == null) {
            wfApprovalSet = new WfApprovalSet();
            wfApprovalSet.setExtraId(approvalSetDTO.getTaskId());
            wfApprovalSet.setType(ProjectStatusEnum.TASK.getStatusName());
            wfApprovalSet.setCreatedBy(SecurityUtils.getUsername());
            wfApprovalSet.setCreatedTime(new Date());
        } else {
            // 无需审批
            if ("1".equals(wfApprovalSet.getApproved())) {
                R<Integer> statusResult = projectTaskProcessFeignService.getTaskStatus(approvalSetDTO.getTaskId(), SecurityConstants.INNER);
                Integer taskStatus = statusResult != null ? statusResult.getData() : null;
                if (!Objects.equals(taskStatus, ProjectStatusEnum.NO_STARTED.getStatus())) {
                    throw new ServiceException("需将任务状态变为未开始才能修改审批设置");
                }
            } else {
                // 需要审批
                // 使用 Feign 调用 pmhub-project 服务查询任务流程
                R<WfTaskProcess> queryResult = projectTaskProcessFeignService.getByExtraIdAndType(
                        approvalSetDTO.getTaskId(), 
                        ProjectStatusEnum.TASK.getStatusName(), 
                        SecurityConstants.INNER);
                WfTaskProcess pt = queryResult != null ? queryResult.getData() : null;
                if (pt != null && StringUtils.isNotBlank(pt.getInstanceId())) {
                    // 根据 instanceId 查询流程的状态是不是已拒绝
                    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(pt.getInstanceId()).singleResult();
                    if (historicProcessInstance != null) {
                        if (StringUtils.isBlank(historicProcessInstance.getEndActivityId())) {
                            // 审批中的流程不允许修改审批设置
                            throw new ServiceException("审批中的流程不允许修改审批设置");
                        } else {
                            List<Comment> list = taskService.getProcessInstanceComments(pt.getInstanceId());
                            list.sort(Comparator.comparing(Comment::getTime).reversed());
                            // 已通过的流程不允许修改审批设置
                            if ("1".equals(list.get(0).getType())) {
                                throw new ServiceException("已通过的流程不允许修改审批设置");
                            }
                        }
                    }
                }
            }
        }
        
        // 更新审批设置信息
        wfApprovalSet.setApproved(approvalSetDTO.getApproved());
        wfApprovalSet.setDeploymentId(approvalSetDTO.getDeploymentId());
        wfApprovalSet.setDefinitionId(approvalSetDTO.getDefinitionId());
        // 保存审批人信息：如果需要审批，则保存审批人信息；如果无需审批，则清空审批人信息
        if ("0".equals(approvalSetDTO.getApproved())) {
            // 需要审批：验证并保存审批人信息
            if (StringUtils.isNotBlank(approvalSetDTO.getApprovalInfo())) {
                // 验证审批人信息是否有效
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> approvalInfoMap = JsonUtils.parseObject(approvalSetDTO.getApprovalInfo(), java.util.Map.class);
                    if (approvalInfoMap != null) {
                        // 检查是否有有效的审批人信息
                        String assignee = (String) approvalInfoMap.get("assignee");
                        String candidateUsers = (String) approvalInfoMap.get("candidateUsers");
                        String candidateGroups = (String) approvalInfoMap.get("candidateGroups");
                        String dataType = (String) approvalInfoMap.get("dataType");
                        
                        // 验证是否有有效的审批人信息
                        boolean hasValidApprover = StringUtils.isNotBlank(assignee) ||
                                                  StringUtils.isNotBlank(candidateUsers) ||
                                                  StringUtils.isNotBlank(candidateGroups) ||
                                                  "INITIATOR".equals(dataType);
                        
                        if (hasValidApprover) {
                            wfApprovalSet.setApprovalInfo(approvalSetDTO.getApprovalInfo());
                        } else {
                            // 审批人信息无效，清空（避免保存无效数据）
                            wfApprovalSet.setApprovalInfo(null);
                        }
                    } else {
                        // JSON解析失败，清空
                        wfApprovalSet.setApprovalInfo(null);
                    }
                } catch (Exception e) {
                    // JSON解析异常，清空
                    log.warn("审批人信息JSON解析失败: {}", e.getMessage());
                    wfApprovalSet.setApprovalInfo(null);
                }
            } else {
                // 如果需要审批但没有提供审批人信息，清空（避免使用旧值）
                wfApprovalSet.setApprovalInfo(null);
            }
        } else {
            // 无需审批：清空审批人信息
            wfApprovalSet.setApprovalInfo(null);
        }
        wfApprovalSet.setUpdatedBy(SecurityUtils.getUsername());
        wfApprovalSet.setUpdatedTime(new Date());
        
        // 根据是否存在来决定是插入还是更新
        if (wfApprovalSet.getId() == null) {
            wfApprovalSetMapper.insert(wfApprovalSet);
            log.info("插入审批设置成功，taskId: {}, approvalInfo: {}", approvalSetDTO.getTaskId(), wfApprovalSet.getApprovalInfo());
        } else {
            wfApprovalSetMapper.updateById(wfApprovalSet);
            log.info("更新审批设置成功，taskId: {}, approvalInfo: {}", approvalSetDTO.getTaskId(), wfApprovalSet.getApprovalInfo());
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertApprovalSet() {
        R<List<WfTaskProcess>> response = projectTaskProcessFeignService.listByType(ProjectStatusEnum.TASK.getStatusName(), SecurityConstants.INNER);
        List<WfTaskProcess> list = response != null && response.getCode() == 200 ? response.getData() : Collections.emptyList();
        if (CollectionUtils.isNotEmpty(list)) {
            log.info("开始优化的任务审批设置的数量:{}", list.size());
            list.forEach(wfTaskProcess -> {
                WfApprovalSet wfApprovalSet = new WfApprovalSet();
                wfApprovalSet.setType(ProjectStatusEnum.TASK.getStatusName());
                wfApprovalSet.setApproved(wfTaskProcess.getApproved());
                wfApprovalSet.setDefinitionId(wfTaskProcess.getDefinitionId());
                wfApprovalSet.setDeploymentId(wfTaskProcess.getDeploymentId());
                wfApprovalSet.setCreatedBy(wfTaskProcess.getCreatedBy());
                wfApprovalSet.setCreatedTime(wfTaskProcess.getCreatedTime());
                wfApprovalSet.setUpdatedBy(wfTaskProcess.getUpdatedBy());
                wfApprovalSet.setUpdatedTime(wfTaskProcess.getUpdatedTime());
                wfApprovalSet.setExtraId(wfTaskProcess.getExtraId());
                wfApprovalSetMapper.insert(wfApprovalSet);
                if ("1".equals(wfTaskProcess.getApproved())) {
                    R<Void> deleteResponse = projectTaskProcessFeignService.deleteById(wfTaskProcess.getId(), SecurityConstants.INNER);
                    if (deleteResponse == null || deleteResponse.getCode() != 200) {
                        log.warn("删除任务流程失败: {}, taskId: {}", deleteResponse != null ? deleteResponse.getMsg() : "response is null", wfTaskProcess.getTaskId());
                    }
                    log.info("开始删除的任务id:{}", wfTaskProcess.getTaskId());
                }
            });
            log.info("结束优化的任务审批设置");
        }
        return true;

    }

    @Override
    public WfTaskProcess insertWfTaskProcess(String extraId, String type, String approved, String definitionId, String deploymentId) {
        R<WfTaskProcess> response = projectTaskProcessFeignService.getByExtraIdAndType(extraId, type, SecurityConstants.INNER);
        WfTaskProcess wp = response != null ? response.getData() : null;
        if (wp != null) {
            wp.setApproved(approved);
            wp.setDefinitionId(definitionId);
            wp.setDeploymentId(deploymentId);
            wp.setUpdatedBy(SecurityUtils.getUsername());
            wp.setUpdatedTime(new Date());
            R<WfTaskProcess> updateResponse = projectTaskProcessFeignService.insertOrUpdate(wp, SecurityConstants.INNER);
            if (updateResponse == null || updateResponse.getCode() != 200) {
                throw new ServiceException("更新任务流程失败");
            }
            return updateResponse.getData();
        } else {
            WfTaskProcess wfTaskProcess = new WfTaskProcess();
            wfTaskProcess.setExtraId(extraId);
            wfTaskProcess.setType(type);
            wfTaskProcess.setApproved(approved);
            extracted(definitionId, deploymentId, wfTaskProcess);
            R<WfTaskProcess> insertResponse = projectTaskProcessFeignService.insertOrUpdate(wfTaskProcess, SecurityConstants.INNER);
            if (insertResponse == null || insertResponse.getCode() != 200) {
                throw new ServiceException("新增任务流程失败");
            }
            return insertResponse.getData();
        }
    }

    @Override
    public boolean insertOrUpdateApprovalSet(String extraId, String type, String approved, String definitionId, String deploymentId) {
        LambdaQueryWrapper<WfApprovalSet> qw = new LambdaQueryWrapper<>();
        qw.eq(WfApprovalSet::getExtraId, extraId).eq(WfApprovalSet::getType, type);
        WfApprovalSet mas = wfApprovalSetMapper.selectOne(qw);
        if (mas != null) {
            mas.setApproved(approved);
            mas.setDefinitionId(definitionId);
            mas.setDeploymentId(deploymentId);
            mas.setUpdatedBy(SecurityUtils.getUsername());
            mas.setUpdatedTime(new Date());
            wfApprovalSetMapper.updateById(mas);
        } else {
            WfApprovalSet wfApprovalSet = new WfApprovalSet();
            wfApprovalSet.setExtraId(extraId);
            wfApprovalSet.setType(type);
            wfApprovalSet.setApproved(approved);
            wfApprovalSet.setDefinitionId(definitionId);
            wfApprovalSet.setDeploymentId(deploymentId);
            wfApprovalSet.setCreatedBy(SecurityUtils.getUsername());
            wfApprovalSet.setCreatedTime(new Date());
            wfApprovalSet.setUpdatedBy(SecurityUtils.getUsername());
            wfApprovalSet.setUpdatedTime(new Date());
            wfApprovalSetMapper.insert(wfApprovalSet);
        }

        return true;
    }

    /**
     * 新增或更新审批设置（支持审批人信息）
     */
    @Override
    public boolean insertOrUpdateApprovalSet(ApprovalSetDTO approvalSetDTO) {
        LambdaQueryWrapper<WfApprovalSet> qw = new LambdaQueryWrapper<>();
        qw.eq(WfApprovalSet::getExtraId, approvalSetDTO.getExtraId()).eq(WfApprovalSet::getType, approvalSetDTO.getType());
        WfApprovalSet mas = wfApprovalSetMapper.selectOne(qw);
        if (mas != null) {
            mas.setApproved(approvalSetDTO.getApproved());
            mas.setDefinitionId(approvalSetDTO.getDefinitionId());
            mas.setDeploymentId(approvalSetDTO.getDeploymentId());
            // 保存审批人信息
            if (StringUtils.isNotBlank(approvalSetDTO.getApprovalInfo())) {
                mas.setApprovalInfo(approvalSetDTO.getApprovalInfo());
            }
            mas.setUpdatedBy(SecurityUtils.getUsername());
            mas.setUpdatedTime(new Date());
            wfApprovalSetMapper.updateById(mas);
        } else {
            WfApprovalSet wfApprovalSet = new WfApprovalSet();
            wfApprovalSet.setExtraId(approvalSetDTO.getExtraId());
            wfApprovalSet.setType(approvalSetDTO.getType());
            wfApprovalSet.setApproved(approvalSetDTO.getApproved());
            wfApprovalSet.setDefinitionId(approvalSetDTO.getDefinitionId());
            wfApprovalSet.setDeploymentId(approvalSetDTO.getDeploymentId());
            // 保存审批人信息
            if (StringUtils.isNotBlank(approvalSetDTO.getApprovalInfo())) {
                wfApprovalSet.setApprovalInfo(approvalSetDTO.getApprovalInfo());
            }
            wfApprovalSet.setCreatedBy(SecurityUtils.getUsername());
            wfApprovalSet.setCreatedTime(new Date());
            wfApprovalSet.setUpdatedBy(SecurityUtils.getUsername());
            wfApprovalSet.setUpdatedTime(new Date());
            wfApprovalSetMapper.insert(wfApprovalSet);
        }

        return true;
    }

    @Override
    public List<WfMaterialsScrappedProcess> insertScrappedProcess(List<String> ids, MaterialsApprovalSetVO materialsApprovalSetVO) {
        List<WfMaterialsScrappedProcess> list = new ArrayList<>(10);
        if (CollectionUtils.isNotEmpty(ids)) {
            ids.forEach(id -> {
                WfMaterialsScrappedProcess wfMaterialsScrappedProcess = new WfMaterialsScrappedProcess();
                wfMaterialsScrappedProcess.setMaterialId(id);
//                wfMaterialsScrappedProcess.setType(materialsApprovalSetVO.getType());
//                wfMaterialsScrappedProcess.setApproved(materialsApprovalSetVO.getApproved());
//                wfMaterialsScrappedProcess.setDefinitionId(materialsApprovalSetVO.getDefinitionId());
//                wfMaterialsScrappedProcess.setDeploymentId(materialsApprovalSetVO.getDeploymentId());
                wfMaterialsScrappedProcess.setCreatedBy(SecurityUtils.getUsername());
                wfMaterialsScrappedProcess.setCreatedTime(new Date());
                wfMaterialsScrappedProcess.setUpdatedBy(SecurityUtils.getUsername());
                wfMaterialsScrappedProcess.setUpdatedTime(new Date());
                wfMaterialsScrappedProcessMapper.insert(wfMaterialsScrappedProcess);
                list.add(wfMaterialsScrappedProcess);
            });
        }
        return list;
    }

    @Override
    public List<WfTaskProcess> selectList(List<String> taskId) {
        // 查询是否存在关联关系
        R<List<WfTaskProcess>> response = projectTaskProcessFeignService.listByExtraIds(taskId, SecurityConstants.INNER);
        if (response == null || response.getCode() != 200) {
            log.warn("根据任务ID集合查询任务流程失败: {}", response != null ? response.getMsg() : "response is null");
            return Collections.emptyList();
        }
        return response.getData();
    }

    @Override
    public List<WfTaskProcess> selectWfTaskProcessList(List<String> extraId, String type) {
        List<WfTaskProcess> list = new ArrayList<>(10);
        if (CollectionUtils.isNotEmpty(extraId)) {
            Map<String, Object> params = new HashMap<>(2);
            params.put("extraIds", extraId);
            params.put("type", type);
            R<List<WfTaskProcess>> response = projectTaskProcessFeignService.listByExtraIdsAndType(params, SecurityConstants.INNER);
            if (response == null || response.getCode() != 200) {
                log.warn("根据业务ID集合和类型查询任务流程失败: {}", response != null ? response.getMsg() : "response is null");
                return list;
            }
            list = response.getData();
        }
        return list;
    }

    @Override
    public void updateProviderApproval(String providerId) {
        R<WfTaskProcess> response = projectTaskProcessFeignService.clearAssociation(providerId, ProcessUtils.SUPPLIER_APPROVAL_TYPE, false, SecurityConstants.INNER);
        if (response == null || response.getCode() != 200) {
            log.warn("清除供应商审批关联失败: {}, providerId: {}", response != null ? response.getMsg() : "response is null", providerId);
        }
    }

    @Override
    public List<WfMaterialsScrappedProcess> selectScrappedList(List<String> ids) {
        List<WfMaterialsScrappedProcess> list = new ArrayList<>(10);
        if (CollectionUtils.isNotEmpty(ids)) {
            LambdaQueryWrapper<WfMaterialsScrappedProcess> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(WfMaterialsScrappedProcess::getMaterialId, ids);
            list = wfMaterialsScrappedProcessMapper.selectList(queryWrapper);
        }
        return list;
    }

    private void extracted(String definitionId, String deploymentId, WfTaskProcess wfTaskProcess) {
        wfTaskProcess.setDefinitionId(definitionId);
        wfTaskProcess.setDeploymentId(deploymentId);
        wfTaskProcess.setCreatedBy(SecurityUtils.getUsername());
        wfTaskProcess.setCreatedTime(new Date());
        wfTaskProcess.setUpdatedBy(SecurityUtils.getUsername());
        wfTaskProcess.setUpdatedTime(new Date());
    }
}
