package com.starlight.booking.repository;

import com.starlight.booking.entity.Booking;
import com.starlight.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT b FROM Booking b
        WHERE b.roomId = :roomId
        AND b.status = :status
        AND b.checkIn < :checkOut
        AND b.checkOut > :checkIn
        """)
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("status") BookingStatus status
    );

    List<Booking> findByUserId(Long userId);
}