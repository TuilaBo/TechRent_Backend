package com.rentaltech.techrental.config;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.device.model.Brand;
import com.rentaltech.techrental.device.model.ConditionDefinition;
import com.rentaltech.techrental.device.model.ConditionSeverity;
import com.rentaltech.techrental.device.model.ConditionType;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceCondition;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.device.model.DeviceStatus;
import com.rentaltech.techrental.device.repository.BrandRepository;
import com.rentaltech.techrental.device.repository.ConditionDefinitionRepository;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceConditionRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.model.TaskRule;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.staff.repository.TaskRuleRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.CustomerStatus;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class InitialDataSeeder implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "string";
    private static final int DEVICES_PER_MODEL = 20;
    private static final Map<TaskCategoryType, Integer> DEFAULT_TASK_LIMITS = new EnumMap<>(TaskCategoryType.class);

    static {
        DEFAULT_TASK_LIMITS.put(TaskCategoryType.PRE_RENTAL_QC, 4);
        DEFAULT_TASK_LIMITS.put(TaskCategoryType.POST_RENTAL_QC, 4);
        DEFAULT_TASK_LIMITS.put(TaskCategoryType.DELIVERY, 8);
        DEFAULT_TASK_LIMITS.put(TaskCategoryType.PICK_UP_RENTAL_ORDER, 6);
        DEFAULT_TASK_LIMITS.put(TaskCategoryType.MAINTENANCE, 5);
        DEFAULT_TASK_LIMITS.put(TaskCategoryType.SETTLEMENT, 10);
    }

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceCategoryRepository deviceCategoryRepository;
    private final BrandRepository brandRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final DeviceRepository deviceRepository;
    private final ConditionDefinitionRepository conditionDefinitionRepository;
    private final DeviceConditionRepository deviceConditionRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final TaskRuleRepository taskRuleRepository;

    private final Map<Long, ConditionDefinition> goodConditionCache = new HashMap<>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAccounts();
        seedTaskRules();
        seedDeviceData();
    }

    private void seedAccounts() {
        if (accountRepository.count() > 0) {
            log.info("Skip account seeding because table already has data");
            return;
        }
        List<AccountSeed> seeds = List.of(
                new AccountSeed("admin123", "admin@yopmail.com", Role.ADMIN),
                new AccountSeed("operator123", "operator@yopmail.com", Role.OPERATOR),
                new AccountSeed("technician123", "technician@yopmail.com", Role.TECHNICIAN),
                new AccountSeed("support123", "customersupportstaff@yopmail.com", Role.CUSTOMER_SUPPORT_STAFF),
                new AccountSeed("customer123", "customer@yopmail.com", Role.CUSTOMER)
        );

        for (AccountSeed seed : seeds) {
            Account existing = accountRepository.findByUsername(seed.username());
            if (existing != null) {
                continue;
            }
            Account account = Account.builder()
                    .username(seed.username())
                    .email(seed.email())
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .role(seed.role())
                    .isActive(true)
                    .build();
            Account saved = accountRepository.save(account);
            attachProfileForRole(saved);
            log.info("Seeded account {} for role {}", seed.username(), seed.role());
        }
    }

    private void seedTaskRules() {
        LocalDateTime now = LocalDateTime.now();
        for (TaskCategoryType category : TaskCategoryType.values()) {
            if (taskRuleRepository.findByTaskCategory(category).isPresent()) {
                continue;
            }
            int limit = DEFAULT_TASK_LIMITS.getOrDefault(category, 5);
            TaskRule rule = TaskRule.builder()
                    .name("Quy định " + category.getName())
                    .description(category.getDescription() + " - tối đa " + limit + " công việc/ngày.")
                    .maxTasksPerDay(limit)
                    .active(true)
                    .taskCategory(category)
                    .staffRole(null)
                    .effectiveFrom(now)
                    .createdBy("initial-seeder")
                    .build();
            taskRuleRepository.save(rule);
            log.info("Seeded task rule cho category {}", category.getName());
        }
    }

    private void attachProfileForRole(Account account) {
        Role role = account.getRole();
        if (role == Role.CUSTOMER) {
            if (customerRepository.existsByAccount_AccountId(account.getAccountId())) {
                return;
            }
            Customer customer = Customer.builder()
                    .account(account)
                    .fullName(capitalize(account.getUsername()))
                    .email(account.getEmail())
                    .status(CustomerStatus.ACTIVE)
                    .build();
            customerRepository.save(customer);
        } else {
            if (staffRepository.findByAccount_AccountId(account.getAccountId()).isPresent()) {
                return;
            }
            StaffRole staffRole = switch (role) {
                case ADMIN -> StaffRole.ADMIN;
                case OPERATOR -> StaffRole.OPERATOR;
                case TECHNICIAN -> StaffRole.TECHNICIAN;
                case CUSTOMER_SUPPORT_STAFF -> StaffRole.CUSTOMER_SUPPORT_STAFF;
                default -> null;
            };
            if (staffRole == null) {
                return;
            }
            Staff staff = Staff.builder()
                    .account(account)
                    .staffRole(staffRole)
                    .isActive(true)
                    .build();
            staffRepository.save(staff);
        }
    }

    private void seedDeviceData() {
        if (deviceCategoryRepository.count() > 0 || deviceModelRepository.count() > 0 || deviceRepository.count() > 0) {
            log.info("Skip device data seeding because related tables already have data");
            return;
        }
        Map<String, DeviceCategory> categoryMap = seedDeviceCategories();
        Map<String, Brand> brandMap = new HashMap<>();

        Map<String, List<DeviceModelSeed>> modelsByCategory = buildModelBlueprints();
        for (Map.Entry<String, List<DeviceModelSeed>> entry : modelsByCategory.entrySet()) {
            DeviceCategory category = categoryMap.get(entry.getKey());
            if (category == null) {
                continue;
            }
            for (DeviceModelSeed modelSeed : entry.getValue()) {
                Brand brand = brandMap.computeIfAbsent(modelSeed.brandName(), this::ensureBrand);
                seedDeviceModelAndDevices(category, brand, modelSeed);
            }
        }
    }

    private Map<String, DeviceCategory> seedDeviceCategories() {
        List<CategorySeed> categories = List.of(
                new CategorySeed("Smartphones", "Thiết bị di động phục vụ nhu cầu công việc."),
                new CategorySeed("Laptops", "Máy tính xách tay cho nhân sự và khách hàng."),
                new CategorySeed("Tablets", "Máy tính bảng phục vụ ký hợp đồng và trình diễn."),
                new CategorySeed("Cameras", "Thiết bị quay chụp cho các sự kiện."),
                new CategorySeed("Wearables", "Thiết bị đeo hỗ trợ theo dõi sức khỏe.")
        );
        Map<String, DeviceCategory> result = new java.util.HashMap<>();
        for (CategorySeed seed : categories) {
            DeviceCategory category = deviceCategoryRepository.findByDeviceCategoryNameIgnoreCase(seed.name())
                .orElseGet(() -> deviceCategoryRepository.save(DeviceCategory.builder()
                        .deviceCategoryName(seed.name())
                        .description(seed.description())
                        .isActive(true)
                        .build()));
            result.put(seed.name(), category);
        }
        return result;
    }

    private Brand ensureBrand(String brandName) {
        String description = brandDescription(brandName);
        return brandRepository.findByBrandNameIgnoreCase(brandName)
                .orElseGet(() -> brandRepository.save(Brand.builder()
                        .brandName(brandName)
                        .description(description)
                        .isActive(true)
                        .build()));
    }

    private Map<String, List<DeviceModelSeed>> buildModelBlueprints() {
        Map<String, List<DeviceModelSeed>> modelsByCategory = new java.util.HashMap<>();
        modelsByCategory.put("Smartphones", List.of(
                new DeviceModelSeed(
                        "iPhone 15 Pro",
                        "Apple",
                        "Flagship iPhone 2023 với chip A17 Pro",
                        "Chip A17 Pro, màn hình 6.1\" ProMotion, camera 48MP, USB-C",
                        new BigDecimal("28800000"),
                        new BigDecimal("1320000"),
                        new BigDecimal("0.20")),
                new DeviceModelSeed(
                        "Galaxy S23 Ultra",
                        "Samsung",
                        "Flagship Galaxy với camera 200MP",
                        "Snapdragon 8 Gen 2, màn hình 6.8\" 120Hz, camera 200MP, S Pen",
                        new BigDecimal("26400000"),
                        new BigDecimal("1200000"),
                        new BigDecimal("0.18"))
        ));
        modelsByCategory.put("Laptops", List.of(
                new DeviceModelSeed(
                        "MacBook Pro 14\" M3",
                        "Apple",
                        "Laptop cao cấp cho nhà sáng tạo",
                        "Chip Apple M3 Pro, màn hình Liquid Retina XDR 14.2\", 18GB RAM, SSD 512GB",
                        new BigDecimal("57600000"),
                        new BigDecimal("2160000"),
                        new BigDecimal("0.25")),
                new DeviceModelSeed(
                        "Dell XPS 13",
                        "Dell",
                        "Laptop doanh nhân mỏng nhẹ",
                        "Intel Core i7 13th Gen, màn 13.4\" InfinityEdge, 16GB RAM, SSD 512GB",
                        new BigDecimal("43200000"),
                        new BigDecimal("1680000"),
                        new BigDecimal("0.22"))
        ));
        modelsByCategory.put("Tablets", List.of(
                new DeviceModelSeed(
                        "iPad Pro 12.9\"",
                        "Apple",
                        "Tablet cao cấp hỗ trợ Apple Pencil",
                        "Chip M2, màn Liquid Retina XDR 12.9\", hỗ trợ Pencil 2 & Magic Keyboard",
                        new BigDecimal("31200000"),
                        new BigDecimal("1080000"),
                        new BigDecimal("0.18")),
                new DeviceModelSeed(
                        "Galaxy Tab S9",
                        "Samsung",
                        "Tablet Android hỗ trợ S Pen",
                        "Snapdragon 8 Gen 2, màn Dynamic AMOLED 11\", IP68, S Pen kèm theo",
                        new BigDecimal("23976000"),
                        new BigDecimal("912000"),
                        new BigDecimal("0.17"))
        ));
        modelsByCategory.put("Cameras", List.of(
                new DeviceModelSeed(
                        "Sony Alpha A7 IV",
                        "Sony",
                        "Máy ảnh mirrorless full-frame",
                        "Cảm biến 33MP full-frame, quay 4K60p, IBIS 5 trục",
                        new BigDecimal("60000000"),
                        new BigDecimal("2040000"),
                        new BigDecimal("0.24")),
                new DeviceModelSeed(
                        "Canon EOS R6",
                        "Canon",
                        "Máy ảnh mirrorless phục vụ sự kiện",
                        "Cảm biến 20MP full-frame, Dual Pixel AF II, quay 4K60p",
                        new BigDecimal("55200000"),
                        new BigDecimal("1920000"),
                        new BigDecimal("0.22"))
        ));
        modelsByCategory.put("Wearables", List.of(
                new DeviceModelSeed(
                        "Apple Watch Ultra 2",
                        "Apple",
                        "Đồng hồ thể thao cao cấp chống nước",
                        "Vỏ titan 49mm, màn Retina 3.000 nit, GPS băng tần kép, WR100",
                        new BigDecimal("19176000"),
                        new BigDecimal("672000"),
                        new BigDecimal("0.15")),
                new DeviceModelSeed(
                        "Garmin Forerunner 965",
                        "Garmin",
                        "Đồng hồ GPS chuyên chạy bộ",
                        "Màn AMOLED 1.4\", GPS đa băng tần, pin 23 ngày, Training Readiness",
                        new BigDecimal("14376000"),
                        new BigDecimal("576000"),
                        new BigDecimal("0.13"))
        ));
        return modelsByCategory;
    }

    private void seedDeviceModelAndDevices(DeviceCategory category, Brand brand, DeviceModelSeed modelSeed) {
        Optional<DeviceModel> existingModel = deviceModelRepository.findByDeviceNameIgnoreCase(modelSeed.name());
        DeviceModel model = existingModel.orElseGet(() -> deviceModelRepository.save(DeviceModel.builder()
                .deviceName(modelSeed.name())
                .brand(brand)
                .deviceCategory(category)
                .description(modelSeed.description())
                .imageURL(null)
                .specifications(modelSeed.specifications())
                .deviceValue(modelSeed.deviceValue())
                .pricePerDay(modelSeed.pricePerDay())
                .depositPercent(modelSeed.depositPercent())
                .amountAvailable((long) DEVICES_PER_MODEL)
                .isActive(true)
                .build()));

        ConditionDefinition goodConditionDefinition = ensureGoodConditionDefinition(model);
        seedDevicesForModel(model, goodConditionDefinition);
    }

    private void seedDevicesForModel(DeviceModel model, ConditionDefinition goodConditionDefinition) {
        long currentCount = deviceRepository.countByDeviceModel_DeviceModelId(model.getDeviceModelId());
        if (currentCount >= DEVICES_PER_MODEL) {
            return;
        }
        List<Device> newDevices = new ArrayList<>();
        for (int i = (int) currentCount; i < DEVICES_PER_MODEL; i++) {
            String serial = generateSerial(model.getDeviceName(), i + 1);
            if (deviceRepository.findBySerialNumber(serial).isPresent()) {
                continue;
            }
            newDevices.add(Device.builder()
                    .serialNumber(serial)
                    .deviceModel(model)
                    .status(DeviceStatus.AVAILABLE)
                    .acquireAt(LocalDateTime.now().minusDays(i))
                    .usageCount(0)
                    .build());
        }
        if (!newDevices.isEmpty()) {
            List<Device> savedDevices = deviceRepository.saveAll(newDevices);
            createInitialDeviceConditions(savedDevices, goodConditionDefinition);
            log.info("Seeded {} devices for model {}", newDevices.size(), model.getDeviceName());
        }
    }

    private String generateSerial(String modelName, int index) {
        String sanitized = modelName.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (sanitized.length() < 4) {
            sanitized = (sanitized + "MODEL").substring(0, 5);
        }
        return sanitized + "-" + String.format("%03d", index);
    }

    private ConditionDefinition ensureGoodConditionDefinition(DeviceModel model) {
        if (model == null || model.getDeviceModelId() == null) {
            return null;
        }
        return goodConditionCache.computeIfAbsent(model.getDeviceModelId(), id -> {
            Optional<ConditionDefinition> existingGood = conditionDefinitionRepository
                    .findFirstByDeviceModel_DeviceModelIdAndConditionType(id, ConditionType.GOOD);
            if (existingGood.isPresent()) {
                return existingGood.get();
            }
            List<ConditionDefinition> existing = conditionDefinitionRepository.findByDeviceModel_DeviceModelId(id);
            if (existing != null && !existing.isEmpty()) {
                Optional<ConditionDefinition> detected = existing.stream()
                        .filter(def -> def.getConditionType() == ConditionType.GOOD)
                        .findFirst();
                if (detected.isPresent()) {
                    return detected.get();
                }
            }
            List<ConditionDefinition> blueprint = buildDefaultConditionDefinitions(model);
            List<ConditionDefinition> toPersist = new ArrayList<>();
            for (ConditionDefinition definition : blueprint) {
                if (!conditionDefinitionRepository.existsByNameIgnoreCase(definition.getName())) {
                    toPersist.add(definition);
                }
            }
            List<ConditionDefinition> persisted = toPersist.isEmpty()
                    ? conditionDefinitionRepository.findByDeviceModel_DeviceModelId(id)
                    : conditionDefinitionRepository.saveAll(toPersist);
            if (!toPersist.isEmpty()) {
                log.info("Seeded {} condition definitions for model {}", toPersist.size(), model.getDeviceName());
            }
            return persisted.stream()
                    .filter(def -> def.getConditionType() == ConditionType.GOOD)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy condition GOOD cho model " + model.getDeviceName()));
        });
    }

    private List<ConditionDefinition> buildDefaultConditionDefinitions(DeviceModel model) {
        BigDecimal deviceValue = Optional.ofNullable(model.getDeviceValue()).orElse(BigDecimal.ZERO);
        return List.of(
                buildConditionDefinition(
                        model,
                        model.getDeviceName() + " - Chuẩn showroom",
                        "Thiết bị hoạt động hoàn hảo, không trầy xước hay lỗi nhỏ.",
                        ConditionType.GOOD,
                        ConditionSeverity.INFO,
                        BigDecimal.ZERO),
                buildConditionDefinition(
                        model,
                        model.getDeviceName() + " - Trầy xước nhẹ",
                        "Một vài vết trầy xước nhỏ không ảnh hưởng công năng.",
                        ConditionType.DAMAGED,
                        ConditionSeverity.LOW,
                        percentageOf(deviceValue, 0.05)),
                buildConditionDefinition(
                        model,
                        model.getDeviceName() + " - Hư hỏng vừa",
                        "Lỗi về ngoại hình hoặc tính năng cần sửa chữa trung bình.",
                        ConditionType.DAMAGED,
                        ConditionSeverity.MEDIUM,
                        percentageOf(deviceValue, 0.15)),
                buildConditionDefinition(
                        model,
                        model.getDeviceName() + " - Hư hỏng nặng",
                        "Thiết bị không thể sử dụng, cần thay thế linh kiện chính.",
                        ConditionType.DAMAGED,
                        ConditionSeverity.HIGH,
                        percentageOf(deviceValue, 0.4)),
                buildConditionDefinition(
                        model,
                        model.getDeviceName() + " - Mất thiết bị",
                        "Thiết bị thất lạc hoặc hư hại không thể phục hồi.",
                        ConditionType.LOST,
                        ConditionSeverity.CRITICAL,
                        deviceValue.setScale(0, RoundingMode.HALF_UP))
        );
    }

    private void createInitialDeviceConditions(List<Device> devices, ConditionDefinition definition) {
        if (devices == null || devices.isEmpty() || definition == null) {
            return;
        }
        List<DeviceCondition> records = new ArrayList<>();
        for (Device device : devices) {
            records.add(DeviceCondition.builder()
                    .device(device)
                    .conditionDefinition(definition)
                    .severity(ConditionSeverity.INFO.name())
                    .note("Thiết bị ở tình trạng tốt khi xuất kho")
                    .images(new ArrayList<>())
                    .build());
        }
        deviceConditionRepository.saveAll(records);
    }

    private ConditionDefinition buildConditionDefinition(DeviceModel model,
                                                         String name,
                                                         String description,
                                                         ConditionType type,
                                                         ConditionSeverity severity,
                                                         BigDecimal compensation) {
        return ConditionDefinition.builder()
                .deviceModel(model)
                .name(name)
                .description(description)
                .conditionType(type)
                .conditionSeverity(severity)
                .defaultCompensation(compensation)
                .build();
    }

    private BigDecimal percentageOf(BigDecimal base, double ratio) {
        if (base == null) {
            return BigDecimal.ZERO;
        }
        return base.multiply(BigDecimal.valueOf(ratio)).setScale(0, RoundingMode.HALF_UP);
    }

    private String capitalize(String username) {
        if (username == null || username.isBlank()) {
            return "Seed User";
        }
        return username.substring(0, 1).toUpperCase(Locale.ROOT) + username.substring(1);
    }
    
    private String brandDescription(String brandName) {
        return switch (brandName.toLowerCase(Locale.ROOT)) {
            case "apple" -> "Tập đoàn công nghệ Hoa Kỳ nổi tiếng với iPhone, iPad, MacBook và Apple Watch.";
            case "samsung" -> "Nhà sản xuất Hàn Quốc dẫn đầu về smartphone, tablet và thiết bị gia dụng.";
            case "dell" -> "Thương hiệu Mỹ chuyên về máy tính doanh nghiệp, gaming và máy trạm.";
            case "sony" -> "Công ty Nhật Bản nổi tiếng với máy ảnh Alpha, TV Bravia và PlayStation.";
            case "canon" -> "Hãng Nhật chuyên sản xuất máy ảnh, máy quay và thiết bị văn phòng.";
            case "garmin" -> "Nhà sản xuất thiết bị GPS và wearables cho thể thao, hàng hải và hàng không.";
            default -> "Thương hiệu thiết bị được sử dụng trong hệ thống TechRent.";
        };
    }

    private record AccountSeed(String username, String email, Role role) {
    }

    private record CategorySeed(String name, String description) {
    }

    private record DeviceModelSeed(String name, String brandName, String description, String specifications,
                                   BigDecimal deviceValue, BigDecimal pricePerDay, BigDecimal depositPercent) {
    }
}
