package com.starlight.roomservice.service;

import com.starlight.roomservice.dto.AvailabilityResponse;
import com.starlight.roomservice.dto.RoomRequest;
import com.starlight.roomservice.dto.RoomResponse;
import com.starlight.roomservice.entity.Room;
import com.starlight.roomservice.entity.RoomAvailabilityBlock;
import com.starlight.roomservice.entity.RoomType;
import com.starlight.roomservice.exception.BadRequestException;
import com.starlight.roomservice.exception.DuplicateResourceException;
import com.starlight.roomservice.exception.ResourceNotFoundException;
import com.starlight.roomservice.repository.PropertyRepository;
import com.starlight.roomservice.repository.RoomAvailabilityBlockRepository;
import com.starlight.roomservice.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;
    private final RoomAvailabilityBlockRepository blockRepository;

    public RoomService(RoomRepository roomRepository,
                        PropertyRepository propertyRepository,
                        RoomAvailabilityBlockRepository blockRepository) {
        this.roomRepository = roomRepository;
        this.propertyRepository = propertyRepository;
        this.blockRepository = blockRepository;
    }

    public RoomResponse create(RoomRequest request) {
        if (!propertyRepository.existsById(request.getPropertyId())) {
            throw new ResourceNotFoundException("Property not found with id: " + request.getPropertyId());
        }

        if (roomRepository.existsByPropertyIdAndRoomNumberIgnoreCase(request.getPropertyId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Room number '" + request.getRoomNumber() + "' already exists for property id " + request.getPropertyId());
        }

        Room room = new Room();
        mapRequestToEntity(request, room);

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    public List<RoomResponse> getAll() {
        return roomRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse getById(Long id) {
        return toResponse(findEntityById(id));
    }

    public RoomResponse update(Long id, RoomRequest request) {
        Room room = findEntityById(id);

        if (!propertyRepository.existsById(request.getPropertyId())) {
            throw new ResourceNotFoundException("Property not found with id: " + request.getPropertyId());
        }

        boolean numberChanged = !room.getRoomNumber().equalsIgnoreCase(request.getRoomNumber())
                || !room.getPropertyId().equals(request.getPropertyId());

        if (numberChanged && roomRepository.existsByPropertyIdAndRoomNumberIgnoreCase(
                request.getPropertyId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Room number '" + request.getRoomNumber() + "' already exists for property id " + request.getPropertyId());
        }

        mapRequestToEntity(request, room);

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    public void delete(Long id) {
        Room room = findEntityById(id);
        roomRepository.delete(room);
    }

    public List<RoomResponse> search(Long propertyId, RoomType roomType, Integer capacity, BigDecimal maxPrice) {
        return roomRepository.search(propertyId, roomType, capacity, maxPrice).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AvailabilityResponse checkAvailability(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = findEntityById(roomId);

        if (checkIn == null || checkOut == null) {
            throw new BadRequestException("checkIn and checkOut dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("checkOut date must be after checkIn date");
        }

        if (!room.isActive()) {
            return new AvailabilityResponse(roomId, checkIn, checkOut, false, "Room is inactive");
        }

        List<RoomAvailabilityBlock> overlaps = blockRepository.findOverlapping(roomId, checkIn, checkOut);

        boolean available = overlaps.isEmpty();
        String message = available
                ? "Room is available for the selected dates"
                : "Room is already booked/blocked for part of the selected dates";

        return new AvailabilityResponse(roomId, checkIn, checkOut, available, message);
    }

    private void mapRequestToEntity(RoomRequest request, Room room) {
        room.setPropertyId(request.getPropertyId());
        room.setRoomNumber(request.getRoomNumber());
        room.setRoomType(request.getRoomType());
        room.setPricePerNight(request.getPricePerNight());
        room.setCapacity(request.getCapacity());
        room.setActive(request.getActive() == null || request.getActive());
    }

    private Room findEntityById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getPropertyId(),
                room.getRoomNumber(),
                room.getRoomType(),
                room.getPricePerNight(),
                room.getCapacity(),
                room.isActive()
        );
    }
}
