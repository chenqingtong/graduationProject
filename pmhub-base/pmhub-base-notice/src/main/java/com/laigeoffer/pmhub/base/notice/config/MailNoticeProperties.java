package com.laigeoffer.pmhub.base.notice.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件发送相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "pmhub.mail")
public class MailNoticeProperties {

    /**
     * 是否开启邮件通知
     */
    private boolean enabled = false;

    /**
     * SMTP 服务器地址，例如 smtp.qq.com
     */
    private String host;

    /**
     * SMTP 端口，QQ 邮箱 SSL 口默认 465
     */
    private Integer port = 465;

    /**
     * 登录账号（QQ 邮箱完整地址）
     */
    private String username;

    /**
     * 授权码/客户端专用密码
     */
    private String password;

    /**
     * 自定义发件人，如果为空则使用 username
     */
    private String from;

    /**
     * 邮件主题统一前缀
     */
    private String subjectPrefix = "[PMHub]";

    /**
     * 是否开启 SMTP AUTH
     */
    private boolean auth = true;

    /**
     * 是否启用 SSL
     */
    private boolean ssl = true;

    /**
     * 是否启用 STARTTLS
     */
    private boolean starttls = false;

    /**
     * 发送/连接超时时间（毫秒）
     */
    private Integer timeout = 15000;

    /**
     * 配置是否完备
     */
    public boolean ready() {
        return enabled && StrUtil.isAllNotBlank(host, username, password);
    }

    public String fromAddress() {
        return StrUtil.blankToDefault(from, username);
    }
}


