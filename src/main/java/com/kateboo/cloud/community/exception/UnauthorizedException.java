package com.kateboo.cloud.community.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
    401 Unauthorized
    인증이 필요한 요청에서, 클라이언트가 인증되지 않았거나
    잘못된 인증 정보를 보냈을 때
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException{
    public UnauthorizedException(String message){
        super(message);
    }
}
