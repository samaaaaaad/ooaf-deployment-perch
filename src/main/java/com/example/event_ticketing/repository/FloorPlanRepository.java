package com.example.event_ticketing.repository;

import com.example.event_ticketing.model.FloorPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FloorPlanRepository extends JpaRepository<FloorPlan, UUID> {

    @Query("SELECT f FROM FloorPlan f WHERE f.organiser IS NULL OR f.organiser.userId = :organiserId ORDER BY f.createdAt ASC")
    List<FloorPlan> findAvailableForOrganiser(@Param("organiserId") UUID organiserId);

    Optional<FloorPlan> findByFloorPlanId(UUID floorPlanId);
}
