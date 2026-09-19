package com.starlight.booking.controller;

import com.starlight.booking.dto.BookingRequest;
import com.starlight.booking.dto.BookingResponse;
import com.starlight.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request, Authentication authentication) {
        Long ownerId = isAdmin(authentication) ? request.getUserId() : authenticatedUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request, ownerId));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookings(@RequestParam(required = false) Long userId, Authentication authentication) {
        if (isAdmin(authentication)) return ResponseEntity.ok(userId == null ? bookingService.getAllBookings() : bookingService.getBookingsByUser(userId));
        return ResponseEntity.ok(bookingService.getBookingsByUser(authenticatedUserId(authentication)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getBookingsForUser(@PathVariable Long userId, Authentication authentication) {
        if (!isAdmin(authentication) && !userId.equals(authenticatedUserId(authentication))) throw new SecurityException("You cannot access another user's bookings");
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(isAdmin(authentication) ? bookingService.getBookingById(id) : bookingService.getBookingForUser(id, authenticatedUserId(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(isAdmin(authentication) ? bookingService.cancelBooking(id) : bookingService.cancelBookingForUser(id, authenticatedUserId(authentication)));
    }

    private Long authenticatedUserId(Authentication authentication) {
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new SecurityException("Invalid authenticated user");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
