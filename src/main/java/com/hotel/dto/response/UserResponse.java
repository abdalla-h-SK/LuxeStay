package com.hotel.dto.response;

import com.hotel.enums.Role;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String email;
    private Role role;
    private boolean verified;
    private String profilePicture;
    private LocalDateTime createdAt;
}
