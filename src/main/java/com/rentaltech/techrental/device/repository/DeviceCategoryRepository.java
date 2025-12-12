package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.DeviceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceCategoryRepository extends JpaRepository<DeviceCategory, Long> {

    @Query("""
            SELECT dc FROM DeviceCategory dc
            WHERE (:deviceCategoryName IS NULL OR dc.deviceCategoryName ILIKE CONCAT('%', COALESCE(CAST(:deviceCategoryName AS string), ''), '%'))
              AND (:isActive IS NULL OR dc.isActive = :isActive)
            """)
    Page<DeviceCategory> searchCategories(@Param("deviceCategoryName") String deviceCategoryName,
                                          @Param("isActive") Boolean isActive,
                                          Pageable pageable);
}
