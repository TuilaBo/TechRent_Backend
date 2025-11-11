package com.rentaltech.techrental.finance.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class VnpayDatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixPaymentMethodConstraint() {
        try {
            // Drop existing constraint if exists
            jdbcTemplate.execute("ALTER TABLE invoice DROP CONSTRAINT IF EXISTS invoice_payment_method_check");
            
            // Add new constraint with VNPAY
            jdbcTemplate.execute(
                "ALTER TABLE invoice ADD CONSTRAINT invoice_payment_method_check " +
                "CHECK (payment_method IN ('PAYOS', 'VNPAY', 'MOMO', 'BANK_ACCOUNT'))"
            );
            
            log.info("Successfully updated invoice_payment_method_check constraint to include VNPAY");
        } catch (Exception e) {
            log.warn("Could not update payment_method constraint (may already be correct): {}", e.getMessage());
        }
        
        try {
            // Add frontend URLs columns if they don't exist
            jdbcTemplate.execute(
                "DO $$ " +
                "BEGIN " +
                "  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='invoice' AND column_name='frontend_success_url') THEN " +
                "    ALTER TABLE invoice ADD COLUMN frontend_success_url VARCHAR(500); " +
                "  END IF; " +
                "  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='invoice' AND column_name='frontend_failure_url') THEN " +
                "    ALTER TABLE invoice ADD COLUMN frontend_failure_url VARCHAR(500); " +
                "  END IF; " +
                "END $$;"
            );
            log.info("Successfully added frontend URLs columns to invoice table");
        } catch (Exception e) {
            log.warn("Could not add frontend URLs columns (may already exist): {}", e.getMessage());
        }
    }
}

