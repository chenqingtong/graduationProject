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
 * 任务待逾期提醒（可）
 *
 * @author chenqingtong
 * @date 2023-03-16 09:04
 */
@Component
@Slf4j
public class TaskNotifyJob {

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
        // 当前时间
        LocalDate localDate = LocalDate.now();
        tasks.stream().filter(taskNotifyDTO -> taskNotifyDTO.getCloseTime() != null)
                .forEach(taskNotifyDTO -> {
                    if (ProjectStatusEnum.PAUSE.getStatus().equals(taskNotifyDTO.getStatus())) {
                        return;
                    }
                    if (StringUtils.isBlank(taskNotifyDTO.getEmail())) {
                        log.debug("Skip mail notify because email is empty, userId:{}", taskNotifyDTO.getUserId());
                        return;
                    }
                    LocalDate closeDate = taskNotifyDTO.getCloseTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    if (ChronoUnit.DAYS.between(localDate, closeDate) == taskNotifyDTO.getNotifyDay()) {
                        log.info("待逾期任务提醒开始, 用户id:{}, 企微id:{}, 任务id:{}", taskNotifyDTO.getUserId(), taskNotifyDTO.getUserWxName(), taskNotifyDTO.getTaskId());
                        // 进行查询 如果数据库不存在记录 则就发送提醒并插入记录
                        LambdaQueryWrapper<ProjectTaskNotify> qw = Wrappers.lambdaQuery(ProjectTaskNotify.class).eq(ProjectTaskNotify::getTaskId, taskNotifyDTO.getTaskId()).eq(ProjectTaskNotify::getOverdue, 0);
                        ProjectTaskNotify existingNotify = projectTaskNotifyMapper.selectOne(qw);
                        if (existingNotify == null) {
                            // 进行待逾期消息提醒
                            // TODO: 2024.04.25 暂时注释掉逾期任务提醒功能
//                        TaskOverdueRemindDTO taskOverdueRemindDTO = new TaskOverdueRemindDTO();
//                        // 设置任务名称
//                        taskOverdueRemindDTO.setTaskName(taskNotifyDTO.getTaskName());
//                        // 设置通知用户id
//                        taskOverdueRemindDTO.setUserIds(Collections.singletonList(taskNotifyDTO.getUserWxName()));
//                        // 设置天数
//                        taskOverdueRemindDTO.setNum(taskNotifyDTO.getNotifyDay());
//                        // 设置任务详情地址
//                        String url = SsoUrlUtils.ssoCreate(appid, agentid, host + path + ssoPath + URLEncoder.encode(host + "/pmhub-project/my-task/info?taskId=" + taskNotifyDTO.getTaskId()));
//                        taskOverdueRemindDTO.setDetailUrl(url);
//                        taskOverdueRemindDTO.setOaTitle("任务即将逾期提醒");
//                        taskOverdueRemindDTO.setOaContext("您的任务【" + taskNotifyDTO.getTaskName() + "】还有【" + taskNotifyDTO.getNotifyDay() + "】天到期，请及时处理！");
//                        taskOverdueRemindDTO.setUserName(taskNotifyDTO.getUserName());
//                        taskOverdueRemindDTO.setLinkUrl(OAUtils.ssoCreate(host + "/pmhub-project/my-task/info?taskId=" + taskNotifyDTO.getTaskId()));
//                        RocketMqUtils.push2Wx(taskOverdueRemindDTO);
                            sendUpcomingMail(taskNotifyDTO, closeDate);
                            // 插入记录
                            ProjectTaskNotify projectTaskNotify = new ProjectTaskNotify();
                            projectTaskNotify.setProjectId(taskNotifyDTO.getProjectId());
                            projectTaskNotify.setTaskId(taskNotifyDTO.getTaskId());
                            projectTaskNotify.setOverdue(0);
                            projectTaskNotify.setUserId(taskNotifyDTO.getUserId());
                            projectTaskNotify.setUserWxName(taskNotifyDTO.getUserWxName());
                            projectTaskNotify.setCloseTime(taskNotifyDTO.getCloseTime());
                            projectTaskNotify.setTaskName(taskNotifyDTO.getTaskName());
                            projectTaskNotifyMapper.insert(projectTaskNotify);
                        } else {
                            log.debug("Skip upcoming mail notify because task {} already has notify record", taskNotifyDTO.getTaskId());
                        }
                        log.info("待逾期任务提醒结束");
                    }

                });
    }

    private void sendUpcomingMail(TaskNotifyDTO dto, LocalDate closeDate) {
        EmailNoticeDTO noticeDTO = EmailNoticeDTO.builder()
                .to(Collections.singletonList(dto.getEmail()))
                .subject("任务即将逾期提醒")
                .content(buildUpcomingContent(dto, closeDate))
                .build();
        emailNoticeService.send(noticeDTO);
    }

    private String buildUpcomingContent(TaskNotifyDTO dto, LocalDate closeDate) {
        long days = dto.getNotifyDay() == null ? ChronoUnit.DAYS.between(LocalDate.now(), closeDate) : dto.getNotifyDay();
        String name = StringUtils.isBlank(dto.getUserName()) ? "同事" : dto.getUserName();
        return String.format("<p>您好，%s：</p><p>任务【%s】还有 %d 天到期（截止 %s）。请尽快处理。</p>",
                name,
                dto.getTaskName(),
                Math.max(days, 0),
                DEADLINE_FORMATTER.format(closeDate));
    }
}
