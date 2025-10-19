package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.TaskCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskCategoryRepository extends JpaRepository<TaskCategory, Long> {
    
    Optional<TaskCategory> findByName(String name);
    
    List<TaskCategory> findByNameContainingIgnoreCase(String name);
    
    boolean existsByName(String name);


}
