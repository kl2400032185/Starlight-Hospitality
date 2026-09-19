package com.starlight.booking.service;

import com.starlight.booking.client.RoomClient;
import com.starlight.booking.client.RoomResponse;
import com.starlight.booking.dto.BookingRequest;
import com.starlight.booking.dto.BookingResponse;
import com.starlight.booking.entity.Booking;
import com.starlight.booking.entity.BookingStatus;
import com.starlight.booking.exception.BookingConflictException;
import com.starlight.booking.exception.BookingNotFoundException;
import com.starlight.booking.exception.RoomNotFoundException;
import com.starlight.booking.repository.BookingRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final RoomClient roomClient;

    public BookingService(BookingRepository bookingRepository, RoomClient roomClient) {
        this.bookingRepository = bookingRepository;
        this.roomClient = roomClient;
    }

    public BookingResponse createBooking(BookingRequest request) {
        return createBooking(request, request.getUserId());
    }

    public BookingResponse createBooking(BookingRequest request, Long userId) {
        validateBookingRequest(request, userId);
        verifyRoomExists(request.getRoomId());
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                request.getRoomId(), request.getCheckIn(), request.getCheckOut(), BookingStatus.CONFIRMED);
        if (!overlaps.isEmpty()) {
            throw new BookingConflictException("Room is already booked for the selected dates");
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setRoomId(request.getRoomId());
        booking.setCheckIn(request.getCheckIn());
        booking.setCheckOut(request.getCheckOut());
        booking.setGuests(request.getGuests());
        booking.setStatus(BookingStatus.CONFIRMED);
        return convertToResponse(bookingRepository.save(booking));
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::convertToResponse).toList();
    }

    public BookingResponse getBookingById(Long id) {
        return convertToResponse(findBooking(id));
    }

    public List<BookingResponse> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId).stream().map(this::convertToResponse).toList();
    }

    public BookingResponse cancelBooking(Long id) {
        Booking booking = findBooking(id);
        booking.setStatus(BookingStatus.CANCELLED);
        return convertToResponse(bookingRepository.save(booking));
    }

    public BookingResponse getBookingForUser(Long id, Long authenticatedUserId) {
        Booking booking = findBooking(id);
        verifyOwnership(booking, authenticatedUserId, "access");
        return convertToResponse(booking);
    }

    public BookingResponse cancelBookingForUser(Long id, Long authenticatedUserId) {
        Booking booking = findBooking(id);
        verifyOwnership(booking, authenticatedUserId, "cancel");
        booking.setStatus(BookingStatus.CANCELLED);
        return convertToResponse(bookingRepository.save(booking));
    }

    private void validateBookingRequest(BookingRequest request, Long userId) {
        if (request == null || userId == null) throw new IllegalArgumentException("User ID is required");
        if (request.getRoomId() == null) throw new IllegalArgumentException("Room ID is required");
        if (request.getCheckIn() == null || request.getCheckOut() == null) throw new IllegalArgumentException("Check-in and check-out dates are required");
        if (!request.getCheckIn().isBefore(request.getCheckOut())) throw new IllegalArgumentException("Check-in date must be before check-out date");
        if (request.getGuests() == null || request.getGuests() < 1) throw new IllegalArgumentException("Guests must be at least 1");
    }

    private void verifyRoomExists(Long roomId) {
        try {
            RoomResponse room = roomClient.getRoomById(roomId);
            if (room == null || room.getId() == null) throw new RoomNotFoundException("Room not found with ID: " + roomId);
        } catch (FeignException.NotFound ex) {
            throw new RoomNotFoundException("Room not found with ID: " + roomId);
        }
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));
    }

    private void verifyOwnership(Booking booking, Long authenticatedUserId, String action) {
        if (!booking.getUserId().equals(authenticatedUserId)) throw new SecurityException("You cannot " + action + " this booking");
    }

    private BookingResponse convertToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setUserId(booking.getUserId());
        response.setRoomId(booking.getRoomId());
        response.setCheckIn(booking.getCheckIn());
        response.setCheckOut(booking.getCheckOut());
        response.setGuests(booking.getGuests());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }
}
