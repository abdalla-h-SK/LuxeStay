package com.hotel.config;

import com.hotel.entity.Room;
import com.hotel.entity.User;
import com.hotel.enums.Role;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedStaff();
        seedRooms();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@luxestay.com")) return;
        User admin = User.builder()
                .username("admin")
                .name("Hotel Admin")
                .email("admin@luxestay.com")
                .password(passwordEncoder.encode("Admin@1234"))
                .role(Role.ADMIN)
                .verified(true)
                .build();
        userRepository.save(admin);
        log.info("Admin user seeded");
    }

    private void seedStaff() {
        if (userRepository.existsByEmail("staff@luxestay.com")) return;
        User staff = User.builder()
                .username("staff01")
                .name("Front Desk Staff")
                .email("staff@luxestay.com")
                .password(passwordEncoder.encode("Admin@1234"))
                .role(Role.STAFF)
                .verified(true)
                .build();
        userRepository.save(staff);
        log.info("Staff user seeded");
    }

    private void seedRooms() {
        if (roomRepository.count() > 0) return;
        roomRepository.save(Room.builder().roomNumber("101").type(RoomType.SINGLE).price(new BigDecimal("89.00")).status(RoomStatus.AVAILABLE).description("Cozy single room perfect for solo travelers.").maxOccupancy(1).imageUrl("https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800").build());
        roomRepository.save(Room.builder().roomNumber("102").type(RoomType.SINGLE).price(new BigDecimal("89.00")).status(RoomStatus.AVAILABLE).description("Bright single room with modern amenities.").maxOccupancy(1).imageUrl("https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800").build());
        roomRepository.save(Room.builder().roomNumber("201").type(RoomType.DOUBLE).price(new BigDecimal("149.00")).status(RoomStatus.AVAILABLE).description("Spacious double room with king-size bed.").maxOccupancy(2).imageUrl("https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800").build());
        roomRepository.save(Room.builder().roomNumber("202").type(RoomType.DOUBLE).price(new BigDecimal("149.00")).status(RoomStatus.AVAILABLE).description("Elegant double room overlooking the garden.").maxOccupancy(2).imageUrl("https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=800").build());
        roomRepository.save(Room.builder().roomNumber("203").type(RoomType.TWIN).price(new BigDecimal("139.00")).status(RoomStatus.AVAILABLE).description("Twin room with two separate beds.").maxOccupancy(2).imageUrl("https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800").build());
        roomRepository.save(Room.builder().roomNumber("301").type(RoomType.SUITE).price(new BigDecimal("299.00")).status(RoomStatus.AVAILABLE).description("Luxurious suite with jacuzzi and panoramic views.").maxOccupancy(3).imageUrl("https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800").build());
        roomRepository.save(Room.builder().roomNumber("302").type(RoomType.SUITE).price(new BigDecimal("299.00")).status(RoomStatus.MAINTENANCE).description("Grand suite with butler service and private terrace.").maxOccupancy(3).imageUrl("https://images.unsplash.com/photo-1563911302283-d2bc129e7570?w=800").build());
        roomRepository.save(Room.builder().roomNumber("401").type(RoomType.DELUXE).price(new BigDecimal("219.00")).status(RoomStatus.AVAILABLE).description("Premium deluxe room with rainfall shower.").maxOccupancy(2).imageUrl("https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800").build());
        roomRepository.save(Room.builder().roomNumber("402").type(RoomType.DELUXE).price(new BigDecimal("219.00")).status(RoomStatus.AVAILABLE).description("Stylish deluxe room with smart TV and concierge.").maxOccupancy(2).imageUrl("https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800").build());
        roomRepository.save(Room.builder().roomNumber("501").type(RoomType.PRESIDENTIAL).price(new BigDecimal("599.00")).status(RoomStatus.AVAILABLE).description("Two-bedroom presidential suite with rooftop terrace.").maxOccupancy(6).imageUrl("https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=800").build());
        log.info("10 rooms seeded");
    }
}