package com.hotel.service.impl;

import com.hotel.dto.request.BookingRequest;
import com.hotel.dto.response.BookingResponse;
import com.hotel.dto.response.PageResponse;
import com.hotel.entity.Booking;
import com.hotel.entity.Room;
import com.hotel.entity.User;
import com.hotel.enums.BookingStatus;
import com.hotel.enums.RoomStatus;
import com.hotel.exception.BadRequestException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.exception.RoomNotAvailableException;
import com.hotel.mapper.BookingMapper;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.UserRepository;
import com.hotel.service.EmailService;
import com.hotel.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RoomServiceImpl roomService;
    private final BookingMapper bookingMapper;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getUserBookings(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(bookingRepository.findByUserId(userId, pageable).map(bookingMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getAllBookings(BookingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null) {
            return PageResponse.from(bookingRepository.findByStatus(status, pageable).map(bookingMapper::toResponse));
        }
        return PageResponse.from(bookingRepository.findAll(pageable).map(bookingMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        return bookingMapper.toResponse(findBookingById(id));
    }


     // Creates a booking with pessimistic write lock to prevent double-booking.
     // The transaction holds the lock until commit, preventing race conditions.

    @Transactional
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BadRequestException("Check-out date must be after check-in date");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Room room = roomService.findRoomById(request.getRoomId());

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() + " is under maintenance");
        }

        // Pessimistic lock — blocks concurrent booking attempts for same room/dates
        List<Booking> overlapping = bookingRepository.findOverlappingBookingsWithLock(
                room.getId(), request.getCheckInDate(), request.getCheckOutDate());

        if (!overlapping.isEmpty()) {
            throw new RoomNotAvailableException(
                "Room " + room.getRoomNumber() + " is not available for the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalPrice = room.getPrice().multiply(BigDecimal.valueOf(nights));

        Booking booking = Booking.builder()
                .user(user)
                .room(room)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .guestsCount(request.getGuestsCount())
                .specialRequests(request.getSpecialRequests())
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: {} for user {}", saved.getId(), userId);

        // Async notifications
        emailService.sendBookingConfirmation(user.getEmail(), saved);
        notificationService.sendToUser(user.getUsername(), "BOOKING_CREATED",
                "Your booking #" + saved.getId() + " is confirmed!", bookingMapper.toResponse(saved));

        return bookingMapper.toResponse(saved);
    }

    @CacheEvict(value = "reports", allEntries = true)
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId, boolean isAdmin) {
        Booking booking = isAdmin
                ? findBookingById(bookingId)
                : bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.CHECKED_IN ||
                booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a booking that is already checked in or completed");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        if (booking.getRoom().getStatus() == RoomStatus.OCCUPIED) {
            roomService.updateRoomStatus(booking.getRoom().getId(), RoomStatus.AVAILABLE);
        }

        // Broadcast booking update so admin/staff panels refresh in real time
        notificationService.broadcastBookingUpdate(bookingMapper.toResponse(booking));

        emailService.sendBookingCancellation(booking.getUser().getEmail(), booking);

        // Admin cancelled → notify the guest
        if (isAdmin) {
            notificationService.sendToUser(booking.getUser().getUsername(), "BOOKING_CANCELLED",
                    "Your booking #" + bookingId + " has been cancelled by the hotel.", null);
        }
        // User cancelled themselves → no notification needed, they know

        // Broadcast room availability update for real-time home page
        notificationService.broadcastRoomUpdate(booking.getRoom().getId());

        log.info("Booking {} cancelled by userId {}", bookingId, userId);
        return bookingMapper.toResponse(booking);
    }

    @CacheEvict(value = "reports", allEntries = true)
    @Transactional
    public BookingResponse checkIn(Long bookingId) {
        Booking booking = findBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed bookings can be checked in");
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        bookingRepository.save(booking);
        roomService.updateRoomStatus(booking.getRoom().getId(), RoomStatus.OCCUPIED);
        notificationService.sendToUser(booking.getUser().getUsername(), "CHECKED_IN",
                "You have checked into room " + booking.getRoom().getRoomNumber(), null);
        notificationService.broadcastBookingUpdate(bookingMapper.toResponse(booking));
        return bookingMapper.toResponse(booking);
    }

    @CacheEvict(value = "reports", allEntries = true)
    @Transactional
    public BookingResponse checkOut(Long bookingId) {
        Booking booking = findBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new BadRequestException("Only checked-in bookings can be checked out");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        roomService.updateRoomStatus(booking.getRoom().getId(), RoomStatus.AVAILABLE);
        notificationService.sendToUser(booking.getUser().getUsername(), "CHECKED_OUT",
                "Thank you for staying with us! We hope to see you again.", null);
        notificationService.broadcastBookingUpdate(bookingMapper.toResponse(booking));
        return bookingMapper.toResponse(booking);
    }

    @CacheEvict(value = "reports", allEntries = true)
    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = findBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    public Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }
}
