package com.laigeoffer.pmhub.workflow.domain.vo;

import cn.hutool.core.util.ObjectUtil;
import com.laigeoffer.pmhub.workflow.core.FormConf;
import lombok.Data;

import java.util.List;

/**
 * 流程详情视图对象
 *
 * @author chenqingtong
 * @createTime 2024/8/7 15:01
 */
@Data
public class WfDetailVo {

    /**
     * 任务表单信息
     */
    private FormConf taskFormData;

    /**
     * 历史流程节点信息
     */
    private List<WfProcNodeVo> historyProcNodeList;

    /**
     * 是否存在任务表单信息
     * @return true:存在；false:不存在
     */
    public Boolean isExistTaskForm() {
        return ObjectUtil.isNotEmpty(this.taskFormData);
    }
}
