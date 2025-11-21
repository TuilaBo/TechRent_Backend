package com.rentaltech.techrental.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Run early, but after Hibernate schema update
// Note: This migration is idempotent - safe to run multiple times
public class DatabaseMigrationInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Starting database migration for existing data...");

            // Fix handover_report.status - only if column exists and has nulls
            try {
                int statusUpdated = jdbcTemplate.update(
                        "UPDATE handover_report SET status = 'PENDING_STAFF_SIGNATURE' WHERE status IS NULL"
                );
                if (statusUpdated > 0) {
                    log.info("Updated {} handover_report records with default status", statusUpdated);
                }
            } catch (Exception e) {
                log.debug("Status column migration skipped: {}", e.getMessage());
            }

            // Fix handover_report.staff_signed
            try {
                int staffSignedUpdated = jdbcTemplate.update(
                        "UPDATE handover_report SET staff_signed = false WHERE staff_signed IS NULL"
                );
                if (staffSignedUpdated > 0) {
                    log.info("Updated {} handover_report records with default staff_signed", staffSignedUpdated);
                }
            } catch (Exception e) {
                log.debug("Staff_signed column migration skipped: {}", e.getMessage());
            }

            // Fix handover_report.customer_signed
            try {
                int customerSignedUpdated = jdbcTemplate.update(
                        "UPDATE handover_report SET customer_signed = false WHERE customer_signed IS NULL"
                );
                if (customerSignedUpdated > 0) {
                    log.info("Updated {} handover_report records with default customer_signed", customerSignedUpdated);
                }
            } catch (Exception e) {
                log.debug("Customer_signed column migration skipped: {}", e.getMessage());
            }

            // Fix Device.usage_count
            try {
                int usageCountUpdated = jdbcTemplate.update(
                        "UPDATE \"Device\" SET usage_count = 0 WHERE usage_count IS NULL"
                );
                if (usageCountUpdated > 0) {
                    log.info("Updated {} Device records with default usage_count", usageCountUpdated);
                }
            } catch (Exception e) {
                log.debug("Usage_count column migration skipped: {}", e.getMessage());
            }

            // After updating data, add NOT NULL constraints (only if column exists and is nullable)
            // Check if column exists and is nullable before adding constraint
            try {
                Boolean isStatusNullable = jdbcTemplate.queryForObject(
                        "SELECT is_nullable = 'YES' FROM information_schema.columns " +
                        "WHERE table_name = 'handover_report' AND column_name = 'status'",
                        Boolean.class
                );
                if (Boolean.TRUE.equals(isStatusNullable)) {
                    jdbcTemplate.execute(
                            "ALTER TABLE handover_report ALTER COLUMN status SET NOT NULL"
                    );
                    jdbcTemplate.execute(
                            "ALTER TABLE handover_report ALTER COLUMN status SET DEFAULT 'PENDING_STAFF_SIGNATURE'"
                    );
                    log.info("Added NOT NULL constraint to handover_report.status");
                } else {
                    log.debug("handover_report.status is already NOT NULL");
                }
            } catch (Exception e) {
                log.debug("Could not add NOT NULL to status (may not exist or already set): {}", e.getMessage());
            }

            try {
                Boolean isStaffSignedNullable = jdbcTemplate.queryForObject(
                        "SELECT is_nullable = 'YES' FROM information_schema.columns " +
                        "WHERE table_name = 'handover_report' AND column_name = 'staff_signed'",
                        Boolean.class
                );
                if (Boolean.TRUE.equals(isStaffSignedNullable)) {
                    jdbcTemplate.execute(
                            "ALTER TABLE handover_report ALTER COLUMN staff_signed SET NOT NULL"
                    );
                    jdbcTemplate.execute(
                            "ALTER TABLE handover_report ALTER COLUMN staff_signed SET DEFAULT false"
                    );
                    log.info("Added NOT NULL constraint to handover_report.staff_signed");
                } else {
                    log.debug("handover_report.staff_signed is already NOT NULL");
                }
            } catch (Exception e) {
                log.debug("Could not add NOT NULL to staff_signed (may not exist or already set): {}", e.getMessage());
            }

            try {
                Boolean isCustomerSignedNullable = jdbcTemplate.queryForObject(
                        "SELECT is_nullable = 'YES' FROM information_schema.columns " +
                        "WHERE table_name = 'handover_report' AND column_name = 'customer_signed'",
                        Boolean.class
                );
                if (Boolean.TRUE.equals(isCustomerSignedNullable)) {
                    jdbcTemplate.execute(
                            "ALTER TABLE handover_report ALTER COLUMN customer_signed SET NOT NULL"
                    );
                    jdbcTemplate.execute(
                            "ALTER TABLE handover_report ALTER COLUMN customer_signed SET DEFAULT false"
                    );
                    log.info("Added NOT NULL constraint to handover_report.customer_signed");
                } else {
                    log.debug("handover_report.customer_signed is already NOT NULL");
                }
            } catch (Exception e) {
                log.debug("Could not add NOT NULL to customer_signed (may not exist or already set): {}", e.getMessage());
            }

            try {
                Boolean isUsageCountNullable = jdbcTemplate.queryForObject(
                        "SELECT is_nullable = 'YES' FROM information_schema.columns " +
                        "WHERE table_name = 'Device' AND column_name = 'usage_count'",
                        Boolean.class
                );
                if (Boolean.TRUE.equals(isUsageCountNullable)) {
                    jdbcTemplate.execute(
                            "ALTER TABLE \"Device\" ALTER COLUMN usage_count SET NOT NULL"
                    );
                    jdbcTemplate.execute(
                            "ALTER TABLE \"Device\" ALTER COLUMN usage_count SET DEFAULT 0"
                    );
                    log.info("Added NOT NULL constraint to Device.usage_count");
                } else {
                    log.debug("Device.usage_count is already NOT NULL");
                }
            } catch (Exception e) {
                log.debug("Could not add NOT NULL to usage_count (may not exist or already set): {}", e.getMessage());
            }

            log.info("Database migration completed successfully");
        } catch (Exception e) {
            log.warn("Database migration failed (this is OK if columns don't exist yet): {}", e.getMessage());
        }
    }
}

