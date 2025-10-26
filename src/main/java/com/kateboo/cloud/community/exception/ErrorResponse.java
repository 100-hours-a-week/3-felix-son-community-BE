package com.kateboo.cloud.community.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON에서 제외
public class ErrorResponse {

    private Integer status;          // HTTP 상태 코드
    private String error;            // 에러 타입
    private String message;          // 에러 메시지
    private LocalDateTime timestamp; // 발생 시간
    private Map<String, String> validationErrors; // 검증 에러 상세 (선택)
}