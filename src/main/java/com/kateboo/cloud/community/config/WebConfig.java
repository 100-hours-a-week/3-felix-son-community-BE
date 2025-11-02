package com.kateboo.cloud.community.config;

import com.kateboo.cloud.community.security.CurrentUserArgumentResolver;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CurrentUserArgumentResolver currentUserArgumentResolver;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init(){
        log.info("정적 리소스 경로 설정: /uploads/** -> file:{}", uploadDir);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver); // ✅ 추가
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:8080") // 프론트엔드 주소 추가
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // ✅ 쿠키 전송 허용
                .exposedHeaders("Authorization");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 파일을 정적 리소스로 제공
        // URL: /uploads/images/abc123.jpg
        // 실제 파일: uploads/images/abc123.jpg
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations("file:" + uploadDir + "/");

        log.info("정적 리소스 핸들러 등록 완료");
    }
}