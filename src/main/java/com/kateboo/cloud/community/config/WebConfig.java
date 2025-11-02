package com.kateboo.cloud.community.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**","/api/posts", "/api/posts/{postId}", "/api/comments/post/{postId}");
    }

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init(){
        log.info("정적 리소스 경로 설정: /uploads/** -> file:{}", uploadDir);
    }


    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowCredentials(true)
                .allowedHeaders("*");

//        registry.addMapping("/uploads/**")
//                .allowedOrigins("http://localhost:3000")
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*");
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