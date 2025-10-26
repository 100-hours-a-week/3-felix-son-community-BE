package com.kateboo.cloud.community.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/terms")
public class TermsController {

    @GetMapping("/service")
    public String termsOfService() {
        log.info("✅ 이용약관 페이지 로드됨");
        return "terms/terms-of-service";
    }

    @GetMapping("/privacy")
    public String privacyPolicy() {
        log.info("✅ 개인정보처리방침 페이지 로드됨");
        return "terms/privacy-policy";
    }
}
