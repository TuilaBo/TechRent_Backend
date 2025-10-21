package com.rentaltech.techrental.config;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class StartupAdminInitializer implements ApplicationRunner {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Check existing admin by username
        Account existing = accountRepository.findByUsername("admin");
        if (existing != null) {
            log.info("Admin account already exists with id={}", existing.getAccountId());
            return;
        }

        // Create default admin as requested (username: admin, password: admin)
        // If Bean Validation is enforced, adjust constraints or use longer values.
        Account admin = Account.builder()
                .username("admin123")
                .password("admin123") // consider hashing if Spring Security is used
                .role(Role.Admin)
                .email("admin@localhost")
                .phoneNumber(null) // optional
                .isActive(true)
                .verificationCode(null)
                .verificationExpiry(null)
                .build();

        admin = accountRepository.save(admin);
        log.info("Created default admin account with id={}", admin.getAccountId());
    }
}
