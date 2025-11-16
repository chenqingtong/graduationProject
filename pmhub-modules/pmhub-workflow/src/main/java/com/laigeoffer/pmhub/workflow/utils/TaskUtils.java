package com.laigeoffer.pmhub.workflow.utils;

import cn.hutool.core.util.ObjectUtil;
import com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.workflow.common.constant.TaskConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流任务工具类
 * 注意：Flowable 相关方法已标记为废弃，仅用于兼容旧代码，简化审批流程不使用这些方法
 *
 * @author chenqingtong
 * @createTime 2024/4/24 12:42
 */
public class TaskUtils {

    /**
     * 获取当前用户ID（字符串格式）
     * 
     * @return 用户ID字符串
     */
    public static String getUserId() {
        return String.valueOf(SecurityUtils.getUserId());
    }

    /**
     * 获取用户组信息
     * 
     * @deprecated Flowable 相关方法，已禁用 Flowable，此方法仅用于兼容旧代码
     * @return candidateGroup
     */
    @Deprecated
    public static List<String> getCandidateGroup() {
        List<String> list = new ArrayList<>();
        LoginUser user = SecurityUtils.getLoginUser();
        if (ObjectUtil.isNotNull(user)) {
            if (ObjectUtil.isNotEmpty(user.getUser().getRoles())) {
                user.getUser().getRoles().forEach(role -> list.add(TaskConstants.ROLE_GROUP_PREFIX + role.getRoleId()));
            }
            if (ObjectUtil.isNotNull(user.getDeptId())) {
                list.add(TaskConstants.DEPT_GROUP_PREFIX + user.getDeptId());
            }
        }
        return list;
    }

}
