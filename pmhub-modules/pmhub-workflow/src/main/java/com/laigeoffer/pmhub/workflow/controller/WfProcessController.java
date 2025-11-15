package com.laigeoffer.pmhub.workflow.controller;

import cn.hutool.core.bean.BeanUtil;
import com.laigeoffer.pmhub.base.core.annotation.Log;
import com.laigeoffer.pmhub.base.core.core.controller.BaseController;
import com.laigeoffer.pmhub.base.core.core.domain.PageQuery;
import com.laigeoffer.pmhub.base.core.core.domain.R;
import com.laigeoffer.pmhub.base.core.core.domain.dto.ProjectProcessDTO;
import com.laigeoffer.pmhub.base.core.core.page.Table2DataInfo;
import com.laigeoffer.pmhub.base.core.enums.BusinessType;
import com.laigeoffer.pmhub.base.core.utils.poi.ExcelUtil;
import com.laigeoffer.pmhub.base.security.annotation.InnerAuth;
import com.laigeoffer.pmhub.base.security.annotation.RequiresPermissions;
import com.laigeoffer.pmhub.base.security.utils.SecurityUtils;
import com.laigeoffer.pmhub.workflow.core.domain.ProcessQuery;
import com.laigeoffer.pmhub.workflow.domain.bo.WfCopyBo;
import com.laigeoffer.pmhub.workflow.domain.vo.*;
import com.laigeoffer.pmhub.workflow.service.IWfCopyService;
import com.laigeoffer.pmhub.workflow.service.IWfProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 工作流流程管理
 *
 * @author chenqingtong
 * @createTime 2024/3/24 18:54
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/process")
public class WfProcessController extends BaseController {

    private final IWfProcessService processService;
    private final IWfCopyService copyService;

