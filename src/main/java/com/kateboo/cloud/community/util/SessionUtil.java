package com.kateboo.cloud.community.util;

import com.kateboo.cloud.community.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class SessionUtil {

    private static final String USER_ID_KEY = "userId";

    /**
     * 세션에서 userId 가져오기
     * 없으면 UnauthorizedException 발생
     */
    public static UUID getUserIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // ✅ 세션이 없으면 null 반환

        if (session == null) {
            log.warn("세션이 존재하지 않습니다.");
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        Object userIdObj = session.getAttribute(USER_ID_KEY);

        if (userIdObj == null) {
            log.warn("세션에 userId가 없습니다. Session ID: {}", session.getId());
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        try {
            if (userIdObj instanceof String) {
                return UUID.fromString((String) userIdObj);
            } else if (userIdObj instanceof UUID) {
                return (UUID) userIdObj;
            } else {
                log.error("세션의 userId 타입이 올바르지 않습니다: {}", userIdObj.getClass());
                throw new UnauthorizedException("세션 정보가 올바르지 않습니다.");
            }
        } catch (IllegalArgumentException e) {
            log.error("userId를 UUID로 변환 실패: {}", userIdObj, e);
            throw new UnauthorizedException("세션 정보가 올바르지 않습니다.");
        }
    }

    /**
     * 세션에 userId 저장
     */
    public static void setUserIdInSession(HttpServletRequest request, UUID userId) {
        HttpSession session = request.getSession(true); // ✅ 세션이 없으면 생성
        session.setAttribute(USER_ID_KEY, userId);
        log.info("세션에 userId 저장: {}, Session ID: {}", userId, session.getId());
    }

    /**
     * 세션 무효화 (로그아웃)
     */
    public static void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("세션 무효화: Session ID: {}", session.getId());
            session.invalidate();
        }
    }
}