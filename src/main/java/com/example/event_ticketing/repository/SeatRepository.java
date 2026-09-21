package com.example.event_ticketing.repository;

import com.example.event_ticketing.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findAllByFloorPlanFloorPlanIdOrderByYAscXAsc(UUID floorPlanId);
    void deleteAllByFloorPlanFloorPlanId(UUID floorPlanId);
}
