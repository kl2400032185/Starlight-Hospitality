package com.starlight.roomservice.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Represents a date range during which a room is NOT available.
 * In Phase 5 (Room Service only) these blocks can be created manually
 * or via an internal API. In later phases, Booking Service will call
 * Room Service to create a block whenever a booking is confirmed,
 * and remove it if a booking is cancelled.
 */
@Entity
@Table(name = "room_availability_blocks")
public class RoomAvailabilityBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "reference", length = 100)
    private String reference; // e.g. future booking ID reference

    public RoomAvailabilityBlock() {
    }

    public RoomAvailabilityBlock(Long id, Long roomId, LocalDate checkIn, LocalDate checkOut, String reference) {
        this.id = id;
        this.roomId = roomId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.reference = reference;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
