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
 * @author canghe
 * @createTime 2022/3/24 20:57
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
     *
     * @param type 审批类型
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
