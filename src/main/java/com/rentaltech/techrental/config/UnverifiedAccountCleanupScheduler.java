package com.rentaltech.techrental.config;

import com.rentaltech.techrental.authentication.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnverifiedAccountCleanupScheduler {

    private final AccountService accountService;

    /**
     * Cleanup unverified accounts mỗi ngày lúc 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupUnverifiedAccounts() {
        log.info("Starting cleanup of unverified accounts...");
        try {
            accountService.cleanupUnverifiedAccounts();
            log.info("Cleanup of unverified accounts completed successfully");
        } catch (Exception e) {
            log.error("Error during cleanup of unverified accounts: {}", e.getMessage(), e);
        }
    }
}
