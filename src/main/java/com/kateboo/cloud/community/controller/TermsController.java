package com.kateboo.cloud.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/terms")
public class TermsController {

    @GetMapping("/service")
    public String termsOfService() {
        return "terms/terms-of-service";
    }

    @GetMapping("/privacy")
    public String privacyPolicy() {
        return "terms/privacy-policy";
    }
}