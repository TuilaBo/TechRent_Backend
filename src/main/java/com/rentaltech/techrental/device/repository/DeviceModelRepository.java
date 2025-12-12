package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.DeviceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceModelRepository extends JpaRepository<DeviceModel, Long> {
    java.util.List<DeviceModel> findByDeviceCategory_DeviceCategoryId(Long deviceCategoryId);

    @Query("""
            SELECT dm FROM DeviceModel dm
            WHERE (:deviceName IS NULL OR dm.deviceName ILIKE CONCAT('%', COALESCE(CAST(:deviceName AS string), ''), '%'))
              AND (:brandId IS NULL OR dm.brand.brandId = :brandId)
              AND (:amountAvailable IS NULL OR dm.amountAvailable = :amountAvailable)
              AND (:deviceCategoryId IS NULL OR dm.deviceCategory.deviceCategoryId = :deviceCategoryId)
              AND (:pricePerDay IS NULL OR dm.pricePerDay = :pricePerDay)
              AND (:isActive IS NULL OR dm.isActive = :isActive)
            """)
    Page<DeviceModel> searchDeviceModels(@Param("deviceName") String deviceName,
                                         @Param("brandId") Long brandId,
                                         @Param("amountAvailable") Long amountAvailable,
                                         @Param("deviceCategoryId") Long deviceCategoryId,
                                         @Param("pricePerDay") java.math.BigDecimal pricePerDay,
                                         @Param("isActive") Boolean isActive,
                                         Pageable pageable);
}
