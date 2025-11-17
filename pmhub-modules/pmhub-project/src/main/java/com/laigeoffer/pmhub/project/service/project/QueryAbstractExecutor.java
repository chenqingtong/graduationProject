package com.laigeoffer.pmhub.project.service.project;

import com.laigeoffer.pmhub.project.domain.vo.project.ProjectReqVO;
import com.laigeoffer.pmhub.project.domain.vo.project.ProjectResVO;

import java.util.List;

/**
 * @author chenqingtong
 * @date 2024-01-09 11:41
 */
public abstract class QueryAbstractExecutor {
    public abstract List<ProjectResVO> query(ProjectReqVO projectReqVO);
}
