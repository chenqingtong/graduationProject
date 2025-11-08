package com.laigeoffer.pmhub.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laigeoffer.pmhub.workflow.domain.WfApprovalTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 简化审批任务Mapper接口
 * 
 * @author system
 * @date 2024
 */
@Mapper
public interface WfApprovalTaskMapper extends BaseMapper<WfApprovalTask> {

    /**
     * 查询用户的待办审批任务列表
     * 
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param deptIds 用户部门ID列表
     * @return 待办审批任务列表
     */
    List<WfApprovalTask> selectTodoListByUserId(
            @Param("userId") String userId,
            @Param("roleIds") List<Long> roleIds,
            @Param("deptIds") List<Long> deptIds
    );

    /**
     * 根据业务ID和类型查询审批任务列表
     * 
     * @param extraId 业务ID
     * @param type 审批类型
     * @return 审批任务列表
     */
    List<WfApprovalTask> selectByExtraIdAndType(
            @Param("extraId") String extraId,
            @Param("type") String type
    );

    /**
     * 根据业务ID和类型查询待审批任务数量
     * 
     * @param extraId 业务ID
     * @param type 审批类型
     * @return 待审批任务数量
     */
    int countPendingByExtraIdAndType(
            @Param("extraId") String extraId,
            @Param("type") String type
    );

    /**
     * 根据业务ID和类型查询已通过审批任务数量
     * 
     * @param extraId 业务ID
     * @param type 审批类型
     * @return 已通过审批任务数量
     */
    int countApprovedByExtraIdAndType(
            @Param("extraId") String extraId,
            @Param("type") String type
    );

    /**
     * 更新审批任务状态
     * 
     * @param id 审批任务ID
     * @param status 新状态
     * @param approvalComment 审批意见
     * @param approverId 审批人ID
     * @return 更新行数
     */
    int updateStatus(
            @Param("id") String id,
            @Param("status") String status,
            @Param("approvalComment") String approvalComment,
            @Param("approverId") String approverId
    );

    /**
     * 取消指定业务的所有待审批任务
     * 
     * @param extraId 业务ID
     * @param type 审批类型
     * @return 更新行数
     */
    int cancelPendingTasks(
            @Param("extraId") String extraId,
            @Param("type") String type
    );

    /**
     * 查询用户的已办审批任务列表（已通过或已拒绝）
     * 
     * @param userId 用户ID
     * @param roleIds 用户角色ID列表
     * @param deptIds 用户部门ID列表
     * @return 已办审批任务列表
     */
    List<WfApprovalTask> selectFinishedListByUserId(
            @Param("userId") String userId,
            @Param("roleIds") List<Long> roleIds,
            @Param("deptIds") List<Long> deptIds
    );
}

