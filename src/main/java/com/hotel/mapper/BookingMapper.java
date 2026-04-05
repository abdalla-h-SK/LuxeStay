package com.hotel.mapper;

import com.hotel.dto.response.BookingResponse;
import com.hotel.entity.Booking;
import org.mapstruct.*;

import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class, RoomMapper.class, PaymentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    @Mapping(target = "nights", expression = "java(calculateNights(booking))")
    BookingResponse toResponse(Booking booking);

    default long calculateNights(Booking booking) {
        if (booking.getCheckInDate() == null || booking.getCheckOutDate() == null) return 0;
        return ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
    }
}
