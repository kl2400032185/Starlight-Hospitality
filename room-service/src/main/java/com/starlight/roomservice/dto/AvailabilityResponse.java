package com.starlight.roomservice.dto;

import java.time.LocalDate;

public class AvailabilityResponse {

    private Long roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private boolean available;
    private String message;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(Long roomId, LocalDate checkIn, LocalDate checkOut, boolean available, String message) {
        this.roomId = roomId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.available = available;
        this.message = message;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
