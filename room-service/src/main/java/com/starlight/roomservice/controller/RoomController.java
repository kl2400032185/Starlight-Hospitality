package com.starlight.roomservice.controller;

import com.starlight.roomservice.dto.AvailabilityResponse;
import com.starlight.roomservice.dto.RoomRequest;
import com.starlight.roomservice.dto.RoomResponse;
import com.starlight.roomservice.entity.RoomType;
import com.starlight.roomservice.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAll() {
        return ResponseEntity.ok(roomService.getAll());
    }

    // IMPORTANT: /search and /availability must stay above /{id}
    @GetMapping("/search")
    public ResponseEntity<List<RoomResponse>> search(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return ResponseEntity.ok(roomService.search(propertyId, roomType, capacity, maxPrice));
    }

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut
    ) {
        return ResponseEntity.ok(roomService.checkAvailability(roomId, checkIn, checkOut));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
