package com.hotel.controller;

import com.hotel.dto.response.*;
import com.hotel.enums.BookingStatus;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.impl.BookingServiceImpl;
import com.hotel.service.impl.ReportServiceImpl;
import com.hotel.mapper.UserMapper;
import com.hotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final BookingServiceImpl bookingService;
    private final ReportServiceImpl reportService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getAllBookings(status, page, size)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var usersPage = (search != null && !search.isBlank())
                ? userRepository.searchUsers(search, pageable)
                : userRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(usersPage.map(userMapper::toResponse))));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<ReportResponse>> getReports() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDashboardReport()));
    }

    @PostMapping("/bookings/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed", bookingService.confirmBooking(id)));
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled",
                bookingService.cancelBooking(id, null, true)));
    }
}
