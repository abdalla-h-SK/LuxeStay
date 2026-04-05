package com.hotel.controller;

import com.hotel.security.UserPrincipal;
import com.hotel.service.impl.RoomServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final RoomServiceImpl roomService;

    // ── Auth Pages ──────────────────────────────────────────────
    @GetMapping({"/", "/index"})
    public String home() { return "user/home"; }

    @GetMapping("/auth/login")
    public String login() { return "auth/login"; }

    @GetMapping("/auth/register")
    public String register() { return "auth/register"; }

    @GetMapping("/auth/verify-otp")
    public String verifyOtp(@RequestParam(required = false) String email,
                            @RequestParam(required = false) String purpose,
                            Model model) {
        model.addAttribute("email", email);
        model.addAttribute("purpose", purpose != null ? purpose : "REGISTRATION");
        return "auth/verify-otp";
    }

    @GetMapping("/auth/forgot-password")
    public String forgotPassword() { return "auth/forgot-password"; }

    @GetMapping("/auth/reset-password")
    public String resetPassword(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "auth/reset-password";
    }

    // ── User Pages ───────────────────────────────────────────────
    @GetMapping("/rooms")
    public String rooms() { return "user/home"; }

    @GetMapping("/rooms/{id}")
    public String roomDetail(@PathVariable Long id, Model model) {
        model.addAttribute("roomId", id);
        return "user/room-detail";
    }

    @GetMapping("/bookings/new")
    public String newBooking(@RequestParam Long roomId, Model model) {
        model.addAttribute("roomId", roomId);
        return "user/booking";
    }

    @GetMapping("/bookings/my")
    public String myBookings() { return "user/my-bookings"; }

    @GetMapping("/payments/success")
    public String paymentSuccess(@RequestParam(required = false) String paymentId,
                                 @RequestParam(required = false) String PayerID,
                                 Model model) {
        model.addAttribute("paymentId", paymentId);
        model.addAttribute("payerId", PayerID);
        return "user/payment-success";
    }

    @GetMapping("/payments/cancel")
    public String paymentCancel() { return "user/payment-cancel"; }

    // ── Admin Pages ──────────────────────────────────────────────
    @GetMapping("/admin")
    public String adminDashboard() { return "admin/dashboard"; }

    @GetMapping("/admin/rooms")
    public String adminRooms() { return "admin/rooms"; }

    @GetMapping("/admin/bookings")
    public String adminBookings() { return "admin/bookings"; }

    @GetMapping("/admin/users")
    public String adminUsers() { return "admin/users"; }

    @GetMapping("/admin/reports")
    public String adminReports() { return "admin/reports"; }
}
