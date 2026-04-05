package com.hotel.service.impl;

import com.hotel.dto.response.ReportResponse;
import com.hotel.enums.BookingStatus;
import com.hotel.enums.Role;
import com.hotel.enums.RoomStatus;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "reports", key = "'dashboard'")
    @Transactional(readOnly = true)
    public ReportResponse getDashboardReport() {
        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.countByStatus(RoomStatus.AVAILABLE);
        long occupiedRooms = roomRepository.countByStatus(RoomStatus.OCCUPIED);
        double occupancyRate = totalRooms > 0 ? (double) occupiedRooms / totalRooms * 100 : 0;

        LocalDateTime monthStart = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = LocalDateTime.now().with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59);

        List<Object[]> topRooms = bookingRepository.findMostBookedRooms(PageRequest.of(0, 5));
        List<ReportResponse.MostBookedRoom> mostBooked = topRooms.stream()
                .map(row -> ReportResponse.MostBookedRoom.builder()
                        .roomNumber((String) row[0])
                        .type(row[1].toString())
                        .bookingCount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        return ReportResponse.builder()
                .totalRevenue(bookingRepository.calculateTotalRevenue())
                .monthlyRevenue(bookingRepository.calculateRevenueByDateRange(monthStart, monthEnd))
                .totalBookings(bookingRepository.count())
                .confirmedBookings(bookingRepository.countByStatus(BookingStatus.CONFIRMED))
                .cancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED))
                .pendingBookings(bookingRepository.countByStatus(BookingStatus.PENDING))
                .totalRooms(totalRooms)
                .availableRooms(availableRooms)
                .occupiedRooms(occupiedRooms)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .totalUsers(userRepository.count())
                .mostBookedRooms(mostBooked)
                .build();
    }
}
