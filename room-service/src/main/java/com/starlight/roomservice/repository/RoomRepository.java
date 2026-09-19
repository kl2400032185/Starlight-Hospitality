package com.starlight.roomservice.repository;

import com.starlight.roomservice.entity.Room;
import com.starlight.roomservice.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByPropertyIdAndRoomNumberIgnoreCase(Long propertyId, String roomNumber);

    List<Room> findByPropertyId(Long propertyId);

    @Query("""
            SELECT r FROM Room r
            WHERE (:propertyId IS NULL OR r.propertyId = :propertyId)
            AND (:roomType IS NULL OR r.roomType = :roomType)
            AND (:capacity IS NULL OR r.capacity >= :capacity)
            AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)
            AND r.active = true
            """)
    List<Room> search(
            @Param("propertyId") Long propertyId,
            @Param("roomType") RoomType roomType,
            @Param("capacity") Integer capacity,
            @Param("maxPrice") BigDecimal maxPrice
    );
}
