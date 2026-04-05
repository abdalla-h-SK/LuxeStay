package com.hotel.dto.response;

import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private RoomType type;
    private BigDecimal price;
    private RoomStatus status;
    private String description;
    private Integer maxOccupancy;
    private String imageUrl;
    private LocalDateTime createdAt;
}
