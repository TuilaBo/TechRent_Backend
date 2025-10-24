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
@Order(1) // Chạy sau StartupAdminInitializer
@RequiredArgsConstructor
public class RoleMigrationRunner implements ApplicationRunner {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting role migration...");
        
        // Migrate old role values to new format
        migrateRole("Customer", Role.CUSTOMER);
        migrateRole("Admin", Role.ADMIN);
        migrateRole("Operator", Role.OPERATOR);
        migrateRole("Technician", Role.TECHNICIAN);
        migrateRole("CustomerSupportStaff", Role.CUSTOMER_SUPPORT_STAFF);
        
        log.info("Role migration completed!");
    }
    
    private void migrateRole(String oldRoleValue, Role newRole) {
        try {
            // Tìm accounts có role cũ (stored as string in database)
            var accounts = accountRepository.findAll().stream()
                    .filter(account -> {
                        try {
                            return account.getRole() != null && 
                                   account.getRole().toString().equals(oldRoleValue);
                        } catch (Exception e) {
                            log.warn("Error checking role for account {}: {}", 
                                    account.getUsername(), e.getMessage());
                            return false;
                        }
                    })
                    .toList();
            
            if (!accounts.isEmpty()) {
                log.info("Migrating {} accounts from {} to {}", accounts.size(), oldRoleValue, newRole);
                
                for (Account account : accounts) {
                    try {
                        account.setRole(newRole);
                        accountRepository.save(account);
                        log.info("Successfully migrated account: {}", account.getUsername());
                    } catch (Exception e) {
                        log.error("Error migrating account {}: {}", account.getUsername(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error migrating role {} to {}: {}", oldRoleValue, newRole, e.getMessage());
        }
    }
}
