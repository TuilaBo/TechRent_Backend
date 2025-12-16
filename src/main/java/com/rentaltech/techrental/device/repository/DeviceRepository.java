package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findBySerialNumber(String serialNumber);

    Long countByDeviceModel_DeviceModelId(Long deviceModelId);

    // Find all devices by model for allocation selection
    List<Device> findByDeviceModel_DeviceModelId(Long deviceModelId);

    // Find all devices by device category (via deviceModel.deviceCategory)
    List<Device> findByDeviceModel_DeviceCategory_DeviceCategoryId(Long deviceCategoryId);
    
    // Find devices with usageCount >= threshold
    List<Device> findByUsageCountGreaterThanEqual(Integer usageCount);

    @Query("""
            SELECT d FROM Device d
            WHERE (:serialNumber IS NULL OR d.serialNumber ILIKE CONCAT('%', COALESCE(CAST(:serialNumber AS string), ''), '%'))
              AND (:status IS NULL OR d.status = :status)
              AND (:deviceModelId IS NULL OR d.deviceModel.deviceModelId = :deviceModelId)
              AND (:brand IS NULL OR d.deviceModel.brand.brandName ILIKE CONCAT('%', COALESCE(CAST(:brand AS string), ''), '%'))
              AND (:deviceName IS NULL OR d.deviceModel.deviceName ILIKE CONCAT('%', COALESCE(CAST(:deviceName AS string), ''), '%'))
            """)
    Page<Device> searchDevices(@Param("serialNumber") String serialNumber,
                               @Param("status") DeviceStatus status,
                               @Param("deviceModelId") Long deviceModelId,
                               @Param("brand") String brand,
                               @Param("deviceName") String deviceName,
                               Pageable pageable);

    @Query("""
            SELECT c.deviceCategoryId, c.deviceCategoryName, COUNT(d)
            FROM Device d
                JOIN d.deviceModel m
                JOIN m.deviceCategory c
            WHERE d.acquireAt BETWEEN :start AND :end
            GROUP BY c.deviceCategoryId, c.deviceCategoryName
            """)
    List<Object[]> countImportsByCategoryBetween(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
