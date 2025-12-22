package com.laigeoffer.pmhub.project.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.laigeoffer.pmhub.api.system.UserFeignService;
import com.laigeoffer.pmhub.api.system.domain.dto.SysUserDTO;
import com.laigeoffer.pmhub.api.workflow.DeployFeignService;
import com.laigeoffer.pmhub.base.core.config.PmhubConfig;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.dto.ApprovalSetDTO;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser;
import com.laigeoffer.pmhub.base.core.core.domain.vo.SysUserVO;
import com.laigeoffer.pmhub.base.core.enums.LogTypeEnum;
import com.laigeoffer.pmhub.base.core.enums.ProjectStatusEnum;
import com.laigeoffer.pmhub.base.core.enums.ProjectTaskPriorityEnum;
import com.laigeoffer.pmhub.base.core.enums.ProjectTaskStatusEnum;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.base.core.utils.DateUtils;
import com.laigeoffer.pmhub.base.core.utils.file.FileUtils;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.base.notice.domain.dto.EmailNoticeDTO;
import com.laigeoffer.pmhub.base.notice.service.EmailNoticeService;
import com.laigeoffer.pmhub.project.domain.*;
import com.laigeoffer.pmhub.project.domain.vo.project.ProjectVO;
import com.laigeoffer.pmhub.project.domain.vo.project.log.*;
import com.laigeoffer.pmhub.project.domain.vo.project.member.ProjectMemberResVO;
import com.laigeoffer.pmhub.project.domain.vo.project.task.*;
import com.laigeoffer.pmhub.project.mapper.*;
import com.laigeoffer.pmhub.project.service.ProjectLogService;
import com.laigeoffer.pmhub.project.service.ProjectTaskService;
import com.laigeoffer.pmhub.project.service.task.QueryTaskLogFactory;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.rmi.ServerException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目任务服务实现类
 * 负责项目任务相关的核心业务逻辑处理，包括：
 * 1. 任务的增删改查
 * 2. 任务状态管理
 * 3. 任务成员管理
 * 4. 任务日志和评论
 * 5. 任务统计和导出
 * 6. 任务审批流程集成
 *
 * @author chenqingtong
 * @date 2024-12-14 15:00
 */
@Service
@Slf4j
public class ProjectTaskServiceImpl extends ServiceImpl<ProjectTaskMapper, ProjectTask> implements ProjectTaskService {

    /** 项目任务数据访问层 */
    @Autowired
    private ProjectTaskMapper projectTaskMapper;

    /** 项目成员数据访问层 */
    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    /** 项目日志服务 */
    @Autowired
    private ProjectLogService projectLogService;

    /** 项目数据访问层 */
    @Autowired
    private ProjectMapper projectMapper;

    /** 项目阶段数据访问层 */
    @Autowired
    private ProjectStageMapper projectStageMapper;

    /** 任务日志查询工厂 */
    @Autowired
    private QueryTaskLogFactory queryTaskLogFactory;

    /** 项目文件数据访问层 */
    @Autowired
    private ProjectFileMapper projectFileMapper;

    /** 项目任务流程数据访问层 */
    @Autowired
    private ProjectTaskProcessMapper projectTaskProcessMapper;

    /** 远程调用流程服务（Feign），用于处理任务审批流程 */
    @Resource
    private DeployFeignService wfDeployService;

    /** 远程调用用户服务（Feign），用于查询用户信息 */
    @Resource
    private UserFeignService userFeignService;

    /** 邮件通知服务 */
    @Autowired
    private EmailNoticeService emailNoticeService;

