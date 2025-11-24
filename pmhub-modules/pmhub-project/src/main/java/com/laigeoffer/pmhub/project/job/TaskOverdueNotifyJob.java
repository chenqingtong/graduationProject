package com.laigeoffer.pmhub.project.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.laigeoffer.pmhub.base.core.enums.ProjectStatusEnum;
import com.laigeoffer.pmhub.base.core.utils.StringUtils;
import com.laigeoffer.pmhub.base.notice.domain.dto.EmailNoticeDTO;
import com.laigeoffer.pmhub.base.notice.service.EmailNoticeService;
import com.laigeoffer.pmhub.project.domain.ProjectTaskNotify;
import com.laigeoffer.pmhub.project.domain.vo.project.task.TaskNotifyDTO;
import com.laigeoffer.pmhub.project.mapper.ProjectTaskMapper;
import com.laigeoffer.pmhub.project.mapper.ProjectTaskNotifyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;


/**
 * 任务已逾期提醒
 *
 * @author chenqingtong
 * @date 2023-03-16 09:04
 */
@Component
@Slf4j
public class TaskOverdueNotifyJob {

    @Autowired
    private ProjectTaskMapper projectTaskMapper;
    @Autowired
    private ProjectTaskNotifyMapper projectTaskNotifyMapper;
    @Autowired
    private EmailNoticeService emailNoticeService;

    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void taskNotify() {
        List<TaskNotifyDTO> tasks = projectTaskMapper.queryTaskNotifyJob();
        if (log.isDebugEnabled()) {
            log.debug("TaskOverdueNotifyJob fetched {} candidate tasks", tasks.size());
        }
        LocalDate localDate = LocalDate.now();
        tasks.stream().filter(taskNotifyDTO -> taskNotifyDTO.getCloseTime() != null)
                .forEach(taskNotifyDTO -> {
                    if (ProjectStatusEnum.PAUSE.getStatus().equals(taskNotifyDTO.getStatus())) {
                        log.debug("Skip overdue mail notify because project {} is paused", taskNotifyDTO.getProjectId());
                        return;
                    }
                    if (StringUtils.isBlank(taskNotifyDTO.getEmail())) {
                        log.debug("Skip overdue mail notify because email is empty, userId:{}", taskNotifyDTO.getUserId());
                        return;
                    }
                    LocalDate closeDate = taskNotifyDTO.getCloseTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    if (ChronoUnit.DAYS.between(localDate, closeDate) < 0L) {
                        log.info("已逾期任务提醒开始, 用户id:{}, 企微id:{}, 任务id:{}", taskNotifyDTO.getUserId(), taskNotifyDTO.getUserWxName(), taskNotifyDTO.getTaskId());
                        LambdaQueryWrapper<ProjectTaskNotify> qw = Wrappers.lambdaQuery(ProjectTaskNotify.class).eq(ProjectTaskNotify::getTaskId, taskNotifyDTO.getTaskId()).eq(ProjectTaskNotify::getOverdue, 1);
                        ProjectTaskNotify projectTaskNotify = projectTaskNotifyMapper.selectOne(qw);
                        // 如果数据库不存在记录则就进行消息通知及插入数据库
                        if (projectTaskNotify == null) {
                            // 进行逾期任务消息提醒（需要使用MQ请开启这段注释）
//                            TaskOvertimeRemindDTO taskOvertimeRemindDTO = new TaskOvertimeRemindDTO();
//                            // 设置发送用户
//                            taskOvertimeRemindDTO.setUserIds(Collections.singletonList(taskNotifyDTO.getUserWxName()));
//                            // 设置任务详情页
//                            String url = SsoUrlUtils.ssoCreate(appid, agentid, host + path + ssoPath + URLEncoder.encode(host + "/pmhub-project/my-task/info?taskId=" + taskNotifyDTO.getTaskId()));
//                            taskOvertimeRemindDTO.setDetailUrl(url);
//                            // 设置任务名称
//                            taskOvertimeRemindDTO.setTaskName(taskNotifyDTO.getTaskName());
//                            taskOvertimeRemindDTO.setOaTitle("任务已逾期提醒");
//                            taskOvertimeRemindDTO.setOaContext("您的任务【" + taskNotifyDTO.getTaskName() + "】已经逾期，请及时处理！");
//                            taskOvertimeRemindDTO.setUserName(taskNotifyDTO.getUserName());
//                            taskOvertimeRemindDTO.setLinkUrl(OAUtils.ssoCreate(host + "/pmhub-project/my-task/info?taskId=" + taskNotifyDTO.getTaskId()));
//                            RocketMqUtils.push2Wx(taskOvertimeRemindDTO);

                            sendOverdueMail(taskNotifyDTO, closeDate);
                            // 插入记录
                            ProjectTaskNotify ptn = new ProjectTaskNotify();
                            ptn.setProjectId(taskNotifyDTO.getProjectId());
                            ptn.setTaskId(taskNotifyDTO.getTaskId());
                            ptn.setOverdue(1);
                            ptn.setUserId(taskNotifyDTO.getUserId());
                            ptn.setUserWxName(taskNotifyDTO.getUserWxName());
                            ptn.setCloseTime(taskNotifyDTO.getCloseTime());
                            ptn.setTaskName(taskNotifyDTO.getTaskName());
                            projectTaskNotifyMapper.insert(ptn);
                        } else {
                            log.debug("Skip overdue mail notify because task {} already has overdue record", taskNotifyDTO.getTaskId());
                        }
                        log.info("已逾期任务提醒结束");

                    } else if (log.isDebugEnabled()) {
                        log.debug("Skip overdue mail notify because task {} deadline {} not overdue yet", taskNotifyDTO.getTaskId(),
                                taskNotifyDTO.getCloseTime());
                    }
                });
    }

    private void sendOverdueMail(TaskNotifyDTO dto, LocalDate closeDate) {
        EmailNoticeDTO noticeDTO = EmailNoticeDTO.builder()
                .to(Collections.singletonList(dto.getEmail()))
                .subject("任务已逾期提醒")
                .content(buildOverdueContent(dto, closeDate))
                .build();
        emailNoticeService.send(noticeDTO);
    }

    private String buildOverdueContent(TaskNotifyDTO dto, LocalDate closeDate) {
        String name = StringUtils.isBlank(dto.getUserName()) ? "同事" : dto.getUserName();
        return String.format("<p>您好，%s：</p><p>任务【%s】已于 %s 到期，请尽快处理。</p>",
                name,
                dto.getTaskName(),
                DEADLINE_FORMATTER.format(closeDate));
    }
}
