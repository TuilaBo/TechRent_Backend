package com.rentaltech.techrental.config;

import com.rentaltech.techrental.device.model.*;
import com.rentaltech.techrental.device.repository.AccessoryCategoryRepository;
import com.rentaltech.techrental.device.repository.AccessoryRepository;
import com.rentaltech.techrental.device.repository.DeviceCategoryRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataBootstrapInitializer implements ApplicationRunner {

    private final DeviceCategoryRepository deviceCategoryRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final DeviceRepository deviceRepository;
    private final AccessoryCategoryRepository accessoryCategoryRepository;
    private final AccessoryRepository accessoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1) Check tables: if any has data, abort startup
        long dc = deviceCategoryRepository.count();
        long dm = deviceModelRepository.count();
        long d = deviceRepository.count();
        long ac = accessoryCategoryRepository.count();
        long a = accessoryRepository.count();

        if (dc > 0 || dm > 0 || d > 0 || ac > 0 || a > 0) {
            String msg = String.format(
                    "Bootstrap aborted: existing data detected [DeviceCategory=%d, DeviceModel=%d, Device=%d, AccessoryCategory=%d, Accessory=%d]",
                    dc, dm, d, ac, a
            );
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // 2) Seed data as requested

        // 2.1) 5 DeviceCategory
        List<DeviceCategory> categories = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            categories.add(DeviceCategory.builder()
                    .deviceCategoryName("Danh mục thiết bị " + i)
                    .description("Mô tả danh mục thiết bị " + i)
                    .isActive(true)
                    .build());
        }
        categories = deviceCategoryRepository.saveAll(categories);

        // 2.2) 10 DeviceModel (2 models per category)
        List<DeviceModel> models = new ArrayList<>();
        int modelIndex = 1;
        for (int ci = 0; ci < categories.size(); ci++) {
            DeviceCategory cat = categories.get(ci);
            for (int k = 1; k <= 2; k++) {
                models.add(DeviceModel.builder()
                        .deviceName("Mẫu thiết bị " + modelIndex)
                        .brand("Thương hiệu " + ((ci % 3) + 1))
                        .imageURL("https://example.com/images/model-" + modelIndex + ".png")
                        .specifications("Thông số kỹ thuật cho mẫu " + modelIndex)
                        .isActive(true)
                        .deviceCategory(cat)
                        .build());
                modelIndex++;
            }
        }
        models = deviceModelRepository.saveAll(models);

        // 2.3) 25 Device (belonging to the 10 models)
        // Distribute: first 5 models -> 2 devices each; last 5 models -> 3 devices each (5*2 + 5*3 = 25)
        List<Device> devices = new ArrayList<>();
        int snCounter = 1;
        for (int mi = 0; mi < models.size(); mi++) {
            DeviceModel model = models.get(mi);
            int countForModel = (mi < 5) ? 2 : 3;
            for (int n = 0; n < countForModel; n++) {
                devices.add(Device.builder()
                        .serialNumber(String.format("SN-%05d", snCounter++))
                        .acquireAt(LocalDateTime.now().minusDays((mi + 1) * (n + 1)))
                        .status(DeviceStatus.AVAILABLE)
                        .shelfCode(String.format("K%d-%c", (mi + 1), 'A' + n))
                        .deviceModel(model)
                        .build());
            }
        }
        devices = deviceRepository.saveAll(devices);

        // 2.4) 10 AccessoryCategory
        List<AccessoryCategory> accessoryCategories = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            accessoryCategories.add(AccessoryCategory.builder()
                    .accessoryCategoryName("Danh mục phụ kiện " + i)
                    .description("Mô tả danh mục phụ kiện " + i)
                    .isActive(true)
                    .build());
        }
        accessoryCategories = accessoryCategoryRepository.saveAll(accessoryCategories);

        // 2.5) 20 Accessory (mapped to created AccessoryCategory and DeviceModel)
        // 2 accessories per accessory category; round-robin link to models
        List<Accessory> accessories = new ArrayList<>();
        int accIdx = 1;
        for (int i = 0; i < accessoryCategories.size(); i++) {
            AccessoryCategory accCat = accessoryCategories.get(i);
            for (int k = 0; k < 2; k++) {
                DeviceModel linkedModel = models.get((i * 2 + k) % models.size());
                accessories.add(Accessory.builder()
                        .accessoryName("Phụ kiện " + accIdx)
                        .description("Mô tả phụ kiện " + accIdx)
                        .imageUrl("https://example.com/images/accessory-" + accIdx + ".png")
                        .isActive(true)
                        .accessoryCategory(accCat)
                        .deviceModel(linkedModel)
                        .build());
                accIdx++;
            }
        }
        accessoryRepository.saveAll(accessories);

        log.info("Bootstrap completed: seeded 5 DeviceCategory, 10 DeviceModel, 25 Device, 10 AccessoryCategory, 20 Accessory.");
    }
}