    /**
     * 查询今日任务数量
     * 统计开始时间在今天的任务数量
     *
     * @return 今日任务数量
     */
    @Override
    public Long queryTodayTaskNum() {
        // 构建查询条件：开始时间在今天范围内，且未删除
        LambdaQueryWrapper<ProjectTask> queryWrapper = new LambdaQueryWrapper<>();
        String todayStart = DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD_HH_MM_SS)
                .format(LocalDateTime.now().with(LocalTime.MIN)); // 今天00:00:00
        String todayEnd = DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD_HH_MM_SS)
                .format(LocalDateTime.now().with(LocalTime.MAX)); // 今天23:59:59

        queryWrapper.between(ProjectTask::getBeginTime, todayStart, todayEnd)
                .eq(ProjectTask::getDeleted, 0);

        Long count = projectTaskMapper.selectCount(queryWrapper);
        return count == null ? 0L : count;
    }

    /**
     * 查询逾期任务数量
     * 统计截止时间已过但未完成的任务数量
     *
     * @return 逾期任务数量
     */
    @Override
    public Long queryOverdueTaskNum() {
        // 构建查询条件：截止时间小于当前时间，未删除，且未完成
        LambdaQueryWrapper<ProjectTask> queryWrapper = new LambdaQueryWrapper<>();
        String now = DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD_HH_MM_SS)
                .format(LocalDateTime.now());

        queryWrapper.lt(ProjectTask::getCloseTime, now) // 截止时间小于当前时间
                .eq(ProjectTask::getDeleted, 0) // 未删除
                .ne(ProjectTask::getExecuteStatus, ProjectTaskStatusEnum.FINISHED.getStatus()); // 未完成

        Long count = projectTaskMapper.selectCount(queryWrapper);
        return count == null ? 0L : count;
    }

    @Override
    public List<TaskStatisticsVO> queryTaskStatisticsList() {
        LambdaQueryWrapper<ProjectTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTask::getDeleted, 0);
        List<TaskStatisticsVO> taskStatisticsVOList = new ArrayList<>(10);
        List<ProjectTask> list = projectTaskMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            for (ProjectTaskStatusEnum value : ProjectTaskStatusEnum.values()) {
                TaskStatisticsVO taskStatisticsVO = new TaskStatisticsVO();
                taskStatisticsVO.setStatus(value.getStatus());
                taskStatisticsVO.setStatusName(value.getStatusName());
                taskStatisticsVO.setTaskNum(0);
                taskStatisticsVOList.add(taskStatisticsVO);
            }
            return taskStatisticsVOList;
        } else {
            // 待认领
            TaskStatisticsVO noClaim = new TaskStatisticsVO();
            noClaim.setStatus(ProjectTaskStatusEnum.NO_CLAIMED.getStatus());
            noClaim.setStatusName(ProjectTaskStatusEnum.NO_CLAIMED.getStatusName());
            noClaim.setTaskNum((int) list.stream().filter(a -> a.getUserId() == null).count());
            taskStatisticsVOList.add(noClaim);
            // 进行中
            List<ProjectTask> doingList = list.stream().filter(a -> ProjectTaskStatusEnum.DOING.getStatus().equals(a.getStatus())).collect(Collectors.toList());
            TaskStatisticsVO doing = new TaskStatisticsVO();
            doing.setStatus(ProjectTaskStatusEnum.DOING.getStatus());
            doing.setStatusName(ProjectTaskStatusEnum.DOING.getStatusName());
            doing.setTaskNum(doingList.size());
            taskStatisticsVOList.add(doing);
            // 已完成
            List<ProjectTask> finishList = list.stream().filter(a -> ProjectTaskStatusEnum.FINISHED.getStatus().equals(a.getStatus())).collect(Collectors.toList());
            TaskStatisticsVO finish = new TaskStatisticsVO();
            finish.setStatus(ProjectTaskStatusEnum.FINISHED.getStatus());
            finish.setStatusName(ProjectTaskStatusEnum.FINISHED.getStatusName());
            finish.setTaskNum(finishList.size());
            taskStatisticsVOList.add(finish);
            // 已逾期
            List<ProjectTask> overdueList = list.stream().filter(a -> a.getCloseTime() != null && a.getCloseTime().getTime() < new Date().getTime()).collect(Collectors.toList());
            TaskStatisticsVO overdue = new TaskStatisticsVO();
            overdue.setStatus(ProjectTaskStatusEnum.OVERDUE.getStatus());
            overdue.setStatusName(ProjectTaskStatusEnum.OVERDUE.getStatusName());
            overdue.setTaskNum(overdueList.size());
            taskStatisticsVOList.add(overdue);
        }

        return taskStatisticsVOList;
    }

    @Override
    public PageInfo<TaskResVO> queryMyTaskList(TaskReqVO taskReqVO) {
        PageInfo<TaskResVO> pageInfo;
        PageHelper.startPage(taskReqVO.getPageNum(), taskReqVO.getPageSize());
        switch (taskReqVO.getType()) {
            // 我执行的
            case 1:
                pageInfo = new PageInfo<>(projectTaskMapper.queryMyExecutedTaskList(taskReqVO.getProjectId(), SecurityUtils.getUserId()));
                if (CollectionUtils.isNotEmpty(pageInfo.getList())) {
                    pageInfo.getList().forEach(a -> a.setStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getStatus())));
                }
                return pageInfo;
            // 我参与的
            case 2:
                pageInfo = new PageInfo<>(projectTaskMapper.queryMyPartookTaskList(taskReqVO.getProjectId(), SecurityUtils.getUserId()));
                if (CollectionUtils.isNotEmpty(pageInfo.getList())) {
                    pageInfo.getList().forEach(a -> a.setStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getStatus())));
                }
                return pageInfo;
            // 我创建的
            case 3:
                pageInfo = new PageInfo<>(projectTaskMapper.queryMyCreatedTaskList(taskReqVO.getProjectId(), SecurityUtils.getUsername()));
                if (CollectionUtils.isNotEmpty(pageInfo.getList())) {
                    pageInfo.getList().forEach(a -> a.setStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getStatus())));
                }
                return pageInfo;
        }
        return new PageInfo<>();
    }

    @Override
    public TaskStatusStatsVO queryTaskStatusStats(ProjectVO projectVO) {
        TaskStatusStatsVO taskStatusStatsVO = new TaskStatusStatsVO();
        LambdaQueryWrapper<ProjectTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTask::getProjectId, projectVO.getProjectId());
        queryWrapper.eq(ProjectTask::getDeleted, 0);
        List<ProjectTask> projectTasks = projectTaskMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(projectTasks)) {
            taskStatusStatsVO.setTotal(projectTasks.size());
            taskStatusStatsVO.setToBeAssign((int) projectTasks.stream().filter(a -> a.getUserId() == null).count());
            taskStatusStatsVO.setUnDone((int) projectTasks.stream().filter(a -> !Objects.equals(a.getStatus(), ProjectTaskStatusEnum.FINISHED.getStatus())).count());
            taskStatusStatsVO.setDone((int) projectTasks.stream().filter(a -> Objects.equals(a.getStatus(), ProjectTaskStatusEnum.FINISHED.getStatus())).count());
            taskStatusStatsVO.setDoneOverdue((int) projectTasks.stream().filter(a -> a.getCloseTime() != null && a.getCloseTime().getTime() < new Date().getTime() && Objects.equals(a.getStatus(), ProjectTaskStatusEnum.FINISHED.getStatus())).count());
            taskStatusStatsVO.setExpireToday((int) projectTasks.stream().filter(a -> a.getCloseTime() != null && DateUtils.dateTime(new Date()).compareTo(DateUtils.dateTime(a.getCloseTime())) == 0).count());
            taskStatusStatsVO.setTimeUndetermined((int) projectTasks.stream().filter(a -> a.getEndTime() == null).count());
            taskStatusStatsVO.setOverdue((int) projectTasks.stream().filter(a -> a.getCloseTime() != null && a.getCloseTime().getTime() < new Date().getTime()).count());

        } else {
            taskStatusStatsVO.setTotal(0);
            taskStatusStatsVO.setToBeAssign(0);
            taskStatusStatsVO.setUnDone(0);
            taskStatusStatsVO.setDone(0);
            taskStatusStatsVO.setDoneOverdue(0);
            taskStatusStatsVO.setExpireToday(0);
            taskStatusStatsVO.setTimeUndetermined(0);
            taskStatusStatsVO.setOverdue(0);
        }
        return taskStatusStatsVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void  deleteTask(TaskIdsVO taskIdsVO) {
        LambdaUpdateChainWrapper<ProjectTask> wrapper = lambdaUpdate().in(ProjectTask::getId, taskIdsVO.getTaskIdList());
        wrapper.set(ProjectTask::getDeleted, 1).set(ProjectTask::getDeletedTime, new Date());
        wrapper.update();
    }

    @Override
    public TaskResVO detail(TaskReqVO taskReqVO) {
        TaskResVO detail = projectTaskMapper.detail(taskReqVO.getTaskId());
        detail.setStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(detail.getStatus()));
        detail.setExecuteStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(detail.getExecuteStatus()));
        // 按创建人账号查昵称
        if (StringUtils.isNotBlank(detail.getCreatedBy())) {
            String creatorNickName = getNickNameByUserName(detail.getCreatedBy());
            if (StringUtils.isNotBlank(creatorNickName)) {
                detail.setCreatedBy(creatorNickName);
            }
        }
        // 填充执行人昵称
        if (detail.getUserId() != null) {
            String executorNickName = getExecutorNickName(detail.getUserId());
            if (StringUtils.isNotBlank(executorNickName)) {
                detail.setExecutor(executorNickName);
            }
        }
        detail.setTaskPriorityName(ProjectTaskPriorityEnum.getStatusNameByStatus(detail.getTaskPriority()));
        return detail;
    }

    private List<SysUser> getSysUserList(List<Long> userIds) {
        // 查询用户信息
        SysUserDTO sysUserDTO = new SysUserDTO();
        sysUserDTO.setUserIds(userIds);
        R<List<SysUserVO>> userResult = userFeignService.listOfInner(sysUserDTO, SecurityConstants.INNER);

        if (Objects.isNull(userResult) || CollectionUtils.isEmpty(userResult.getData())) {
            throw new ServiceException("远程调用查询用户列表：" + userIds + " 失败");
        }
        List<SysUserVO> userVOList = userResult.getData();
        return userVOList.stream()
                .map(userVO -> (SysUser) userVO)
                .collect(Collectors.toList());
    }

    private String getExecutorNickName(Long userId) {
        if (userId == null) {
            return null;
        }
        List<SysUser> sysUsers = getSysUserList(Collections.singletonList(userId));
        if (CollectionUtils.isEmpty(sysUsers)) {
            return null;
        }
        return sysUsers.get(0).getNickName();
    }

    private void fillExecutorInfo(List<TaskResVO> tasks) {
        List<Long> needResolveUserIds = tasks.stream()
                .filter(task -> Objects.nonNull(task.getUserId()) && StringUtils.isBlank(task.getExecutor()))
                .map(TaskResVO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(needResolveUserIds)) {
            return;
        }
        List<SysUser> sysUsers = getSysUserList(needResolveUserIds);
        Map<Long, SysUser> userMap = sysUsers.stream()
                .collect(Collectors.toMap(SysUser::getUserId, user -> user, (existing, replacement) -> existing));
        Map<String, String> assignToUpdateMap = new HashMap<>();
        tasks.forEach(task -> {
            if (StringUtils.isBlank(task.getExecutor())) {
                SysUser sysUser = userMap.get(task.getUserId());
                if (Objects.nonNull(sysUser)) {
                    String nickName = sysUser.getNickName();
                    if (StringUtils.isNotBlank(nickName)) {
                        task.setExecutor(nickName);
                        if (StringUtils.isNotBlank(task.getTaskId())) {
                            assignToUpdateMap.put(task.getTaskId(), nickName);
                        }
                    }
                }
            }
        });
        if (!assignToUpdateMap.isEmpty()) {
            assignToUpdateMap.forEach((taskId, nickName) -> projectTaskMapper.updateAssignTo(taskId, nickName));
        }
    }

    private String getNickNameByUserName(String userName) {
        SysUserDTO sysUserDTO = new SysUserDTO();
        sysUserDTO.setUserName(userName);
        R<List<SysUserVO>> userResult = userFeignService.listOfInner(sysUserDTO, SecurityConstants.INNER);
        if (Objects.isNull(userResult) || CollectionUtils.isEmpty(userResult.getData())) {
            return null;
        }
        return userResult.getData().get(0).getNickName();
    }

    @Override
    public List<ProjectMemberResVO> queryExecutorList(TaskReqVO taskReqVO) {
        List<ProjectMemberResVO> list = projectMemberMapper.queryExecutorList(taskReqVO.getProjectId());
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 拿到userids
        List<Long> userIds = list.stream().map(ProjectMemberResVO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        SysUserDTO sysUserDTO = new SysUserDTO();
        sysUserDTO.setUserIds(userIds);
        R<List<SysUserVO>> userResult = userFeignService.listOfInner(sysUserDTO, SecurityConstants.INNER);

        if (Objects.isNull(userResult) || CollectionUtils.isEmpty(userResult.getData())) {
            throw new ServiceException("远程调用查询用户列表：" + userIds + " 失败");
        }
        List<SysUserVO> userVOList = userResult.getData();

        // 匹配设置值
        Map<Long, SysUserVO> userMap = userVOList.stream().collect(Collectors.toMap(SysUserVO::getUserId, a -> a));
        list.forEach(projectMemberResVO -> {
            SysUserVO sysUserVO = userMap.get(projectMemberResVO.getUserId());
            if (Objects.nonNull(sysUserVO)) {
                projectMemberResVO.setUserName(sysUserVO.getUserName());
                projectMemberResVO.setNickName(sysUserVO.getNickName());
                projectMemberResVO.setEmail(sysUserVO.getEmail());
                projectMemberResVO.setAvatar(sysUserVO.getAvatar());
            }
        });
        return list;
    }

    @Override
    public PageInfo<TaskResVO> list(TaskReqVO taskReqVO) {
        PageHelper.startPage(taskReqVO.getPageNum(), taskReqVO.getPageSize());
        List<TaskResVO> list = projectTaskMapper.list(taskReqVO, SecurityUtils.getUserId());
        if (CollectionUtils.isEmpty(list)) {
            return new PageInfo<>(list);
        }
        list.forEach(a -> {
            WorkFlowable workFlowable = new WorkFlowable();
            workFlowable.setTaskId(a.getTaskProcessId());
            workFlowable.setApproved(a.getApproved());
            workFlowable.setDeploymentId(a.getDeployId());
            workFlowable.setProcInsId(a.getProcInsId());
            workFlowable.setDefinitionId(a.getDefinitionId());
            a.setWorkFlowable(workFlowable);
            a.setTaskPriorityName(ProjectTaskPriorityEnum.getStatusNameByStatus(a.getTaskPriority()));
            a.setStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getStatus()));
            a.setExecuteStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getExecuteStatus()));
            if (a.getEndTime() != null && a.getBeginTime() != null) {
                a.setPeriod(DateUtils.differentDaysByMillisecond(a.getEndTime(), a.getBeginTime()));
            }
        });
        fillExecutorInfo(list);
        return new PageInfo<>(list);
    }

    /**
     * 添加新任务
     * 使用Seata分布式事务（AT模式）确保任务创建、成员添加、审批设置等操作的原子性
     * 创建任务时会自动：
     * 1. 验证项目状态
     * 2. 验证任务时间
     * 3. 创建任务记录
     * 4. 添加任务成员
     * 5. 记录操作日志
     * 6. 设置审批流程（如果需要）
     *
     * @param taskReqVO 任务请求对象，包含任务信息
     * @return 任务ID
     * @throws ServiceException 如果项目已暂停或审批服务调用失败
     */
    @Override
    @GlobalTransactional(name = "pmhub-project-addTask", rollbackFor = Exception.class) // Seata分布式事务，AT模式
    public String add(TaskReqVO taskReqVO) {
        // 获取全局事务ID，用于分布式事务追踪
        String xid = RootContext.getXID();
        log.info("---------------开始新建任务: " + "\t" + "xid: " + xid);

        // 1. 验证项目状态：如果项目已暂停，不允许新增任务
        if (ProjectStatusEnum.PAUSE.getStatus().equals(projectTaskMapper.queryProjectStatus(taskReqVO.getProjectId()))) {
            throw new ServiceException("归属项目已暂停，无法新增任务");
        }

        // 2. 验证任务时间：开始时间、结束时间、截止时间的逻辑关系
        validateTaskTime(taskReqVO.getBeginTime(), taskReqVO.getEndTime(), taskReqVO.getCloseTime());

        // 3. 创建任务实体
        ProjectTask projectTask = new ProjectTask();
        // 如果指定了父任务ID，则设置为子任务
        if (StringUtils.isNotBlank(taskReqVO.getTaskId())) {
            projectTask.setTaskPid(taskReqVO.getTaskId());
        }
        // 复制请求对象属性到任务实体
        BeanUtils.copyProperties(taskReqVO, projectTask);

        // 获取执行人昵称
        String executorNickName = getExecutorNickName(taskReqVO.getUserId());
        projectTask.setAssignTo(executorNickName);
        projectTask.setCreatedBy(SecurityUtils.getUsername());
        projectTask.setCreatedTime(new Date());
        projectTask.setUpdatedBy(SecurityUtils.getUsername());
        projectTask.setUpdatedTime(new Date());
        projectTaskMapper.insert(projectTask);

        // 4. 添加任务成员：将创建者添加为任务成员（标记为创建者）
        insertMember(projectTask.getId(), 1, SecurityUtils.getUserId());

        // 5. 记录任务创建日志
        saveLog("addTask", projectTask.getId(), taskReqVO.getProjectId(),
                taskReqVO.getTaskName(), "参与了任务", null);

        // 6. 如果指定了执行人且不是创建者，则将执行人添加为任务成员
        if (taskReqVO.getUserId() != null && !Objects.equals(taskReqVO.getUserId(), SecurityUtils.getUserId())) {
            insertMember(projectTask.getId(), 0, taskReqVO.getUserId());

            // 记录邀请成员日志
            String inviteeName = executorNickName;
            if (StringUtils.isBlank(inviteeName)) {
                inviteeName = getExecutorNickName(taskReqVO.getUserId());
            }
            saveLog("invitePartakeTask", projectTask.getId(), taskReqVO.getProjectId(),
                    taskReqVO.getTaskName(), "邀请 " + inviteeName + " 参与任务", taskReqVO.getUserId());
        }

        // 7. 发送任务指派消息提醒（当前已注释，待实现）（已实现）
        extracted(taskReqVO.getTaskName(), taskReqVO.getUserId(),
                SecurityUtils.getUsername(), projectTask.getId());

        // 8. 添加或更新审批设置（远程调用 pmhub-workflow 微服务）
        ApprovalSetDTO approvalSetDTO = new ApprovalSetDTO(
                projectTask.getId(),
                ProjectStatusEnum.TASK.getStatusName(),
                taskReqVO.getApproved(),
                taskReqVO.getDefinitionId(),
                taskReqVO.getDeploymentId()
        );
        R<Boolean> result = wfDeployService.insertOrUpdateApprovalSet(approvalSetDTO, SecurityConstants.INNER);

        // 验证审批服务调用结果
        if (Objects.isNull(result) || Objects.isNull(result.getData())
                || R.fail().equals(result.getData())) {
            throw new ServiceException("远程调用审批服务失败");
        }

        log.info("---------------结束新建任务: " + "\t" + "xid: " + xid);
        return projectTask.getId();
    }

    /**
     * 发送任务指派邮件通知
     *
     * @param taskName 任务名称
     * @param userId 被指派用户ID
     * @param username 指派人用户名
     * @param taskId 任务ID
     */
    private void extracted(String taskName, Long userId, String username, String taskId) {
        // 如果用户ID为空，不发送通知
        if (userId == null) {
            return;
        }

        try {
            // 获取被指派用户信息
            List<SysUser> sysUsers = getSysUserList(Collections.singletonList(userId));
            if (CollectionUtils.isEmpty(sysUsers)) {
                log.warn("无法获取用户信息，跳过邮件通知，userId: {}", userId);
                return;
            }

            SysUser assignedUser = sysUsers.get(0);
            String email = assignedUser.getEmail();

            // 如果用户邮箱为空，不发送通知
            if (StringUtils.isBlank(email)) {
                log.debug("用户邮箱为空，跳过邮件通知，userId: {}", userId);
                return;
            }

            // 获取指派人昵称
            String assignerNickName = getExecutorNickName(SecurityUtils.getUserId());
            if (StringUtils.isBlank(assignerNickName)) {
                assignerNickName = username;
            }

            // 获取被指派人昵称
            String assigneeNickName = StringUtils.isBlank(assignedUser.getNickName())
                    ? "同事"
                    : assignedUser.getNickName();

            // 构建邮件内容
            String emailContent = buildTaskAssignEmailContent(
                    assigneeNickName,
                    assignerNickName,
                    taskName,
                    taskId
            );

            // 发送邮件
            EmailNoticeDTO noticeDTO = EmailNoticeDTO.builder()
                    .to(Collections.singletonList(email))
                    .subject("任务指派提醒")
                    .content(emailContent)
                    .htmlContent(true)
                    .build();

            emailNoticeService.send(noticeDTO);
            log.info("任务指派邮件通知已发送，任务ID: {}, 收件人: {}", taskId, email);

        } catch (Exception e) {
            log.error("发送任务指派邮件通知失败，任务ID: {}, 用户ID: {}", taskId, userId, e);
        }
    }

    /**
     * 构建任务指派邮件内容
     *
     * @param assigneeNickName 被指派人昵称
     * @param assignerNickName 指派人昵称
     * @param taskName 任务名称
     * @param taskId 任务ID
     * @return 邮件HTML内容
     */
    private String buildTaskAssignEmailContent(String assigneeNickName, String assignerNickName,
                                               String taskName, String taskId) {
        // 获取应用基础URL，如果未配置则使用相对路径（兼容旧配置）
        String baseUrl = PmhubConfig.getBaseUrl();
        String taskUrl;
        if (StringUtils.isNotBlank(baseUrl)) {
            // 确保baseUrl不以斜杠结尾
            baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            taskUrl = baseUrl + "/pmhub-project/my-task/info?taskId=" + taskId;
        } else {
            // 如果未配置baseUrl，使用相对路径（可能被邮件客户端转换为错误域名）
            log.warn("应用基础URL未配置，邮件链接将使用相对路径，可能导致链接错误。请在配置文件中设置 pmhub.base-url");
            taskUrl = "/pmhub-project/my-task/info?taskId=" + taskId;
        }

        StringBuilder content = new StringBuilder();
        content.append("<div style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
        content.append("<p>您好，").append(assigneeNickName).append("：</p>");
        content.append("<p>【").append(assignerNickName).append("】给您指派了任务【<strong>")
               .append(taskName).append("</strong>】，请及时处理！</p>");
        content.append("<p style='margin-top: 20px;'>");
        content.append("<a href='").append(taskUrl)
               .append("' style='display: inline-block; padding: 10px 20px; background-color: #409EFF; ")
               .append("color: #fff; text-decoration: none; border-radius: 4px;'>查看任务详情</a>");
        content.append("</p>");
        content.append("<p style='margin-top: 20px; color: #999; font-size: 12px;'>");
        content.append("此邮件由系统自动发送，请勿回复。");
        content.append("</p>");
        content.append("</div>");
        return content.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(TaskReqVO taskReqVO) {
        // 1. 获取原任务信息并验证项目状态
        ProjectTask oldObj = projectTaskMapper.selectById(taskReqVO.getTaskId());
        // 如果原项目或新切换的项目处于暂停状态，则禁止操作
        if (ProjectStatusEnum.PAUSE.getStatus().equals(projectTaskMapper.queryProjectStatus(oldObj.getProjectId()))) {
            throw new ServiceException("归属项目已暂停，无法操作任务");
        }
        if (ProjectStatusEnum.PAUSE.getStatus().equals(projectTaskMapper.queryProjectStatus(taskReqVO.getProjectId()))) {
            throw new ServiceException("该任务不能切换到已暂停的项目");
        }

        // 2. 验证任务时间的逻辑关系（如：开始时间不能晚于结束时间）
        validateTaskTime(taskReqVO.getBeginTime(), taskReqVO.getEndTime(), taskReqVO.getCloseTime());
         // TODO: 2024.06.24 暂时注释掉审批过滤，待远程调用
//        if (!Objects.equals(oldObj.getStatus(), taskReqVO.getStatus())) {
//            // 根据 taskId 去查询 是否需要审批
//            String queryApproved = projectTaskMapper.queryApproved(taskReqVO.getTaskId());
//            String approved = "0";
//            if (approved.equals(queryApproved)) {
//                throw new ServiceException("该任务需要审批，任务状态不允许手动修改");
//            } else {
//                if (!SecurityUtils.getUsername().equals(oldObj.getCreatedBy())) {
//                    throw new ServiceException("该任务不需要审批，只有创建人才能修改任务状态");
//                }
//            }
//        }
        ProjectTask projectTask = new ProjectTask();
        BeanUtils.copyProperties(taskReqVO, projectTask);
        projectTask.setId(taskReqVO.getTaskId());
        projectTask.setProjectId(taskReqVO.getProjectId());
        // 设置执行人昵称并更新数据库
        projectTask.setAssignTo(getExecutorNickName(taskReqVO.getUserId()));
        projectTask.setUpdatedTime(new Date());
        projectTaskMapper.updateById(projectTask);

        // 4. 更新任务成员（处理执行人变更逻辑）
        LambdaQueryWrapper<ProjectMember> qw = new LambdaQueryWrapper<>();
        qw.eq(ProjectMember::getPtId, taskReqVO.getTaskId()).eq(ProjectMember::getType, ProjectStatusEnum.TASK.getStatusName());
        List<ProjectMember> projectMembers = projectMemberMapper.selectList(qw);

        // 情况 A：任务当前只有一名成员（通常是创建者自己执行，或之前指派了一个人）
        if (projectMembers.size() == 1) {
            // 如果指派的人变了，则新增一名任务成员
            if (!Objects.equals(taskReqVO.getUserId(), projectMembers.get(0).getUserId())) {
                ProjectMember projectMember = new ProjectMember();
                projectMember.setPtId(taskReqVO.getTaskId());
                projectMember.setType(ProjectStatusEnum.TASK.getStatusName());
                projectMember.setJoinedTime(new Date());
                projectMember.setUserId(taskReqVO.getUserId());
                projectMember.setCreatedBy(SecurityUtils.getUsername());
                projectMember.setCreatedTime(new Date());
                projectMember.setUpdatedBy(SecurityUtils.getUsername());
                projectMember.setUpdatedTime(new Date());
                projectMemberMapper.insert(projectMember);
            }
        }
        // 情况 B：任务已有两名成员（通常是创建者 + 执行人）
        else if (projectMembers.size() == 2) {
            Map<Long, List<ProjectMember>> map = projectMembers.stream().collect(Collectors.groupingBy(ProjectMember::getUserId));
            List<ProjectMember> pms = map.get(taskReqVO.getUserId());
            // 如果新指派的用户不在当前成员列表中，则替换掉原有的非创建者成员
            if (CollectionUtils.isEmpty(pms)) {
                // 更新非创建者（creator=0）的那条成员记录
                LambdaQueryWrapper<ProjectMember> lqw = new LambdaQueryWrapper<>();
                lqw.eq(ProjectMember::getPtId, taskReqVO.getTaskId()).eq(ProjectMember::getCreator, 0);
                ProjectMember projectMember = projectMemberMapper.selectOne(lqw);
                projectMember.setUserId(taskReqVO.getUserId());
                projectMember.setUpdatedBy(SecurityUtils.getUsername());
                projectMember.setUpdatedTime(new Date());
                projectMember.setJoinedTime(new Date());
                projectMemberMapper.updateById(projectMember);
            } else {
                // 如果新指派的用户已经是成员，且它是创建者，则删除另一个非创建者成员记录（变回一人执行模式）
                if (pms.get(0).getCreator() == 1) {
                    LambdaQueryWrapper<ProjectMember> lqw = new LambdaQueryWrapper<>();
                    lqw.eq(ProjectMember::getPtId, taskReqVO.getTaskId()).eq(ProjectMember::getCreator, 0);
                    projectMemberMapper.delete(lqw);
                }
            }
        }

        // 5. 如果执行人发生变更，发送邮件通知
        if (!oldObj.getUserId().equals(taskReqVO.getUserId())) {
            extracted(taskReqVO.getTaskName(), taskReqVO.getUserId(), SecurityUtils.getUsername(), taskReqVO.getTaskId());
        }

        // 6. 记录操作日志（比对字段变更）
        ProjectTask newObj = projectTaskMapper.selectById(taskReqVO.getTaskId());
        // 获取对象属性变动详情
        List<LogDataVO> data = FieldUtils.getChangedFields(newObj, oldObj);
        data.forEach(a -> {
            LogVO lv = new LogVO();
            lv.setLogType(LogTypeEnum.TRENDS.getStatus());
            lv.setOperateType("editTask");
            lv.setType(ProjectStatusEnum.TASK.getStatusName());
            lv.setPtId(projectTask.getId());
            lv.setProjectId(projectTask.getProjectId());
            lv.setUserId(SecurityUtils.getUserId());
            lv.setRemark(a.getRemark());

            // 转换变更内容的显示值（如用户ID转昵称，状态码转文字说明）
            List<LogContentVO> logContentVOList = a.getLogContentVOList();
            logContentVOList.forEach(logContentVO -> {
                switch (logContentVO.getField()) {
                    case "userId":
                        logContentVO.setOldValue(getSysUserList(Collections.singletonList(Long.valueOf(logContentVO.getOldValue()))).get(0).getNickName());
                        logContentVO.setNewValue(getSysUserList(Collections.singletonList(Long.valueOf(logContentVO.getNewValue()))).get(0).getNickName());
                        break;
                    case "status":
                    case "executeStatus":
                        logContentVO.setOldValue(ProjectTaskStatusEnum.getStatusNameByStatus(Integer.parseInt(logContentVO.getOldValue())));
                        logContentVO.setNewValue(ProjectTaskStatusEnum.getStatusNameByStatus(Integer.parseInt(logContentVO.getNewValue())));
                        break;
                    case "taskPriority":
                        logContentVO.setOldValue(ProjectTaskPriorityEnum.getStatusNameByStatus(Integer.parseInt(logContentVO.getOldValue())));
                        logContentVO.setNewValue(ProjectTaskPriorityEnum.getStatusNameByStatus(Integer.parseInt(logContentVO.getNewValue())));
                        break;
                }
            });
            lv.setContent(JSON.toJSONString(logContentVOList));
            lv.setCreatedBy(SecurityUtils.getUsername());
            lv.setCreatedTime(new Date());
            lv.setUpdatedBy(SecurityUtils.getUsername());
            lv.setUpdatedTime(new Date());
            projectLogService.run(lv);

            // 7. 任务完成度特殊处理：如果状态变更为已完成，进度自动设为100%
            if (ProjectTaskStatusEnum.FINISHED.getStatus().equals(taskReqVO.getStatus())) {
                projectTask.setTaskProcess(new BigDecimal("100"));
            }
            projectTaskMapper.updateById(projectTask);
        });
    }

    /**
     * 验证任务时间的逻辑关系
     * 确保：开始时间 <= 结束时间 <= 截止时间
     *
     * @param beginTime 预计开始时间
     * @param endTime 预计完成时间
     * @param closeTime 截止时间
     * @throws ServiceException 如果时间逻辑不正确
     */
    private void validateTaskTime(Date beginTime, Date endTime, Date closeTime) {
        // 验证：开始时间不能晚于结束时间
        if (Objects.nonNull(beginTime) && Objects.nonNull(endTime) && beginTime.after(endTime)) {
            throw new ServiceException("预计开始日期不能晚于预计完成日期");
        }
        // 验证：结束时间不能晚于截止时间
        if (Objects.nonNull(endTime) && Objects.nonNull(closeTime) && endTime.after(closeTime)) {
            throw new ServiceException("预计完成日期不能晚于截止日期");
        }
    }

    @Override
    public List<TaskResVO> queryChildTask(TaskReqVO taskReqVO) {
        List<TaskResVO> taskResVOList = projectTaskMapper.queryChildTask(taskReqVO.getTaskId());
        taskResVOList.forEach(detail -> {
            detail.setStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(detail.getStatus()));
            detail.setExecuteStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(detail.getExecuteStatus()));
            String createdBy = "";
            if (detail.getUserId() != null) {
                createdBy = getSysUserList(Collections.singletonList(detail.getUserId())).get(0).getNickName();
                detail.setExecutor(createdBy);
            }
            detail.setCreatedBy(createdBy);
            detail.setTaskPriorityName(ProjectTaskPriorityEnum.getStatusNameByStatus(detail.getTaskPriority()));
        });
        return taskResVOList;
    }

    @Override
    public List<BurnDownChartVO> burnDownChart(ProjectVO projectVO) {
        List<BurnDownChartVO> list = new ArrayList<>(10);
        LambdaQueryWrapper<ProjectTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTask::getProjectId, projectVO.getProjectId()).orderByAsc(ProjectTask::getCreatedTime);
        List<ProjectTask> projectTasks = projectTaskMapper.selectList(queryWrapper);

        if (CollectionUtils.isNotEmpty(projectTasks)) {
            Date createdTime = projectTasks.get(0).getCreatedTime();
            String beginDate = DateUtils.dateTime(createdTime);
            String endDate = DateUtils.dateTime(new Date());
            List<String> betweenDate = DateUtils.getBetweenDate(beginDate, endDate);
            betweenDate.forEach(date -> {
                LocalDate now = LocalDate.parse(date, DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD)).plusDays(1);
                BurnDownChartVO burnDownChartVO = new BurnDownChartVO();
                burnDownChartVO.setDate(date);
                LambdaQueryWrapper<ProjectTask> qw = new LambdaQueryWrapper<>();
                qw.eq(ProjectTask::getProjectId, projectVO.getProjectId()).lt(ProjectTask::getCreatedTime, now);
                List<ProjectTask> projectTasks2 = projectTaskMapper.selectList(qw);
                burnDownChartVO.setTaskNum(projectTasks2.size());
                burnDownChartVO.setUnDoneTaskNum((int) projectTasks2.stream().filter(a -> !Objects.equals(a.getStatus(), ProjectTaskStatusEnum.FINISHED.getStatus())).count());
                burnDownChartVO.setBaseLineNum((int) projectTasks2.stream().filter(a -> !Objects.equals(a.getStatus(), ProjectTaskStatusEnum.FINISHED.getStatus())).filter(o -> {
                    if (o.getEndTime() == null) {
                        if (o.getCreatedTime() != null) {
                            Instant instant = o.getCreatedTime().toInstant();
                            ZoneId zoneId = ZoneId.systemDefault();
                            LocalDate create = instant.atZone(zoneId).toLocalDate();
                            return create.plusDays(5).isAfter(now);
                        }
                        return true;
                    } else {
                        Instant instant = o.getEndTime().toInstant();
                        ZoneId zoneId = ZoneId.systemDefault();
                        LocalDate end = instant.atZone(zoneId).toLocalDate();
                        return end.plusDays(-1).isBefore(now);
                    }
                }).count());
                list.add(burnDownChartVO);
            });
        }
        return list;
    }

    @Override
    public List<ProjectMemberResVO> queryUserList(ProjectTaskReqVO projectTaskReqVO) {
        List<ProjectMemberResVO> list = projectMemberMapper.queryTaskUserList(projectTaskReqVO.getTaskId());
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 拿到userids
        List<Long> userIds = list.stream().map(ProjectMemberResVO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        SysUserDTO sysUserDTO = new SysUserDTO();
        sysUserDTO.setUserIds(userIds);
        R<List<SysUserVO>> userResult = userFeignService.listOfInner(sysUserDTO, SecurityConstants.INNER);

        if (Objects.isNull(userResult) || CollectionUtils.isEmpty(userResult.getData())) {
            throw new ServiceException("远程调用查询用户列表：" + userIds + " 失败");
        }
        List<SysUserVO> userVOList = userResult.getData();

        // 匹配设置值
        Map<Long, SysUserVO> userMap = userVOList.stream().collect(Collectors.toMap(SysUserVO::getUserId, a -> a));
        list.forEach(projectMemberResVO -> {
            SysUserVO sysUserVO = userMap.get(projectMemberResVO.getUserId());
            if (Objects.nonNull(sysUserVO)) {
                projectMemberResVO.setUserName(sysUserVO.getUserName());
                projectMemberResVO.setNickName(sysUserVO.getNickName());
            }
        });
        return list;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComment(TaskCommentVO taskCommentVO) {
        ProjectLog projectLog = new ProjectLog();
        projectLog.setProjectId(taskCommentVO.getProjectId());
        projectLog.setOperateType("comment");
        projectLog.setUserId(SecurityUtils.getUserId());
        projectLog.setRemark("添加了评论");
        projectLog.setContent(taskCommentVO.getComment());
        projectLog.setLogType(LogTypeEnum.COMMENT.getStatus());
        projectLog.setPtId(taskCommentVO.getTaskId());
        projectLog.setType(ProjectStatusEnum.TASK.getStatusName());
        projectLog.setCreatedBy(SecurityUtils.getUsername());
        projectLog.setCreatedTime(new Date());
        projectLog.setUpdatedBy(SecurityUtils.getUsername());
        projectLog.setUpdatedTime(new Date());
        projectLogService.save(projectLog);
    }

    /**
     *
     * @param logReqVO
     * @return
     */
    @Override
    public List<ProjectLogVO> queryTaskLogList(LogReqVO logReqVO) {
        PageHelper.startPage(logReqVO.getPageNum(), logReqVO.getPageSize());
        return queryTaskLogFactory.execute(logReqVO.getLogType(), logReqVO.getTaskId());
    }

    @Override
    public void downloadTemplate(String taskId, HttpServletResponse response) throws IOException {

        // 根据 taskId 查询最新的模板
        LambdaQueryWrapper<ProjectFile> lw = new LambdaQueryWrapper<>();
        lw.eq(ProjectFile::getPtId, taskId).eq(ProjectFile::getType, ProjectStatusEnum.TEMPLATE.getStatusName()).orderByDesc(ProjectFile::getCreatedTime);
        List<ProjectFile> projectFiles = projectFileMapper.selectList(lw);
        if (CollectionUtils.isEmpty(projectFiles)) {
            throw new ServerException("不存在模板文件，请上传之后再下载");
        } else {
            String filePath = projectFiles.get(0).getPathName();
            String fileUrl = projectFiles.get(0).getFileUrl();
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, fileUrl.substring(fileUrl.lastIndexOf("/") + 1));
            FileUtils.writeBytes(filePath, response);
        }

    }

    @Override
    public List<TaskExportVO> exportAll() {
        List<TaskExportVO> list = projectTaskMapper.exportAll(SecurityUtils.getUserId());
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 拿到userids
        List<Long> userIds = list.stream().map(TaskExportVO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        SysUserDTO sysUserDTO = new SysUserDTO();
        sysUserDTO.setUserIds(userIds);
        R<List<SysUserVO>> userResult = userFeignService.listOfInner(sysUserDTO, SecurityConstants.INNER);

        if (Objects.isNull(userResult) || CollectionUtils.isEmpty(userResult.getData())) {
            throw new ServiceException("远程调用查询用户列表：" + userIds + " 失败");
        }
        List<SysUserVO> userVOList = userResult.getData();

        // 匹配设置值
        Map<Long, SysUserVO> userMap = userVOList.stream().collect(Collectors.toMap(SysUserVO::getUserId, a -> a));
        list.forEach(a -> {
            a.setExecuteStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getExecuteStatus()));
            a.setStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getStatus()));
            a.setTaskPriorityName(ProjectTaskPriorityEnum.getStatusNameByStatus(a.getTaskPriority()));

            // 设置用户信息
            SysUserVO sysUserVO = userMap.get(a.getUserId());
            if (Objects.nonNull(sysUserVO)) {
                a.setExecutor(sysUserVO.getNickName());
                a.setCreatedBy(sysUserVO.getNickName());
            }
        });
        return list;
    }

    @Override
    public List<TaskExportVO> export(String taskIds) {
        List<String> taskIdList = Arrays.asList(taskIds.split(","));
        List<TaskExportVO> list = projectTaskMapper.export(taskIdList);

        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 拿到userids
        List<Long> userIds = list.stream().map(TaskExportVO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        SysUserDTO sysUserDTO = new SysUserDTO();
        sysUserDTO.setUserIds(userIds);
        R<List<SysUserVO>> userResult = userFeignService.listOfInner(sysUserDTO, SecurityConstants.INNER);

        if (Objects.isNull(userResult) || CollectionUtils.isEmpty(userResult.getData())) {
            throw new ServiceException("远程调用查询用户列表：" + userIds + " 失败");
        }
        List<SysUserVO> userVOList = userResult.getData();

        // 匹配设置值
        Map<Long, SysUserVO> userMap = userVOList.stream().collect(Collectors.toMap(SysUserVO::getUserId, a -> a));
        list.forEach(a -> {
            a.setExecuteStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getExecuteStatus()));
            a.setStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getStatus()));
            a.setTaskPriorityName(ProjectTaskPriorityEnum.getStatusNameByStatus(a.getTaskPriority()));

            // 设置用户信息
            SysUserVO sysUserVO = userMap.get(a.getUserId());
            if (Objects.nonNull(sysUserVO)) {
                a.setExecutor(sysUserVO.getNickName());
                a.setCreatedBy(sysUserVO.getNickName());
            }
        });

        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importTask(List<TaskExcelVO> taskList) {
        if (CollectionUtils.isEmpty(taskList)) {
            throw new ServiceException("导入任务数据不能为空");
        }
        // todo 后期优化成批量查询，性能优化
        taskList.forEach(task -> {
            // 查询用户信息
            R<LoginUser> userResult = userFeignService.info(task.getUsername(), SecurityConstants.INNER);

            if (Objects.isNull(userResult) || Objects.isNull(userResult.getData())) {
                throw new ServiceException("登录用户：" + task.getUsername() + " 不存在");
            }

            LoginUser loginUser = userResult.getData();
            if (Objects.isNull(loginUser)) {
                return;
            }
            SysUser sysUser =  loginUser.getUser();

            ProjectTask projectTask = new ProjectTask();
            projectTask.setTaskName(task.getTaskName());
            projectTask.setBeginTime(DateUtils.parseDate(task.getBeginTime()));
            projectTask.setEndTime(DateUtils.parseDate(task.getEndTime()));
            projectTask.setCloseTime(DateUtils.parseDate(task.getCloseTime()));
            projectTask.setTaskPriority(Integer.valueOf(task.getTaskPriority()));
            LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<>();
            qw.eq(Project::getProjectCode, task.getProjectCode());
            String projectId = projectMapper.selectOne(qw).getId();
            if (StringUtils.isBlank(projectId)) {
                return;
            }
            // 根据项目id查询成员
            LambdaQueryWrapper<ProjectMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ProjectMember::getPtId, projectId).eq(ProjectMember::getType, ProjectStatusEnum.PROJECT.getStatusName());
            List<ProjectMember> projectMembers = projectMemberMapper.selectList(queryWrapper);
            List<Long> userIds = projectMembers.stream().map(ProjectMember::getUserId).collect(Collectors.toList());
            if (!userIds.contains(sysUser.getUserId())) {
                return;
            }
            projectTask.setProjectId(projectId);
            LambdaQueryWrapper<ProjectStage> qw2 = new LambdaQueryWrapper<>();
            qw2.eq(ProjectStage::getProjectId, projectId).orderByAsc(ProjectStage::getStageCode);
            projectTask.setProjectStageId(projectStageMapper.selectList(qw2).get(0).getId());
            projectTask.setUserId(sysUser.getUserId());
            projectTask.setAssignTo(sysUser.getNickName());
            projectTask.setCreatedBy(SecurityUtils.getUsername());
            projectTask.setCreatedTime(new Date());
            projectTask.setUpdatedBy(SecurityUtils.getUsername());
            projectTask.setUpdatedTime(new Date());
            projectTaskMapper.insert(projectTask);
            insertMember(projectTask.getId(), 1, SecurityUtils.getUserId());
            // 添加日志
            saveLog("importTask", projectTask.getId(), projectTask.getProjectId(), projectTask.getTaskName()
                    , "导入了任务", null);
            // 将执行人加入
            if (projectTask.getUserId() != null && !Objects.equals(projectTask.getUserId(), SecurityUtils.getUserId())) {
                insertMember(projectTask.getId(), 0, projectTask.getUserId());
                // 添加日志
                saveLog("invitePartakeTask", projectTask.getId(), projectTask.getProjectId(), projectTask.getTaskName()
                        ,"邀请 " + getSysUserList(Collections.singletonList(projectTask.getUserId())).get(0).getNickName() + " 参与任务"
                        , projectTask.getUserId());
            }
        });
    }

    /**
     * 插入任务成员
     * 将用户添加为任务成员，并标记是否为创建者
     *
     * @param taskId 任务ID
     * @param creator 是否为创建者（1-是，0-否）
     * @param userId 用户ID
     */
    void insertMember(String taskId, Integer creator, Long userId) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.setPtId(taskId);
        projectMember.setType(ProjectStatusEnum.TASK.getStatusName()); // 类型：任务
        projectMember.setJoinedTime(new Date());
        projectMember.setUserId(userId);
        projectMember.setCreatedBy(SecurityUtils.getUsername());
        projectMember.setCreatedTime(new Date());
        projectMember.setUpdatedBy(SecurityUtils.getUsername());
        projectMember.setUpdatedTime(new Date());
        projectMember.setCreator(creator); // 标记是否为创建者
        projectMemberMapper.insert(projectMember);
    }

    /**
     * 保存任务操作日志
     * 记录任务的各类操作，如创建、编辑、评论等
     *
     * @param operateType 操作类型，如：addTask、editTask、comment等
     * @param taskId 任务ID
     * @param projectId 项目ID
     * @param taskName 任务名称
     * @param remark 备注信息
     * @param userId 被操作的用户ID（可选）
     */
    void saveLog(String operateType, String taskId, String projectId, String taskName, String remark, Long userId) {
        LogVO logVO = new LogVO();
        logVO.setLogType(LogTypeEnum.TRENDS.getStatus());
        logVO.setOperateType(operateType);
        logVO.setType(ProjectStatusEnum.TASK.getStatusName());
        logVO.setPtId(taskId);
        logVO.setProjectId(projectId);
        logVO.setUserId(SecurityUtils.getUserId());
        if (userId != null) {
            logVO.setToUserId(userId);
        }
        logVO.setRemark(remark);
        logVO.setContent(taskName);
        logVO.setCreatedBy(SecurityUtils.getUsername());
        logVO.setCreatedTime(new Date());
        logVO.setUpdatedBy(SecurityUtils.getUsername());
        logVO.setUpdatedTime(new Date());
        projectLogService.run(logVO);
    }

    @Override
    public void downloadTaskTemplate(HttpServletResponse response) throws IOException {
        String filePath = PmhubConfig.getProfile() + "/template/taskTemplate.xlsx";
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        FileUtils.setAttachmentResponseHeader(response, "任务模板.xlsx");
        FileUtils.writeBytes(filePath, response);
    }

    @Override
    public PageInfo<TaskResVO> taskList(TaskReqVO taskReqVO) {
        PageHelper.startPage(taskReqVO.getPageNum(), taskReqVO.getPageSize());
        List<TaskResVO> list = projectTaskMapper.taskList(taskReqVO);
        if (CollectionUtils.isEmpty(list)) {
            return new PageInfo<>(list);
        }
        list.forEach(a -> {
            a.setTaskPriorityName(ProjectTaskPriorityEnum.getStatusNameByStatus(a.getTaskPriority()));
            a.setStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getStatus()));
            a.setExecuteStatusName(ProjectTaskStatusEnum.getStatusNameByStatus(a.getExecuteStatus()));
            if (a.getEndTime() != null && a.getBeginTime() != null) {
                a.setPeriod(DateUtils.differentDaysByMillisecond(a.getEndTime(), a.getBeginTime()));
            }
            WorkFlowable workFlowable = new WorkFlowable();
            workFlowable.setTaskId(a.getTaskProcessId());
            workFlowable.setApproved(a.getApproved());
            workFlowable.setDeploymentId(a.getDeployId());
            workFlowable.setProcInsId(a.getProcInsId());
            workFlowable.setDefinitionId(a.getDefinitionId());
            a.setWorkFlowable(workFlowable);
        });
        fillExecutorInfo(list);
        return new PageInfo<>(list);
    }

    @Override
    public Long countTaskNum() {
        LambdaQueryWrapper<ProjectTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTask::getDeleted, 0);
        if (projectTaskMapper.selectCount(queryWrapper) == null) {
            return 0L;
        }
        return projectTaskMapper.selectCount(queryWrapper);
    }

    @Override
    public List<Project> queryProjectsStatus(List<String> projectIds) {
        return projectTaskMapper.queryProjectsStatus(projectIds);
    }

    @Override
    public List<ProjectTaskProcess> taskProcessList(List<String> taskIds) {
        LambdaQueryWrapper<ProjectTaskProcess> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(ProjectTaskProcess::getExtraId, taskIds);
        return projectTaskProcessMapper.selectList(queryWrapper);
    }

}
