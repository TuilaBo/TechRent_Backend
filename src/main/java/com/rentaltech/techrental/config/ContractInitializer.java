package com.rentaltech.techrental.config;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.ContractType;
import com.rentaltech.techrental.contract.repository.ContractRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.OrderStatus;
import com.rentaltech.techrental.webapi.customer.model.RentalOrder;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.repository.RentalOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
// @Component
@Order(3)
@Profile("!test")
@RequiredArgsConstructor
public class ContractInitializer implements ApplicationRunner {

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (contractRepository.count() > 0) {
            log.debug("Contract data already exists, skipping initialization");
            return;
        }

        log.info("Initializing sample contract data...");

        try {
            // Get or create a sample customer
            Customer sampleCustomer = getOrCreateSampleCustomer();

            // Get or create sample orders
            RentalOrder order1 = getOrCreateSampleOrder(sampleCustomer, 1);
            RentalOrder order2 = getOrCreateSampleOrder(sampleCustomer, 2);

            // Create sample contracts
            createSampleContracts(sampleCustomer, order1, order2);
            
            log.info("Contract initialization completed successfully");
        } catch (Exception e) {
            log.error("Error initializing contract data: {}", e.getMessage(), e);
            // Skip initialization if error occurs - don't crash app
        }
    }

    private Customer getOrCreateSampleCustomer() {
        return customerRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Account account = accountRepository.findAll().stream()
                            .findFirst()
                            .orElseGet(() -> {
                                Account newAccount = Account.builder()
                                        .username("sample_customer")
                                        .password("password123")
                                        .email("customer@example.com")
                                        .isActive(true)
                                        .role(com.rentaltech.techrental.authentication.model.Role.CUSTOMER)
                                        .build();
                                return accountRepository.save(newAccount);
                            });

                    Customer customer = Customer.builder()
                            .account(account)
                            .email("customer@example.com")
                            .phoneNumber("0901234567")
                            .fullName("Nguyễn Văn Khách Hàng")
                            .shippingAddress("123 Đường ABC, Quận XYZ, TP.HCM")
                            .status(com.rentaltech.techrental.webapi.customer.model.CustomerStatus.ACTIVE)
                            .build();
                    return customerRepository.save(customer);
                });
    }

    private RentalOrder getOrCreateSampleOrder(Customer customer, int orderNum) {
        if (rentalOrderRepository.count() >= orderNum) {
            return rentalOrderRepository.findAll().get(orderNum - 1);
        }

        RentalOrder order = RentalOrder.builder()
                .startDate(LocalDateTime.now().plusDays(orderNum * 7))
                .endDate(LocalDateTime.now().plusDays(orderNum * 7 + 30))
                .shippingAddress("123 Đường ABC, Quận XYZ, TP.HCM")
                .orderStatus(OrderStatus.PENDING)
                .depositAmount(BigDecimal.valueOf(1000000.0 * orderNum))
                .depositAmountHeld(BigDecimal.ZERO)
                .depositAmountUsed(BigDecimal.ZERO)
                .depositAmountRefunded(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(5000000.0 * orderNum))
                .pricePerDay(BigDecimal.valueOf(200000.0))
                .customer(customer)
                .build();

        return rentalOrderRepository.save(order);
    }

    private void createSampleContracts(Customer customer, RentalOrder order1, RentalOrder order2) {
        LocalDateTime now = LocalDateTime.now();

        // Contract 1: DRAFT - linked to order1
        Contract contract1 = Contract.builder()
                .contractNumber("HD" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0001")
                .title("Hợp đồng thuê laptop Dell XPS - Đơn #" + order1.getOrderId())
                .description("Hợp đồng thuê laptop Dell XPS 15 cho dự án phát triển phần mềm trong 30 ngày")
                .contractType(ContractType.EQUIPMENT_RENTAL)
                .status(ContractStatus.DRAFT)
                .customerId(customer.getCustomerId())
                .orderId(order1.getOrderId())
                .contractContent("<h2>HỢP ĐỒNG THUÊ THIẾT BỊ CÔNG NGHỆ</h2>" +
                        "<p><strong>Đơn thuê:</strong> #" + order1.getOrderId() + "</p>" +
                        "<p><strong>Ngày bắt đầu:</strong> " + order1.getStartDate() + "</p>" +
                        "<p><strong>Ngày kết thúc:</strong> " + order1.getEndDate() + "</p>" +
                        "<p><strong>Số ngày thuê:</strong> 30 ngày</p>" +
                        "<h3>Thiết bị thuê:</h3>" +
                        "<ul>" +
                        "<li>3x Laptop Dell XPS 15 (Dell) - Giá/ngày: 200000 - Tiền cọc: 15000000</li>" +
                        "</ul>" +
                        "<p><strong>Tổng tiền thuê:</strong> " + order1.getTotalPrice() + " VNĐ</p>" +
                        "<p><strong>Tiền cọc:</strong> " + order1.getDepositAmount() + " VNĐ</p>")
                .termsAndConditions("ĐIỀU KHOẢN VÀ ĐIỀU KIỆN THUÊ THIẾT BỊ\n\n" +
                        "1. Khách hàng có trách nhiệm bảo quản thiết bị trong thời gian thuê.\n" +
                        "2. Nếu thiết bị bị hư hỏng hoặc mất mát, khách hàng sẽ phải chịu chi phí sửa chữa hoặc thay thế.\n" +
                        "3. Tiền cọc sẽ được hoàn trả sau khi thiết bị được kiểm tra và không có hư hỏng.\n" +
                        "4. Khách hàng phải trả lại thiết bị đúng hạn, nếu quá hạn sẽ bị phạt 10% giá trị thiết bị mỗi ngày.\n" +
                        "5. Mọi tranh chấp sẽ được giải quyết theo pháp luật Việt Nam.\n")
                .rentalPeriodDays(30)
                .totalAmount(order1.getTotalPrice())
                .depositAmount(order1.getDepositAmount())
                .startDate(order1.getStartDate())
                .endDate(order1.getEndDate())
                .expiresAt(order1.getEndDate().plusDays(7))
                .createdBy(customer.getAccount().getAccountId())
                .build();

        // Contract 2: PENDING_SIGNATURE - linked to order2
        Contract contract2 = Contract.builder()
                .contractNumber("HD" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0002")
                .title("Hợp đồng thuê máy trạm Lenovo - Đơn #" + order2.getOrderId())
                .description("Hợp đồng thuê máy trạm Lenovo ThinkStation cho công việc thiết kế đồ họa chuyên nghiệp")
                .contractType(ContractType.EQUIPMENT_RENTAL)
                .status(ContractStatus.PENDING_SIGNATURE)
                .customerId(customer.getCustomerId())
                .orderId(order2.getOrderId())
                .contractContent("<h2>HỢP ĐỒNG THUÊ THIẾT BỊ CÔNG NGHỆ</h2>" +
                        "<p><strong>Đơn thuê:</strong> #" + order2.getOrderId() + "</p>" +
                        "<p><strong>Ngày bắt đầu:</strong> " + order2.getStartDate() + "</p>" +
                        "<p><strong>Ngày kết thúc:</strong> " + order2.getEndDate() + "</p>" +
                        "<p><strong>Số ngày thuê:</strong> 30 ngày</p>" +
                        "<h3>Thiết bị thuê:</h3>" +
                        "<ul>" +
                        "<li>2x Máy trạm Lenovo ThinkStation (Lenovo) - Giá/ngày: 300000 - Tiền cọc: 20000000</li>" +
                        "</ul>" +
                        "<p><strong>Tổng tiền thuê:</strong> " + order2.getTotalPrice() + " VNĐ</p>" +
                        "<p><strong>Tiền cọc:</strong> " + order2.getDepositAmount() + " VNĐ</p>")
                .termsAndConditions("ĐIỀU KHOẢN VÀ ĐIỀU KIỆN THUÊ THIẾT BỊ\n\n" +
                        "1. Khách hàng có trách nhiệm bảo quản thiết bị trong thời gian thuê.\n" +
                        "2. Nếu thiết bị bị hư hỏng hoặc mất mát, khách hàng sẽ phải chịu chi phí sửa chữa hoặc thay thế.\n" +
                        "3. Tiền cọc sẽ được hoàn trả sau khi thiết bị được kiểm tra và không có hư hỏng.\n" +
                        "4. Khách hàng phải trả lại thiết bị đúng hạn, nếu quá hạn sẽ bị phạt 10% giá trị thiết bị mỗi ngày.\n" +
                        "5. Mọi tranh chấp sẽ được giải quyết theo pháp luật Việt Nam.\n")
                .rentalPeriodDays(30)
                .totalAmount(order2.getTotalPrice())
                .depositAmount(order2.getDepositAmount())
                .startDate(order2.getStartDate())
                .endDate(order2.getEndDate())
                .expiresAt(order2.getEndDate().plusDays(7))
                .createdBy(customer.getAccount().getAccountId())
                .build();

        // Contract 3: SIGNED
        Contract contract3 = Contract.builder()
                .contractNumber("HD" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0003")
                .title("Hợp đồng thuê máy MacBook Pro")
                .description("Hợp đồng thuê máy MacBook Pro M2 cho dự án phát triển ứng dụng di động")
                .contractType(ContractType.EQUIPMENT_RENTAL)
                .status(ContractStatus.SIGNED)
                .customerId(customer.getCustomerId())
                .contractContent("<h2>HỢP ĐỒNG THUÊ THIẾT BỊ CÔNG NGHỆ</h2>" +
                        "<p><strong>Thiết bị:</strong> MacBook Pro M2</p>" +
                        "<p><strong>Thời gian thuê:</strong> 60 ngày</p>" +
                        "<p><strong>Tổng tiền:</strong> 12000000 VNĐ</p>")
                .termsAndConditions("Điều khoản và điều kiện hợp đồng thuê thiết bị.")
                .rentalPeriodDays(60)
                .totalAmount(BigDecimal.valueOf(12000000))
                .depositAmount(BigDecimal.valueOf(2400000))
                .startDate(now.minusDays(10))
                .endDate(now.plusDays(50))
                .expiresAt(now.plusDays(57))
                .signedAt(now.minusDays(10))
                .createdBy(customer.getAccount().getAccountId())
                .build();

        contractRepository.saveAll(java.util.List.of(contract1, contract2, contract3));
        
        log.info("Created 3 sample contracts with statuses: DRAFT, PENDING_SIGNATURE, SIGNED");
    }
}


