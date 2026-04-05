package com.hotel.security.oauth2;

import com.hotel.entity.User;
import com.hotel.repository.UserRepository;
import com.hotel.security.jwt.JwtTokenProvider;
import com.hotel.service.OtpService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final OtpService otpService;

    @Value("${app.oauth2.authorized-redirect-uris}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Always require OTP for Google logins for security
        try {
            otpService.generateAndSendOtp(email, "GOOGLE_LOGIN");
        } catch (Exception e) {
            log.error("Failed to send OTP for Google login: {}", e.getMessage());
        }

        // Redirect to OTP verification page with email hint
        String targetUrl = UriComponentsBuilder.fromUriString("/auth/verify-otp")
                .queryParam("email", email)
                .queryParam("purpose", "GOOGLE_LOGIN")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
