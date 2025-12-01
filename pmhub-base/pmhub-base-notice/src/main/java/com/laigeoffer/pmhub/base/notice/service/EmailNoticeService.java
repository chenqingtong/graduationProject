package com.laigeoffer.pmhub.base.notice.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laigeoffer.pmhub.base.notice.config.MailNoticeProperties;
import com.laigeoffer.pmhub.base.notice.domain.dto.EmailNoticeDTO;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * 统一的邮件发送服务
 */
@Component
@Slf4j
public class EmailNoticeService {

    @Resource
    private MailNoticeProperties mailNoticeProperties;

    /**
     * 发送通知邮件。未配置或未开启时会直接跳过。
     */
    public void send(EmailNoticeDTO dto) {
        if (!mailNoticeProperties.ready()) {
            log.warn("Mail notice disabled or not configured, skip subject {}. enabled={}, host={}, username={}",
                    dto == null ? "" : dto.getSubject(),
                    mailNoticeProperties.isEnabled(),
                    mailNoticeProperties.getHost(),
                    mailNoticeProperties.getUsername());
            return;
        }
        if (dto == null || CollUtil.isEmpty(dto.getTo())) {
            log.warn("Mail notice skipped because receiver list is empty");
            return;
        }
        try {
            JavaMailSenderImpl mailSender = buildSender();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailNoticeProperties.fromAddress());
            helper.setTo(toArray(dto.getTo()));
            if (CollUtil.isNotEmpty(dto.getCc())) {
                helper.setCc(toArray(dto.getCc()));
            }
            if (CollUtil.isNotEmpty(dto.getBcc())) {
                helper.setBcc(toArray(dto.getBcc()));
            }
            helper.setSubject(buildSubject(dto.getSubject()));
            helper.setText(StrUtil.emptyToDefault(dto.getContent(), ""), dto.isHtmlContent());
            mailSender.send(message);
            log.info("Mail notice sent to {}", dto.getTo());
        } catch (Exception ex) {
            log.error("Failed to send mail notice", ex);
        }
    }

    private JavaMailSenderImpl buildSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailNoticeProperties.getHost());
        sender.setPort(mailNoticeProperties.getPort());
        sender.setUsername(mailNoticeProperties.getUsername());
        sender.setPassword(mailNoticeProperties.getPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", String.valueOf(mailNoticeProperties.isAuth()));
        properties.put("mail.smtp.timeout", mailNoticeProperties.getTimeout());
        properties.put("mail.smtp.connectiontimeout", mailNoticeProperties.getTimeout());
        if (mailNoticeProperties.isSsl()) {
            properties.put("mail.smtp.ssl.enable", "true");
        }
        if (mailNoticeProperties.isStarttls()) {
            properties.put("mail.smtp.starttls.enable", "true");
        }
        return sender;
    }

    private String[] toArray(List<String> list) {
        return list.stream().filter(StrUtil::isNotBlank).toArray(String[]::new);
    }

    private String buildSubject(String raw) {
        String prefix = StrUtil.blankToDefault(mailNoticeProperties.getSubjectPrefix(), "");
        if (StrUtil.isBlank(prefix)) {
            return raw;
        }
        if (StrUtil.isBlank(raw)) {
            return prefix;
        }
        return prefix + " " + raw;
    }
}

