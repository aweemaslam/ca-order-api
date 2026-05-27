package com.caorderapi.repository;

import com.caorderapi.model.UpdateStatusRestrictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateStatusRestrictionRepository extends JpaRepository<UpdateStatusRestrictionEntity, Integer> {
    boolean existsByCurrentStatusAndAllowedNextStatusAndActiveTrue(String currentStatus, String allowedNextStatus);
}

