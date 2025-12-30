package com.laigeoffer.pmhub.workflow.controller;

import com.laigeoffer.pmhub.base.core.core.controller.BaseController;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.dto.ApprovalSetDTO;
import com.laigeoffer.pmhub.base.core.enums.ProjectStatusEnum;
import com.laigeoffer.pmhub.base.security.annotation.DistributedLock;
import com.laigeoffer.pmhub.base.security.annotation.InnerAuth;
import com.laigeoffer.pmhub.workflow.service.IWfDeployService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程部署
 *
 * @author chenqingtong
 * @createTime 2024/3/24 20:57
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/deploy")
public class WfDeployController extends BaseController {

    private final IWfDeployService deployService;
    /**
     * 已下线 flowable 管理功能接口，保留审批设置相关接口供业务方使用
     */

    /**
     * 查询各类型下的审批设置
     * 此接口用于获取不同业务类型的审批配置信息，在前端设置审批规则时调用
     *
     * 调用场景：
     * - 前端任务详情页面点击"设置任务"按钮时调用
     * - 根据任务类型和任务ID查询对应的审批设置
     * - 返回审批设置信息供前端展示和编辑
     *
     * @param type 审批类型，路径参数，取值范围：
     *            - "task": 任务审批设置（需要taskId参数）
     *            - "project": 项目审批设置
     *            - "template": 模板审批设置
     *            - 其他业务类型对应的字符串标识
     * @param taskId 任务ID，查询参数，可选。
     *              当type为"task"时，必须提供具体的taskId来查询该任务的审批设置
     * @return R<MaterialsApprovalSetVO> 审批设置信息，包含：
     *         - approved: 是否需要审批 ("0":需要, "1":无需)
     *         - type: 审批类型
     *         - deploymentId: 流程部署ID
     *         - definitionId: 流程定义ID
     *         - approvalInfo: 审批人信息(JSON格式)
     */
    @GetMapping("/refApproval/{type}")
    public R<?> findApprovalByType(@PathVariable(value = "type") String type, @RequestParam(value = "taskId", required = false) String taskId) {
        return R.ok(deployService.queryApprovalSet(type, taskId));
    }

    /**
     * 更新审批设置
     * @param approvalSetDTO
     * @return
     */
    @InnerAuth
    @PostMapping("/updateApprovalSet")
    @DistributedLock(key = "#approvalSetDTO.approved", lockTime = 10L, keyPrefix = "workflow-approve-")
    public R<?> updateApprovalSet(@RequestBody ApprovalSetDTO approvalSetDTO) {
        return R.ok(deployService.updateApprovalSet(approvalSetDTO, ProjectStatusEnum.PROJECT.getStatusName()));
    }

    /**
     * 更新审批设置2
     * @param approvalSetDTO
     * @return
     */
    @InnerAuth
    @PostMapping("/updateApprovalSet2")
    public R<?> updateApprovalSet2(@RequestBody ApprovalSetDTO approvalSetDTO) {
        return R.ok(deployService.updateApprovalSet2(approvalSetDTO, ProjectStatusEnum.PROJECT.getStatusName()));
    }

    /**
     * 查询流程部署关联表单信息
     * @param taskId
     * @return
     */
    @InnerAuth
    @GetMapping("/selectList")
    public R<?> selectList(List<String> taskId) {
        return R.ok(deployService.selectList(taskId));
    }

    /**
     * 添加&更新审批设置
     * @param approvalSetDTO
     * @return
     */
    @InnerAuth
    @PostMapping("/insertOrUpdateApprovalSet")
    public R<Boolean> insertOrUpdateApprovalSet(@RequestBody ApprovalSetDTO approvalSetDTO) {
        // 使用新的重载方法，支持审批人信息
        if (approvalSetDTO.getExtraId() != null && approvalSetDTO.getType() != null) {
            return R.ok(deployService.insertOrUpdateApprovalSet(approvalSetDTO));
        } else {
            // 兼容旧接口
            return R.ok(deployService.insertOrUpdateApprovalSet(approvalSetDTO.getExtraId(), approvalSetDTO.getType(), approvalSetDTO.getApproved(), approvalSetDTO.getDefinitionId(), approvalSetDTO.getDeploymentId()));
        }
    }

    /**
     * 添加审批设置
     * @return
     */
    @InnerAuth
    @PostMapping("/insertApprovalSet")
    public R<?> insertApprovalSet() {
        return R.ok(deployService.insertApprovalSet());
    }

}
