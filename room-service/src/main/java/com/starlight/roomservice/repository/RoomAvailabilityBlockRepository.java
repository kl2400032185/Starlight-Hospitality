package com.starlight.roomservice.repository;

import com.starlight.roomservice.entity.RoomAvailabilityBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomAvailabilityBlockRepository extends JpaRepository<RoomAvailabilityBlock, Long> {

    /**
     * Returns overlapping blocks for a room using standard date-range overlap logic:
     * existing.checkIn < requested.checkOut AND existing.checkOut > requested.checkIn
     */
    @Query("""
            SELECT b FROM RoomAvailabilityBlock b
            WHERE b.roomId = :roomId
            AND b.checkIn < :checkOut
            AND b.checkOut > :checkIn
            """)
    List<RoomAvailabilityBlock> findOverlapping(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    List<RoomAvailabilityBlock> findByRoomId(Long roomId);
}
