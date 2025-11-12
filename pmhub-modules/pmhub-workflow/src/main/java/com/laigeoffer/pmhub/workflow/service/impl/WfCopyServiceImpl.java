package com.laigeoffer.pmhub.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laigeoffer.pmhub.api.project.ProjectTaskProcessFeignService;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.core.domain.PageQuery;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.page.Table2DataInfo;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.base.core.utils.StringUtils;
import com.laigeoffer.pmhub.workflow.domain.WfApprovalTask;
import com.laigeoffer.pmhub.workflow.domain.WfCopy;
import com.laigeoffer.pmhub.workflow.domain.bo.WfCopyBo;
import com.laigeoffer.pmhub.workflow.domain.bo.WfTaskBo;
import com.laigeoffer.pmhub.workflow.domain.vo.WfCopyVo;
import com.laigeoffer.pmhub.workflow.mapper.WfApprovalTaskMapper;
import com.laigeoffer.pmhub.workflow.mapper.WfCopyMapper;
import com.laigeoffer.pmhub.workflow.service.IWfCopyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程抄送Service业务层处理
 *
 * @author canghe
 * @date 2022-05-19
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class WfCopyServiceImpl implements IWfCopyService {

    private final WfCopyMapper baseMapper;

    private final HistoryService historyService;

    private final WfApprovalTaskMapper wfApprovalTaskMapper;

    // Feign 客户端在运行时由 Spring Cloud OpenFeign 动态生成，IDE 无法识别，但运行时可以正常注入
    private final ProjectTaskProcessFeignService projectTaskProcessFeignService;

    /**
     * 查询流程抄送
     *
     * @param copyId 流程抄送主键
     * @return 流程抄送
     */
    @Override
    public WfCopyVo queryById(Long copyId){
        return baseMapper.selectVoById(copyId);
    }

    /**
     * 查询流程抄送列表
     *
     * @param bo 流程抄送
     * @return 流程抄送
     */
    @Override
    public Table2DataInfo<WfCopyVo> selectPageList(WfCopyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WfCopy> lqw = buildQueryWrapper(bo);
        lqw.orderByDesc(WfCopy::getCreateTime);
        Page<WfCopyVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if (CollUtil.isNotEmpty(result.getRecords())) {
            enrichCopyRecords(result.getRecords());
        }
        return Table2DataInfo.build(result);
    }

    /**
     * 查询流程抄送列表
     *
     * @param bo 流程抄送
     * @return 流程抄送
     */
    @Override
    public List<WfCopyVo> selectList(WfCopyBo bo) {
        LambdaQueryWrapper<WfCopy> lqw = buildQueryWrapper(bo);
        List<WfCopyVo> list = baseMapper.selectVoList(lqw);
        if (CollUtil.isNotEmpty(list)) {
            enrichCopyRecords(list);
        }
        return list;
    }

    private LambdaQueryWrapper<WfCopy> buildQueryWrapper(WfCopyBo bo) {
        LambdaQueryWrapper<WfCopy> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, WfCopy::getUserId, bo.getUserId());
        lqw.like(StringUtils.isNotBlank(bo.getProcessName()), WfCopy::getProcessName, bo.getProcessName());
        lqw.like(StringUtils.isNotBlank(bo.getOriginatorName()), WfCopy::getOriginatorName, bo.getOriginatorName());
        return lqw;
    }

    private void enrichCopyRecords(List<WfCopyVo> records) {
        log.info("开始补充抄送记录附加信息，记录数：{}", records.size());
        List<String> taskIds = records.stream()
            .map(WfCopyVo::getTaskId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
        if (CollUtil.isEmpty(taskIds)) {
            log.info("抄送记录中没有任务ID，跳过补充逻辑");
            return;
        }
        List<WfApprovalTask> approvalTasks = wfApprovalTaskMapper.selectBatchIds(taskIds);
        if (CollUtil.isEmpty(approvalTasks)) {
            log.info("未查询到对应的审批任务，taskIds={}", taskIds);
            return;
        }
        Map<String, WfApprovalTask> taskMap = approvalTasks.stream()
            .collect(Collectors.toMap(WfApprovalTask::getId, task -> task, (left, right) -> left));
        Map<String, String> taskNameCache = new HashMap<>(taskMap.size());
        for (WfCopyVo record : records) {
            WfApprovalTask approvalTask = taskMap.get(record.getTaskId());
            if (approvalTask == null) {
                log.warn("审批任务不存在，taskId={}", record.getTaskId());
                continue;
            }
            record.setStatus(approvalTask.getStatus());
            record.setApprovalComment(approvalTask.getApprovalComment());
            record.setExtraId(approvalTask.getExtraId());
            record.setDetailUrl(approvalTask.getUrl());

            String extraId = approvalTask.getExtraId();
            if (StringUtils.isBlank(extraId)) {
                log.info("审批任务{}未绑定业务ID(extraId)，跳过查询任务名称", approvalTask.getId());
                continue;
            }
            String taskName = taskNameCache.get(extraId);
            if (taskName == null) {
                log.info("调用项目服务查询任务名称，extraId={}", extraId);
                R<String> response = projectTaskProcessFeignService.getTaskNameById(extraId, SecurityConstants.INNER);
                if (response != null && response.getCode() == 200) {
                    taskName = StringUtils.isNotBlank(response.getData()) ? response.getData() : response.getMsg();
                    log.info("项目服务返回任务名称，extraId={}, taskName={}", extraId, taskName);
                } else {
                    log.warn("项目服务查询任务名称失败，extraId={}，响应={}", extraId, response);
                    taskName = "";
                }
                taskNameCache.put(extraId, taskName);
            }
            if (StringUtils.isNotBlank(taskName)) {
                record.setTaskName(taskName);
                // 兼容现有前端字段
                record.setProcessName(taskName);
            } else {
                log.info("未获取到任务名称，extraId={}", extraId);
            }
        }
    }

    @Override
    public Boolean makeCopy(WfTaskBo taskBo) {
        if (StringUtils.isBlank(taskBo.getCopyUserIds())) {
            // 若抄送用户为空，则不需要处理，返回成功
            return true;
        }
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(taskBo.getProcInsId()).singleResult();
        String[] ids = taskBo.getCopyUserIds().split(",");
        List<WfCopy> copyList = new ArrayList<>(ids.length);
        Long originatorId = SecurityUtils.getUserId();
        SysUser sysUser = baseMapper.selectUserById(originatorId);
        String originatorName = sysUser.getNickName();
        String title = historicProcessInstance.getProcessDefinitionName() + "-" + taskBo.getTaskName();
        String currentUsername = SecurityUtils.getUsername();
        Date currentTime = new Date();
        for (String id : ids) {
            Long userId = Long.valueOf(id);
            WfCopy copy = new WfCopy();
            copy.setTitle(title);
            copy.setProcessId(historicProcessInstance.getProcessDefinitionId());
            copy.setProcessName(historicProcessInstance.getProcessDefinitionName());
            copy.setDeploymentId(historicProcessInstance.getDeploymentId());
            copy.setInstanceId(taskBo.getProcInsId());
            copy.setTaskId(taskBo.getTaskId());
            copy.setUserId(userId);
            copy.setOriginatorId(originatorId);
            copy.setOriginatorName(originatorName);
            // 设置创建时间和更新时间
            copy.setCreateTime(currentTime);
            copy.setUpdateTime(currentTime);
            copy.setCreateBy(currentUsername);
            copy.setUpdateBy(currentUsername);
            copyList.add(copy);
        }
        return baseMapper.insertBatch(copyList);
    }
}
