package com.kateboo.cloud.community.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
    403 Forbidden
    클라이언트가 인증은 되었으나, 해당 리소스에 대한 접근 권한이 없을 때
*/
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
