package com.hotel.service.impl;

import com.hotel.dto.request.RoomRequest;
import com.hotel.dto.response.PageResponse;
import com.hotel.dto.response.RoomResponse;
import com.hotel.entity.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import com.hotel.exception.BadRequestException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.mapper.RoomMapper;
import com.hotel.repository.RoomRepository;
import com.hotel.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getAllRooms(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return PageResponse.from(roomRepository.findAll(pageable).map(roomMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getAvailableRooms(
            LocalDate checkIn, LocalDate checkOut,
            RoomType type, BigDecimal minPrice, BigDecimal maxPrice,
            int page, int size, String sortBy) {

        validateDates(checkIn, checkOut);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Room> rooms = roomRepository.findAvailableRooms(checkIn, checkOut, type, minPrice, maxPrice, pageable);
        return PageResponse.from(rooms.map(roomMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        return roomMapper.toResponse(findRoomById(id));
    }

    @CacheEvict(value = "rooms", allEntries = true)
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new BadRequestException("Room number already exists: " + request.getRoomNumber());
        }
        Room room = roomMapper.toEntity(request);
        Room saved = roomRepository.save(room);
        log.info("Room created: {}", saved.getRoomNumber());
        return roomMapper.toResponse(saved);
    }

    @CacheEvict(value = "rooms", allEntries = true)
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = findRoomById(id);
        if (!room.getRoomNumber().equals(request.getRoomNumber())
                && roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new BadRequestException("Room number already exists: " + request.getRoomNumber());
        }
        roomMapper.updateEntity(request, room);
        Room updated = roomRepository.save(room);
        notificationService.broadcastRoomUpdate(roomMapper.toResponse(updated));
        log.info("Room updated: {}", updated.getRoomNumber());
        return roomMapper.toResponse(updated);
    }

    @CacheEvict(value = "rooms", allEntries = true)
    @Transactional
    public void deleteRoom(Long id) {
        Room room = findRoomById(id);
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new BadRequestException("Cannot delete an occupied room");
        }
        roomRepository.delete(room);
        log.info("Room deleted: {}", room.getRoomNumber());
    }

    @Transactional
    public RoomResponse updateRoomStatus(Long id, RoomStatus status) {
        Room room = findRoomById(id);
        room.setStatus(status);
        Room updated = roomRepository.save(room);
        notificationService.broadcastRoomUpdate(roomMapper.toResponse(updated));
        return roomMapper.toResponse(updated);
    }

    public Room findRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) throw new BadRequestException("Dates are required");
        if (!checkOut.isAfter(checkIn)) throw new BadRequestException("Check-out must be after check-in");
        if (checkIn.isBefore(LocalDate.now())) throw new BadRequestException("Check-in cannot be in the past");
    }
}
