package com.kateboo.cloud.community.exception;

/*
    500 Internal Server error
    서버 내부에서 예기치 못한 오류가 발생했을 때
 */
public class InternalServerException extends RuntimeException {
    public InternalServerException(String message) {
        super(message);
    }
}