    /**
     * 查询可发起流程列表
     *
     * @param pageQuery 分页参数
     */
    @GetMapping(value = "/list")
    @RequiresPermissions("workflow:process:startList")
    public Table2DataInfo<WfDefinitionVo> startProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        return processService.selectPageStartProcessList(processQuery, pageQuery);
    }

    /**
     * 获取待办列表
     */
    @RequiresPermissions("workflow:process:todoList")
    @GetMapping(value = "/todoList")
    public Table2DataInfo<WfTaskVo> todoProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        return processService.selectPageTodoProcessList(processQuery, pageQuery);
    }

    /**
     * 获取已办列表
     *
     * @param pageQuery 分页参数
     */
    @RequiresPermissions("workflow:process:finishedList")
    @GetMapping(value = "/finishedList")
    public Table2DataInfo<WfTaskVo> finishedProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        return processService.selectPageFinishedProcessList(processQuery, pageQuery);
    }

    /**
     * 获取抄送列表
     *
     * @param copyBo 流程抄送对象
     * @param pageQuery 分页参数
     */
    @RequiresPermissions("workflow:process:copyList")
    @GetMapping(value = "/copyList")
    public Table2DataInfo<WfCopyVo> copyProcessList(WfCopyBo copyBo, PageQuery pageQuery) {
        copyBo.setUserId(SecurityUtils.getUserId());
        return copyService.selectPageList(copyBo, pageQuery);
    }

    /**
     * 导出可发起流程列表
     */
    @RequiresPermissions("workflow:process:startExport")
    @Log(title = "可发起流程", businessType = BusinessType.EXPORT)
    @PostMapping("/startExport")
    public void startExport(@Validated ProcessQuery processQuery, HttpServletResponse response) {
        List<WfDefinitionVo> list = processService.selectStartProcessList(processQuery);
        ExcelUtil.exportExcel2(list, "可发起流程", WfDefinitionVo.class, response);
    }

    /**
     * 导出待办流程列表
     */
    @RequiresPermissions("workflow:process:todoExport")
    @Log(title = "待办流程", businessType = BusinessType.EXPORT)
    @PostMapping("/todoExport")
    public void todoExport(@Validated ProcessQuery processQuery, HttpServletResponse response) {
        List<WfTaskVo> list = processService.selectTodoProcessList(processQuery);
        List<WfTodoTaskExportVo> listVo = BeanUtil.copyToList(list, WfTodoTaskExportVo.class);
        ExcelUtil.exportExcel2(listVo, "待办流程", WfTodoTaskExportVo.class, response);
    }

    /**
     * 导出已办流程列表
     */
    @RequiresPermissions("workflow:process:finishedExport")
    @Log(title = "已办流程", businessType = BusinessType.EXPORT)
    @PostMapping("/finishedExport")
    public void finishedExport(@Validated ProcessQuery processQuery, HttpServletResponse response) {
        List<WfTaskVo> list = processService.selectFinishedProcessList(processQuery);
        List<WfFinishedTaskExportVo> listVo = BeanUtil.copyToList(list, WfFinishedTaskExportVo.class);
        ExcelUtil.exportExcel2(listVo, "已办流程", WfFinishedTaskExportVo.class, response);
    }

    /**
     * 导出抄送流程列表
     */
    @RequiresPermissions("workflow:process:copyExport")
    @Log(title = "抄送流程", businessType = BusinessType.EXPORT)
    @PostMapping("/copyExport")
    public void copyExport(WfCopyBo copyBo, HttpServletResponse response) {
        copyBo.setUserId(SecurityUtils.getUserId());
        List<WfCopyVo> list = copyService.selectList(copyBo);
        ExcelUtil.exportExcel2(list, "抄送流程", WfCopyVo.class, response);
    }

    /**
     * 根据流程定义id启动流程实例
     * processDefId 为可选参数，如果未提供则从审批设置中获取
     *
     * @param taskId 任务ID
     * @param processDefId 流程定义id（可选）
     * @param variables 变量集合,json对象
     */
    @RequiresPermissions("workflow:process:start")
    @PostMapping("/startTaskApprove/{taskId}")
    public R<Void> startTaskProcessByDefId(@PathVariable(value = "taskId") String taskId, @RequestParam(value = "processDefId", required = false) String processDefId, @RequestParam("url") String url, @RequestBody Map<String, Object> variables) {
        log.info("开始发起任务审批流程，taskId: {}, processDefId: {}, url: {}", taskId, processDefId, url);
        try {
            processService.startTaskProcessByDefId(taskId, processDefId, url, variables);
            log.info("任务审批流程启动成功，taskId: {}", taskId);
            return R.ok("流程启动成功");
        } catch (Exception e) {
            log.error("任务审批流程启动失败，taskId: {}, 错误信息: {}", taskId, e.getMessage(), e);
            throw e;
        }
    }



    /**
     * 读取xml文件
     * @param processDefId 流程定义ID
     */
    @GetMapping("/bpmnXml/{processDefId}")
    public R<String> getBpmnXml(@PathVariable(value = "processDefId") String processDefId) {
        return R.ok(null, processService.queryBpmnXmlById(processDefId));
    }

    /**
     * 查询流程详情信息
     *
     * @param procInsId 流程实例ID
     * @param deployId 部署ID
     * @param taskId 任务ID
     */
    @GetMapping("/detail")
    public R<WfDetailVo> detail(String procInsId, String deployId, String taskId) {
        return R.ok(processService.queryProcessDetail(procInsId, deployId, taskId));
    }

    /**
     * 启动项目发布流程实例
     * @param request
     * @return
     */
    @InnerAuth
    @PostMapping("/startProjectProcess")
    public R<Integer> startProjectProcess(@RequestBody ProjectProcessDTO request) {
        return R.ok(processService.startProjectProcessByDefId(request.getProjectId(), request.getProcDefId(), request.getUrl(),request.getVariables()));
    }

    /**
     * 启动任务审批流程实例
     * @param request
     * @return
     */
    @InnerAuth
    @PostMapping("/startTaskProcessByDefId")
    public R<Void> startTaskProcessByDefId(@RequestBody ProjectProcessDTO request) {
        processService.startTaskProcessByDefId(request.getProjectId(), request.getProcDefId(), request.getUrl(),request.getVariables());
        return R.ok("任务审批流程启动成功");
    }

    /**
     * 完成简化审批任务
     * @param dto 审批完成DTO
     * @return
     */
    @RequiresPermissions("workflow:process:approval")
    @PostMapping("/completeSimplifiedApproval")
    @Log(title = "简化审批", businessType = BusinessType.UPDATE)
    public R<Void> completeSimplifiedApproval(@RequestBody com.laigeoffer.pmhub.workflow.domain.dto.ApprovalCompleteDTO dto) {
        processService.completeSimplifiedApproval(dto);
        return R.ok(dto.getApproved() ? "审批通过" : "审批已拒绝");
    }

    /**
     * 查询审批任务列表（用于显示审批进度）
     * @param extraId 业务ID（如任务ID）
     * @param type 审批类型（如task）
     * @return 审批任务列表
     */
    @GetMapping("/getApprovalTaskList")
    @RequiresPermissions("workflow:process:query")
    public R<List<com.laigeoffer.pmhub.workflow.domain.WfApprovalTask>> getApprovalTaskList(
            @RequestParam("extraId") String extraId,
            @RequestParam("type") String type) {
        List<com.laigeoffer.pmhub.workflow.domain.WfApprovalTask> list = processService.getApprovalTaskList(extraId, type);
        return R.ok(list);
    }
}
