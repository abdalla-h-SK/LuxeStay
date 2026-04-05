package com.hotel.controller;

import com.hotel.dto.request.PaymentRequest;
import com.hotel.dto.response.ApiResponse;
import com.hotel.dto.response.PaymentResponse;
import com.hotel.security.UserPrincipal;
import com.hotel.service.impl.PaymentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentServiceImpl paymentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment initiated",
                paymentService.createPayment(principal.getId(), request)));
    }

    // PayPal v2 sends: ?token=ORDER_ID&PayerID=PAYER_ID
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<PaymentResponse>> executePayment(
            @RequestParam String token,
            @RequestParam(required = false) String PayerID) {
        return ResponseEntity.ok(ApiResponse.success("Payment successful",
                paymentService.executePayment(token, PayerID)));
    }

    // Also handle GET for when PayPal redirects directly
    @GetMapping("/execute")
    public ResponseEntity<ApiResponse<PaymentResponse>> executePaymentGet(
            @RequestParam String token,
            @RequestParam(required = false) String PayerID) {
        return ResponseEntity.ok(ApiResponse.success("Payment successful",
                paymentService.executePayment(token, PayerID)));
    }
}
