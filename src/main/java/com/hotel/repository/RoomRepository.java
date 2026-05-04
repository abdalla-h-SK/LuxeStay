package com.hotel.repository;

import com.hotel.entity.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    Page<Room> findByStatus(RoomStatus status, Pageable pageable);


     // Find available rooms that are not booked during the given date range.
     // Uses pessimistic approach - excludes rooms with ANY overlapping confirmed/pending booking.

    @Query("""
        SELECT r FROM Room r
        WHERE r.status = 'AVAILABLE'
        AND r.id NOT IN (
            SELECT b.room.id FROM Booking b
            WHERE b.status IN ('CONFIRMED', 'PENDING', 'CHECKED_IN')
            AND b.checkInDate < :checkOut
            AND b.checkOutDate > :checkIn
        )
        AND (:type IS NULL OR r.type = :type)
        AND (:minPrice IS NULL OR r.price >= :minPrice)
        AND (:maxPrice IS NULL OR r.price <= :maxPrice)
        """)
    Page<Room> findAvailableRooms(
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut,
        @Param("type") RoomType type,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    @Query("SELECT r FROM Room r WHERE r.status = :status AND (:type IS NULL OR r.type = :type)")
    Page<Room> findByStatusAndType(
        @Param("status") RoomStatus status,
        @Param("type") RoomType type,
        Pageable pageable
    );

    @Query("SELECT COUNT(r) FROM Room r WHERE r.status = :status")
    long countByStatus(RoomStatus status);

    @Query("""
        SELECT r.type as type, COUNT(b) as count
        FROM Booking b JOIN b.room r
        WHERE b.status = 'COMPLETED'
        GROUP BY r.type
        ORDER BY count DESC
        """)
    List<Object[]> findMostBookedRoomTypes();

    @Query("SELECT r FROM Room r WHERE r.id NOT IN " +
           "(SELECT b.room.id FROM Booking b WHERE b.status IN ('CONFIRMED', 'CHECKED_IN', 'PENDING') " +
           "AND b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
    List<Room> findAllAvailableForDates(@Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);
}
