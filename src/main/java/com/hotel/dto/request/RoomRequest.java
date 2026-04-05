package com.hotel.dto.request;

import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomRequest {
    @NotBlank(message = "Room number is required")
    @Size(max = 10)
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType type;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    private RoomStatus status = RoomStatus.AVAILABLE;

    @Size(max = 2000)
    private String description;

    @Min(1) @Max(20)
    private Integer maxOccupancy = 2;

    private String imageUrl;
}
