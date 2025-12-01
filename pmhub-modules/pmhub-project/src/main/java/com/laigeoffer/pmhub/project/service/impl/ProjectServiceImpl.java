package com.laigeoffer.pmhub.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.laigeoffer.pmhub.api.system.UserFeignService;
import com.laigeoffer.pmhub.api.system.domain.dto.SysUserDTO;
import com.laigeoffer.pmhub.base.core.constant.SecurityConstants;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.entity.SysUser;
import com.laigeoffer.pmhub.base.core.core.domain.model.LoginUser;
import com.laigeoffer.pmhub.base.core.core.domain.vo.SysUserVO;
import com.laigeoffer.pmhub.base.core.enums.LogTypeEnum;
import com.laigeoffer.pmhub.base.core.enums.ProjectStageEnum;
import com.laigeoffer.pmhub.base.core.enums.ProjectStatusEnum;
import com.laigeoffer.pmhub.base.core.exception.ServiceException;
import com.laigeoffer.pmhub.base.core.utils.DateUtils;
import com.laigeoffer.pmhub.base.core.utils.uuid.Seq;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.project.domain.*;
import com.laigeoffer.pmhub.project.domain.vo.project.*;
import com.laigeoffer.pmhub.project.domain.vo.project.log.LogVO;
import com.laigeoffer.pmhub.project.domain.vo.project.task.TaskStatisticsByDateVO;
import com.laigeoffer.pmhub.project.mapper.*;
import com.laigeoffer.pmhub.project.service.ProjectLogService;
import com.laigeoffer.pmhub.project.service.ProjectService;
import com.laigeoffer.pmhub.project.service.project.QueryProjectFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目服务实现类
 * 负责项目相关的核心业务逻辑处理，包括：
 * 1. 项目的增删改查
 * 2. 项目状态管理（归档、取消归档等）
 * 3. 项目成员管理
 * 4. 项目日志记录
 * 5. 项目统计和排行
 *
 * @author chenqingtong
 * @date 2024-12-13 10:08
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {
    
    /** 项目数据访问层 */
    @Autowired
    private ProjectMapper projectMapper;
    
    /** 项目任务数据访问层 */
    @Autowired
    private ProjectTaskMapper projectTaskMapper;
    
    /** 项目成员数据访问层 */
    @Autowired
    private ProjectMemberMapper projectMemberMapper;
    
    /** 项目阶段数据访问层 */
    @Autowired
    private ProjectStageMapper projectStageMapper;
    
    /** 项目日志服务 */
    @Autowired
    private ProjectLogService projectLogService;
    
    /** 项目收藏数据访问层 */
    @Autowired
    private ProjectCollectionMapper projectCollectionMapper;
    
    /** 项目查询工厂，用于根据不同类型查询项目 */
    @Autowired
    private QueryProjectFactory queryProjectFactory;

    /** 用户服务远程调用接口（Feign） */
    @Resource
    private UserFeignService userFeignService;

    /** 项目发布状态常量 */
    private final String NO_PUBLISHED_NAME = "未发布";
    private final String PUBLISHED_NAME = "已发布";
    
    /** 项目类型常量 */
    private final String PUBLIC = "公开项目";
    private final String PRIVATE = "私有项目";

    /**
     * 查询项目进度排行列表
     * 按照项目进度从高到低排序，返回前10个项目
     *
     * @return 项目排行列表，按进度降序排列
     */
    @Override
    public List<ProjectRankVO> queryProjectRankList() {
        List<ProjectRankVO> projectRankVOList = new ArrayList<>(10);
        // 获取当前登录用户信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        
        // 构建查询条件：查询未删除的项目
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Project::getDeleted, 0);
        List<Project> list = projectMapper.selectList(queryWrapper);
        
        // 如果项目列表为空，直接返回空列表
        if (CollectionUtils.isEmpty(list)) {
            return projectRankVOList;
        }
        
        // 对项目按进度降序排序
        List<Project> collect = list.stream()
                .sorted(Comparator.comparing(Project::getProjectProcess).reversed())
                .collect(Collectors.toList());
        
        // 转换为排行VO对象
        collect.forEach(project -> {
            ProjectRankVO projectRankVO = new ProjectRankVO();
            projectRankVO.setProjectId(project.getId());
            projectRankVO.setProjectName(project.getProjectName());
            projectRankVO.setProcess(project.getProjectProcess());
            projectRankVO.setUserName(loginUser.getUsername());
            projectRankVO.setNickName(loginUser.getUser().getNickName());
            projectRankVOList.add(projectRankVO);
        });
        return projectRankVOList;
    }

    /**
     * 查询与我有关的项目列表
     * 包括我创建的、我参与的、我收藏的项目
     *
     * @return 项目列表，已设置状态名称
     */
    @Override
    public List<ProjectVO> queryMyProjectList() {
        // 根据当前用户ID查询相关项目
        List<ProjectVO> projects = projectMapper.queryMyProjectList(SecurityUtils.getUserId());
        
        // 为每个项目设置状态名称（将状态码转换为状态文本）
        projects.forEach(project -> {
            project.setStatusName(ProjectStatusEnum.getStatusNameByStatus(project.getStatus()));
        });
        return projects;
    }

    /**
     * 删除项目（逻辑删除）
     * 删除前会检查项目下是否存在未删除的任务，如果存在则不允许删除
     * 删除操作会记录项目日志
     *
     * @param projectVO 项目视图对象，包含项目ID
     * @return 影响的行数
     * @throws ServiceException 如果项目下存在任务，则抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteProject(ProjectVO projectVO) {
        // 查询项目信息
        Project project = projectMapper.selectById(projectVO.getProjectId());
        
        // 检查项目下是否存在未删除的任务
        LambdaQueryWrapper<ProjectTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTask::getProjectId, project.getId())
                .eq(ProjectTask::getDeleted, 0);
        boolean exist = projectTaskMapper.exists(queryWrapper);
        
        // 如果存在任务，不允许删除项目
        if (exist) {
            throw new ServiceException("该项目下存在任务不允许删除");
        }
        
        // 执行逻辑删除：设置删除标志和时间
        project.setDeleted(1);
        project.setDeletedTime(new Date());
        project.setUpdatedBy(SecurityUtils.getUsername());
        project.setUpdatedTime(new Date());
        int i = projectMapper.updateById(project);
        
        // 记录项目删除日志
        LogVO logVO = new LogVO();
        logVO.setLogType(LogTypeEnum.TRENDS.getStatus()); // 日志类型：动态
        logVO.setOperateType("delete"); // 操作类型：删除
        logVO.setType(ProjectStatusEnum.PROJECT.getStatusName()); // 类型：项目
        logVO.setPtId(project.getId());
        logVO.setProjectId(project.getId());
        logVO.setUserId(SecurityUtils.getUserId());
        logVO.setCreatedBy(SecurityUtils.getUsername());
        logVO.setCreatedTime(new Date());
        logVO.setUpdatedBy(SecurityUtils.getUsername());
        logVO.setUpdatedTime(new Date());
        projectLogService.run(logVO);
        
        return i;
    }

    /**
     * 查询项目详情
     * 包括项目基本信息、状态、类型、发布状态、收藏状态等
     * 对于私有项目，会检查当前用户是否为项目成员
     *
     * @param projectId 项目ID
     * @return 项目详情视图对象
     * @throws ServiceException 如果项目为私有且用户不是成员，或用户信息查询失败
     */
    @Override
    public ProjectResVO detail(String projectId) {
        // 查询项目基本信息
        ProjectResVO detail = projectMapper.detail(projectId);
        
        // 如果项目有前缀编码，则使用前缀作为项目编码
        if (StringUtils.isNotBlank(detail.getPrefix())) {
            detail.setProjectCode(detail.getPrefix());
        }
        
        // 设置项目状态名称
        detail.setStatusName(ProjectStatusEnum.getStatusNameByStatus(detail.getStatus()));
        
        // 私有项目权限检查：如果项目类型为私有（1），需要验证当前用户是否为项目成员
        if (detail.getProjectType() == 1) {
            Long userId = SecurityUtils.getUserId();
            LambdaQueryWrapper<ProjectMember> qw = new LambdaQueryWrapper<>();
            qw.eq(ProjectMember::getUserId, userId)
                    .eq(ProjectMember::getType, ProjectStatusEnum.PROJECT.getStatusName())
                    .eq(ProjectMember::getPtId, projectId);
            List<ProjectMember> projectMembers = projectMemberMapper.selectList(qw);
            
            // 如果不是项目成员，不允许查看
            if(CollectionUtils.isEmpty(projectMembers)) {
                throw new ServiceException("该项目为私有项目，你不是项目成员无法查看");
            }
        }
        
        // 设置项目类型名称（0-公开，1-私有）
        detail.setProjectTypeName(detail.getProjectType() == 0 ? PUBLIC : PRIVATE);
        
        // 查询当前用户是否收藏了该项目
        LambdaQueryWrapper<ProjectCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectCollection::getUserId, detail.getUserId())
                .eq(ProjectCollection::getProjectId, detail.getProjectId());
        ProjectCollection projectCollection = projectCollectionMapper.selectOne(queryWrapper);
        detail.setCollected(projectCollection != null);
        
        // 设置发布状态名称（0-未发布，1-已发布）
        detail.setPublishedName(detail.getPublished() == 0 ? NO_PUBLISHED_NAME : PUBLISHED_NAME);

        // 通过Feign远程调用用户服务，查询项目负责人的用户信息
        R<LoginUser> userResult = userFeignService.getInfoByUserId(detail.getUserId(), SecurityConstants.INNER);

        // 验证远程调用结果
        if (Objects.isNull(userResult) || Objects.isNull(userResult.getData())) {
            throw new ServiceException("远程调用查询用户：" + detail.getUserId() + " 不存在");
        }
        
        // 设置负责人昵称
        LoginUser userInfo = userResult.getData();
        detail.setNickName(userInfo.getNickName());
        
        return detail;
    }

    @Override
    public List<DoingProjectVO> queryDoingProject() {
        List<DoingProjectVO> list = new ArrayList<>(10);
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Project::getStatus, ProjectStatusEnum.DOING.getStatus()).eq(Project::getDeleted, 0)
                .orderByDesc(Project::getProjectProcess);
        List<Project> projects = projectMapper.selectList(queryWrapper);

        if (CollectionUtils.isNotEmpty(projects)) {
            // 根据 userIds 查询用户列表
            List<Long> userIds = projects.stream().map(Project::getUserId).distinct().collect(Collectors.toList());
            SysUserDTO sysUserDTO = new SysUserDTO();
            sysUserDTO.setUserIds(userIds);
            R<List<SysUserVO>> userResult = userFeignService.listOfInner(sysUserDTO, SecurityConstants.INNER);

            if (Objects.isNull(userResult) || CollectionUtils.isEmpty(userResult.getData())) {
                throw new ServiceException("远程调用查询用户列表：" + userIds + " 失败");
            }
            List<SysUserVO> userVOList = userResult.getData();
            List<SysUser> sysUsers = userVOList.stream()
                    .map(userVO -> (SysUser) userVO)
                    .collect(Collectors.toList());
            Map<Long, List<SysUser>> map = sysUsers.stream().collect(Collectors.groupingBy(SysUser::getUserId));
            projects.forEach(a -> {
                DoingProjectVO doingProjectVO = new DoingProjectVO();
                doingProjectVO.setProjectId(a.getId());
                doingProjectVO.setProjectName(a.getProjectName());
                doingProjectVO.setCover(a.getCover());
                doingProjectVO.setProcess(a.getProjectProcess());
                doingProjectVO.setUserId(a.getUserId());
                doingProjectVO.setNickName(map.get(a.getUserId()).get(0).getNickName());
                list.add(doingProjectVO);
            });

        }
        return list;
    }

    /**
     * 保存新项目
     * 创建项目时会自动：
     * 1. 生成项目编码
     * 2. 创建所有项目阶段
     * 3. 将创建者添加为项目成员
     * 4. 记录项目创建日志
     *
     * @param project 项目实体对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProject(Project project) {
        // 1. 生成项目编码：P + 序列号
        project.setProjectCode("P" + Seq.getId());
        project.setUserId(SecurityUtils.getUserId());
        project.setCreatedBy(SecurityUtils.getUsername());
        project.setCreatedTime(new Date());
        project.setUpdatedBy(SecurityUtils.getUsername());
        project.setUpdatedTime(new Date());
        projectMapper.insert(project);
        
        // 2. 为项目创建所有阶段（从枚举中获取所有阶段）
        for (ProjectStageEnum value : ProjectStageEnum.values()) {
            ProjectStage projectStage = new ProjectStage();
            projectStage.setProjectId(project.getId());
            projectStage.setStageCode(value.getStatus());
            projectStage.setStageName(value.getStatusName());
            projectStage.setCreatedBy(SecurityUtils.getUsername());
            projectStage.setCreatedTime(new Date());
            projectStage.setUpdatedBy(SecurityUtils.getUsername());
            projectStage.setUpdatedTime(new Date());
            projectStageMapper.insert(projectStage);
        }
        
        // 3. 设置项目的当前阶段为第一个阶段（STAGE_0）
        LambdaQueryWrapper<ProjectStage> qw = new LambdaQueryWrapper<>();
        qw.eq(ProjectStage::getProjectId, project.getId())
                .eq(ProjectStage::getStageCode, ProjectStageEnum.STAGE_0.getStatus());
        project.setProjectStageId(projectStageMapper.selectOne(qw).getId());
        projectMapper.updateById(project);
        
        // 4. 将项目创建者添加为项目成员，并标记为创建者
        ProjectMember projectMember = new ProjectMember();
        projectMember.setPtId(project.getId());
        projectMember.setType(ProjectStatusEnum.PROJECT.getStatusName());
        projectMember.setUserId(SecurityUtils.getUserId());
        projectMember.setJoinedTime(new Date());
        projectMember.setCreatedTime(new Date());
        projectMember.setCreatedBy(SecurityUtils.getUsername());
        projectMember.setUpdatedBy(SecurityUtils.getUsername());
        projectMember.setUpdatedTime(new Date());
        projectMember.setCreator(1); // 标记为创建者
        projectMemberMapper.insert(projectMember);
        
        // 5. 记录项目创建日志
        saveLog("create", project.getId(), null);
        
        // 6. 记录邀请成员日志（创建者自己）
        saveLog("inviteMember", project.getId(), SecurityUtils.getUserId());
    }

    /**
     * 保存项目操作日志
     * 用于记录项目的各种操作，如创建、编辑、删除、邀请成员等
     *
     * @param operateType 操作类型，如：create、edit、delete、inviteMember等
     * @param projectId 项目ID
     * @param userId 被操作的用户ID（可选，如邀请成员时需要）
     */
    public void saveLog(String operateType, String projectId, Long userId) {
        LogVO logVO = new LogVO();
        logVO.setLogType(LogTypeEnum.TRENDS.getStatus());
        logVO.setOperateType(operateType);
        logVO.setType(ProjectStatusEnum.PROJECT.getStatusName());
        logVO.setPtId(projectId);
        logVO.setProjectId(projectId);
        logVO.setUserId(SecurityUtils.getUserId());
        if (userId != null) {
            logVO.setToUserId(userId);
        }
        logVO.setCreatedBy(SecurityUtils.getUsername());
        logVO.setCreatedTime(new Date());
        logVO.setUpdatedBy(SecurityUtils.getUsername());
        logVO.setUpdatedTime(new Date());
        projectLogService.run(logVO);
    }

    /**
     * 分页查询项目列表
     * 根据查询条件（类型、状态、关键词等）查询项目，并补充用户信息、收藏状态等
     *
     * @param projectReqVO 项目查询请求对象，包含分页参数和查询条件
     * @return 分页结果，包含项目列表和分页信息
     */
    @Override
    public PageInfo<ProjectResVO> list(ProjectReqVO projectReqVO) {
        // 开启分页
        PageHelper.startPage(projectReqVO.getPageNum(), projectReqVO.getPageSize());
        
        // 通过工厂模式根据查询类型执行不同的查询策略
        List<ProjectResVO> list = queryProjectFactory.execute(projectReqVO);
        
        if (CollectionUtils.isNotEmpty(list)) {
            // 1. 提取所有项目负责人ID，用于批量查询用户信息
            List<Long> userIds = list.stream()
                    .map(ProjectResVO::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 2. 通过Feign远程调用用户服务，批量查询用户信息
            SysUserDTO sysUserDTO = new SysUserDTO();
            sysUserDTO.setUserIds(userIds);
            R<List<SysUserVO>> userResult = userFeignService.listOfInner(sysUserDTO, SecurityConstants.INNER);

            // 验证远程调用结果
            if (Objects.isNull(userResult) || CollectionUtils.isEmpty(userResult.getData())) {
                throw new ServiceException("远程调用查询用户列表：" + userIds + " 失败");
            }
            
            // 3. 将用户VO转换为用户实体，并按用户ID分组
            List<SysUserVO> userVOList = userResult.getData();
            List<SysUser> sysUsers = userVOList.stream()
                    .map(userVO -> (SysUser) userVO)
                    .collect(Collectors.toList());
            Map<Long, List<SysUser>> map = sysUsers.stream()
                    .collect(Collectors.groupingBy(SysUser::getUserId));

            // 4. 为每个项目补充详细信息
            list.forEach(a -> {
                // 设置项目编码（如果有前缀则使用前缀）
                if (StringUtils.isNotBlank(a.getPrefix())) {
                    a.setProjectCode(a.getPrefix());
                }
                
                // 设置项目状态名称
                a.setStatusName(ProjectStatusEnum.getStatusNameByStatus(a.getStatus()));
                
                // 设置发布状态名称
                a.setPublishedName(a.getPublished() == 0 ? NO_PUBLISHED_NAME : PUBLISHED_NAME);
                
                // 设置项目类型名称
                a.setProjectTypeName(a.getProjectType() == 0 ? PUBLIC : PRIVATE);
                
                // 设置负责人昵称
                a.setNickName(map.get(a.getUserId()).get(0).getNickName());
                
                // 查询当前用户是否收藏了该项目
                LambdaQueryWrapper<ProjectCollection> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(ProjectCollection::getUserId, SecurityUtils.getUserId())
                        .eq(ProjectCollection::getProjectId, a.getProjectId());
                ProjectCollection projectCollection = projectCollectionMapper.selectOne(queryWrapper);
                a.setCollected(projectCollection != null);
            });
        }

        return new PageInfo<>(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archived(String projectId) {
        // 项目是否已发布 未发布不能归档
        Project project = projectMapper.selectById(projectId);
        if (project.getPublished() == 0) {
            throw new ServiceException("项目未发布不允许归档");
        }
        LambdaUpdateChainWrapper<Project> luw = lambdaUpdate().eq(Project::getId, projectId);
        luw.set(Project::getArchived, 1).set(Project::getArchivedTime, new Date()).set(Project::getStatus, ProjectStatusEnum.ARCHIVED.getStatus());
        luw.update();
        // 添加项目日志
        LogVO logVO = new LogVO();
        logVO.setLogType(LogTypeEnum.TRENDS.getStatus());
        logVO.setOperateType("archive");
        logVO.setType(ProjectStatusEnum.PROJECT.getStatusName());
        logVO.setPtId(projectId);
        logVO.setProjectId(projectId);
        logVO.setUserId(SecurityUtils.getUserId());
        logVO.setCreatedBy(SecurityUtils.getUsername());
        logVO.setCreatedTime(new Date());
        logVO.setUpdatedBy(SecurityUtils.getUsername());
        logVO.setUpdatedTime(new Date());
        projectLogService.run(logVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelArchived(String projectId) {
        LambdaUpdateChainWrapper<Project> luw = lambdaUpdate().eq(Project::getId, projectId);
        luw.set(Project::getArchived, 0).set(Project::getArchivedTime, null).set(Project::getStatus, ProjectStatusEnum.DOING.getStatus());
        luw.update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quit(String projectId) {
        Project project = projectMapper.selectById(projectId);
        if (SecurityUtils.getUsername().equals(project.getCreatedBy())) {
            throw new ServiceException("项目创建人不能退出");
        }
        LambdaQueryWrapper<ProjectMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectMember::getPtId, projectId).eq(ProjectMember::getType, ProjectStatusEnum.PROJECT.getStatusName())
                .eq(ProjectMember::getUserId, SecurityUtils.getUserId());
        projectMemberMapper.delete(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editProject(Project project) {
        // 根据项目 id 和 stageCode 查询阶段
        LambdaQueryWrapper<ProjectStage> qw = new LambdaQueryWrapper<>();
        qw.eq(ProjectStage::getProjectId, project.getProjectId()).eq(ProjectStage::getStageCode, project.getStageCode());
        project.setProjectStageId(projectStageMapper.selectOne(qw).getId());
        project.setId(project.getProjectId());
        project.setType(project.getProjectType());
        if (StringUtils.isNotBlank(project.getPrefix())) {
            project.setProjectCode(project.getPrefix());
        }

        if (Objects.equals(project.getStatus(), ProjectStatusEnum.ARCHIVED.getStatus())) {
            if (project.getPublished() == 0) {
                throw new ServiceException("项目未发布不允许归档");
            }
            project.setArchived(1);
            project.setArchivedTime(new Date());
        }
        project.setUpdatedTime(new Date());
        projectMapper.updateById(project);
        // 添加项目日志
        LogVO logVO = new LogVO();
        logVO.setLogType(LogTypeEnum.TRENDS.getStatus());
        logVO.setOperateType("edit");
        logVO.setType(ProjectStatusEnum.PROJECT.getStatusName());
        logVO.setPtId(project.getId());
        logVO.setProjectId(project.getId());
        logVO.setUserId(SecurityUtils.getUserId());
        logVO.setCreatedBy(SecurityUtils.getUsername());
        logVO.setCreatedTime(new Date());
        logVO.setUpdatedBy(SecurityUtils.getUsername());
        logVO.setUpdatedTime(new Date());
        projectLogService.run(logVO);

    }

    @Override
    public List<TaskStatisticsByDateVO> taskStatisticsByDate(String projectId) {
        List<TaskStatisticsByDateVO> list = new ArrayList<>(10);
        LambdaQueryWrapper<ProjectTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectTask::getProjectId, projectId).orderByAsc(ProjectTask::getCreatedTime);
        List<ProjectTask> projectTasks = projectTaskMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(projectTasks)) {
            projectTasks.forEach(projectTask -> projectTask.setCreatedDate(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, projectTask.getCreatedTime())));
            Map<String, List<ProjectTask>> map = projectTasks.stream().collect(Collectors.groupingBy(ProjectTask::getCreatedDate));
            Date createdTime = projectTasks.get(0).getCreatedTime();
            String beginDate = DateUtils.dateTime(createdTime);
            String endDate = DateUtils.dateTime(new Date());
            List<String> betweenDate = DateUtils.getBetweenDate(beginDate, endDate);
            betweenDate.forEach(date -> {
                TaskStatisticsByDateVO statistics = new TaskStatisticsByDateVO();
                statistics.setDate(date);
                statistics.setTotal(CollectionUtils.isNotEmpty(map.get(date)) ? map.get(date).size() : 0);
                list.add(statistics);
            });
        }
        return list;
    }

    @Override
    public List<ProjectVO> queryAllProject() {
        List<ProjectVO> list = new ArrayList<>(10);
        // 查询未删除的项目
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<>();
        qw.eq(Project::getDeleted, 0);
        List<Project> projects = projectMapper.selectList(qw);
        if (CollectionUtils.isNotEmpty(projects)) {
            projects.forEach(a -> {
                ProjectVO projectVO = new ProjectVO();
                projectVO.setProjectId(a.getId());
                projectVO.setProjectName(a.getProjectName());
                projectVO.setStatus(a.getStatus());
                projectVO.setStatusName(ProjectStatusEnum.getStatusNameByStatus(a.getStatus()));
                list.add(projectVO);
            });

        }
        return list;
    }

    @Override
    public Long countProjectNum() {
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Project::getDeleted, 0);
        if (projectMapper.selectCount(queryWrapper) == null) {
            return 0L;
        }
        return  projectMapper.selectCount(queryWrapper);
    }

}
