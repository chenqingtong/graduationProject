package com.laigeoffer.pmhub.gateway.config;

import com.laigeoffer.pmhub.base.core.config.PmhubConfig;
import com.laigeoffer.pmhub.base.core.constant.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * 网关静态资源处理配置
 * 用于处理 /profile/** 路径的静态资源请求
 *
 * @author chenqingtong
 */
@Configuration
public class StaticResourceConfig {

    /**
     * 配置静态资源路由
     */
    @Bean
    public RouterFunction<ServerResponse> staticResourceRouterFunction() {
        return RouterFunctions.route(
                GET(Constants.RESOURCE_PREFIX + "/**"),
                request -> {
                    try {
                        // 获取请求路径
                        String requestPath = request.path();
                        
                        // 移除 /profile 前缀，获取实际文件路径
                        String filePath = requestPath.substring(Constants.RESOURCE_PREFIX.length());
                        
                        // URL解码，处理中文文件名
                        filePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8.toString());
                        
                        // 安全检查：防止路径遍历攻击
                        if (filePath.contains("..")) {
                            return ServerResponse.status(HttpStatus.FORBIDDEN).build();
                        }
                        
                        // 构建完整文件路径
                        String profilePath = PmhubConfig.getProfile();
                        if (profilePath == null || profilePath.isEmpty()) {
                            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .bodyValue("文件路径配置未设置");
                        }
                        
                        // 确保路径分隔符正确
                        if (!profilePath.endsWith(File.separator) && !filePath.startsWith("/")) {
                            profilePath += File.separator;
                        }
                        if (filePath.startsWith("/")) {
                            filePath = filePath.substring(1);
                        }
                        
                        Path fullPath = Paths.get(profilePath, filePath);
                        File file = fullPath.toFile();
                        
                        // 检查文件是否存在
                        if (!file.exists() || !file.isFile()) {
                            return ServerResponse.status(HttpStatus.NOT_FOUND).build();
                        }
                        
                        // 检查文件是否在配置的目录下（防止路径遍历）
                        Path profileBasePath = Paths.get(profilePath).toAbsolutePath().normalize();
                        Path fileAbsolutePath = fullPath.toAbsolutePath().normalize();
                        if (!fileAbsolutePath.startsWith(profileBasePath)) {
                            return ServerResponse.status(HttpStatus.FORBIDDEN).build();
                        }
                        
                        // 根据文件扩展名设置Content-Type
                        String contentType = getContentType(file.getName());
                        
                        // 读取文件内容
                        Flux<DataBuffer> dataBufferFlux = DataBufferUtils.readInputStream(
                                () -> new FileInputStream(file),
                                new DefaultDataBufferFactory(),
                                4096
                        );
                        
                        // 设置响应头
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.parseMediaType(contentType));
                        headers.setContentLength(file.length());
                        headers.setCacheControl("public, max-age=3600");
                        
                        return ServerResponse.ok()
                                .headers(h -> h.addAll(headers))
                                .body(dataBufferFlux, DataBuffer.class);
                    } catch (Exception e) {
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .bodyValue("文件读取失败: " + e.getMessage());
                    }
                }
        );
    }
    
    /**
     * 根据文件名获取Content-Type
     */
    private String getContentType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerFileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFileName.endsWith(".zip")) {
            return "application/zip";
        } else if (lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".docx")) {
            return "application/msword";
        } else if (lowerFileName.endsWith(".xls") || lowerFileName.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else {
            return "application/octet-stream";
        }
    }
}

