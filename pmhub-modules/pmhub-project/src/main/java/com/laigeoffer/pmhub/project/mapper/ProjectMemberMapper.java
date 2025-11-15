package com.laigeoffer.pmhub.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laigeoffer.pmhub.project.domain.ProjectMember;
import com.laigeoffer.pmhub.project.domain.vo.project.member.ProjectMemberReqVO;
import com.laigeoffer.pmhub.project.domain.vo.project.member.ProjectMemberResVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author chenqingtong
 * @date 2024-12-12 14:29
 */
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

    List<ProjectMemberResVO> searchMember(@Param("data") ProjectMemberReqVO projectMemberReqVO);

    List<ProjectMemberResVO> queryExecutorList(@Param("projectId") String projectId);
    List<ProjectMemberResVO> queryTaskUserList(@Param("taskId") String taskId);
}
