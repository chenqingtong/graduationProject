package com.laigeoffer.pmhub.base.notice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 邮件通知载体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNoticeDTO {

    /**
     * 收件人列表，使用 QQ 邮箱时填写完整邮箱地址
     */
    @Builder.Default
    private List<String> to = Collections.emptyList();

    /**
     * 抄送人
     */
    @Builder.Default
    private List<String> cc = Collections.emptyList();

    /**
     * 密送人
     */
    @Builder.Default
    private List<String> bcc = Collections.emptyList();

    /**
     * 邮件主题
     */
    private String subject;

    /**
     * 邮件正文
     */
    private String content;

    /**
     * 是否以 HTML 方式渲染正文，默认开启方便排版
     */
    @Builder.Default
    private boolean htmlContent = true;

}


