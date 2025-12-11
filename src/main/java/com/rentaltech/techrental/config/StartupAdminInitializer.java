//package com.rentaltech.techrental.config;
//
//import com.rentaltech.techrental.authentication.model.Account;
//import com.rentaltech.techrental.authentication.model.Role;
//import com.rentaltech.techrental.authentication.repository.AccountRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//@Slf4j
//@Component
//@Order(0)
//@RequiredArgsConstructor
//public class StartupAdminInitializer implements ApplicationRunner {
//
//    private final AccountRepository accountRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Override
//    @Transactional
//    public void run(ApplicationArguments args) {
//        // Check existing admin by username
//        Account existing = accountRepository.findByUsername("admin123");
//        if (existing != null) {
//            log.info("Admin account already exists with id={}", existing.getAccountId());
//            // Nếu password chưa được encode, encode lại
//            if (existing.getPassword() != null && !existing.getPassword().startsWith("$2a$") && !existing.getPassword().startsWith("$2b$")) {
//                existing.setPassword(passwordEncoder.encode(existing.getPassword()));
//                accountRepository.save(existing);
//                log.info("Updated admin password to encrypted format");
//            }
//            return;
//        }
//
//        // Create default admin as requested (username: admin123, password: admin123)
//        Account admin = Account.builder()
//                .username("admin123")
//                .password(passwordEncoder.encode("admin123")) // Encode password với BCrypt
//                .role(Role.ADMIN)
//                .email("admin@localhost")
//                .phoneNumber(null) // optional
//                .isActive(true)
//                .verificationCode(null)
//                .verificationExpiry(null)
//                .build();
//
//        admin = accountRepository.save(admin);
//        log.info("Created default admin account with id={}", admin.getAccountId());
//    }
//}
