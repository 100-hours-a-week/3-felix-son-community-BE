package com.kateboo.cloud.community.scheduler;

import com.kateboo.cloud.community.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 비활성 계정 자동 정리 스케줄러
 * 7일이 지난 탈퇴 계정을 자동으로 영구 삭제
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCleanupScheduler {

    private final UserService userService;

    /**
     * 매일 새벽 3시에 7일 지난 계정 자동 삭제
     * cron: 초(0) 분(0) 시(3) 일(*) 월(*) 요일(*)
     *
     * 다른 스케줄 옵션:
     * - 10분마다: "0 *\/10 * * * *"  (테스트용)
     * - 매시간 정각: "0 0 * * * *"
     * - 매주 월요일 새벽 3시: "0 0 3 * * MON"
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredAccounts() {
        log.info("========================================");
        log.info("비활성 계정 자동 정리 시작");
        log.info("========================================");

        try {
            userService.deleteExpiredAccounts();
            log.info("비활성 계정 자동 정리 완료");
        } catch (Exception e) {
            log.error("비활성 계정 정리 중 오류 발생", e);
        }

        log.info("========================================");
    }

}
