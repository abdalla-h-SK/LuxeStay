package com.hotel.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private long pendingBookings;
    private long totalRooms;
    private long availableRooms;
    private long occupiedRooms;
    private double occupancyRate;
    private long totalUsers;
    private List<MostBookedRoom> mostBookedRooms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MostBookedRoom {
        private String roomNumber;
        private String type;
        private long bookingCount;
    }
}
