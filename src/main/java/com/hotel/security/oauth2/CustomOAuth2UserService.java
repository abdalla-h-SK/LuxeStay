package com.hotel.security.oauth2;

import com.hotel.entity.User;
import com.hotel.enums.Role;
import com.hotel.repository.UserRepository;
import com.hotel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        User user = processOAuth2User(userInfo);
        return new CustomOAuth2User(UserPrincipal.create(user), oAuth2User.getAttributes());
    }

    private User processOAuth2User(OAuth2UserInfo userInfo) {
        Optional<User> existingByGoogle = userRepository.findByGoogleId(userInfo.getId());
        if (existingByGoogle.isPresent()) {
            return updateExistingUser(existingByGoogle.get(), userInfo);
        }

        Optional<User> existingByEmail = userRepository.findByEmail(userInfo.getEmail());
        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
            user.setGoogleId(userInfo.getId());
            user.setProfilePicture(userInfo.getImageUrl());
            // Don't auto-verify — require OTP even for Google login
            return userRepository.save(user);
        }

        return registerNewOAuth2User(userInfo);
    }

    private User registerNewOAuth2User(OAuth2UserInfo userInfo) {
        String baseUsername = userInfo.getEmail().split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        String username = generateUniqueUsername(baseUsername);

        User user = User.builder()
                .googleId(userInfo.getId())
                .name(userInfo.getName())
                .email(userInfo.getEmail())
                .username(username)
                .profilePicture(userInfo.getImageUrl())
                .role(Role.GUEST)
                .verified(false) // Requires OTP verification
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        user.setName(userInfo.getName());
        user.setProfilePicture(userInfo.getImageUrl());
        return userRepository.save(user);
    }

    private String generateUniqueUsername(String base) {
        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter++;
        }
        return username;
    }
}
