package com.hotel.repository;

import com.hotel.entity.Booking;
import com.hotel.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    /**
     * Pessimistic lock to prevent double-booking race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status IN ('CONFIRMED', 'PENDING', 'CHECKED_IN')
        AND b.checkInDate < :checkOut
        AND b.checkOutDate > :checkIn
        """)
    List<Booking> findOverlappingBookingsWithLock(
        @Param("roomId") Long roomId,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.room.id = :roomId
        AND b.status IN ('CONFIRMED', 'PENDING', 'CHECKED_IN')
        AND b.checkInDate < :checkOut
        AND b.checkOutDate > :checkIn
        """)
    List<Booking> findOverlappingBookings(
        @Param("roomId") Long roomId,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut
    );

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status = 'COMPLETED'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status = 'COMPLETED' AND b.createdAt BETWEEN :start AND :end")
    BigDecimal calculateRevenueByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    long countByStatus(BookingStatus status);

    @Query("""
        SELECT r.roomNumber as roomNumber, r.type as type, COUNT(b) as bookingCount
        FROM Booking b JOIN b.room r
        WHERE b.status IN ('COMPLETED', 'CONFIRMED', 'CHECKED_IN')
        GROUP BY r.id, r.roomNumber, r.type
        ORDER BY bookingCount DESC
        """)
    List<Object[]> findMostBookedRooms(Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.checkInDate = :date")
    List<Booking> findCheckInsForDate(@Param("date") LocalDate date);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CHECKED_IN' AND b.checkOutDate = :date")
    List<Booking> findCheckOutsForDate(@Param("date") LocalDate date);
}
