package com.kateboo.cloud.community.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
    400 Bad Request
    클라이언트의 요청이 잘못되었을 때
    ex) 필수필드 누락, 유효성 검사 실패, 문법 오류
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}