package com.hotel.service.impl;

import com.hotel.dto.request.PaymentRequest;
import com.hotel.dto.response.PaymentResponse;
import com.hotel.entity.Booking;
import com.hotel.entity.Payment;
import com.hotel.enums.BookingStatus;
import com.hotel.enums.PaymentMethod;
import com.hotel.enums.PaymentStatus;
import com.hotel.exception.BadRequestException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.mapper.PaymentMapper;
import com.hotel.repository.PaymentRepository;
import com.hotel.service.EmailService;
import com.hotel.websocket.NotificationService;
import com.hotel.config.PayPalConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.RoundingMode;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl {

    private final PaymentRepository paymentRepository;
    private final BookingServiceImpl bookingService;
    private final PaymentMapper paymentMapper;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final PayPalConfig payPalConfig;
    private final RestTemplate restTemplate;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ── Get PayPal Access Token ──────────────────────────────────
    private String getAccessToken() {
        String credentials = payPalConfig.getClientId() + ":" + payPalConfig.getClientSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>("grant_type=client_credentials", headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                payPalConfig.getBaseUrl() + "/v1/oauth2/token",
                HttpMethod.POST,
                entity,
                Map.class
        );

        return (String) response.getBody().get("access_token");
    }

    // ── Create Payment ───────────────────────────────────────────
    @Transactional
    public PaymentResponse createPayment(Long userId, PaymentRequest request) {
        Booking booking = bookingService.findBookingById(request.getBookingId());

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to pay for this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled booking");
        }
        if (paymentRepository.findByBookingId(booking.getId())
                .map(p -> p.getStatus() == PaymentStatus.PAID).orElse(false)) {
            throw new BadRequestException("This booking has already been paid");
        }

        String returnUrl = frontendUrl + "/payments/success";
        String cancelUrl = frontendUrl + "/payments/cancel";

        try {
            String accessToken = getAccessToken();

            // Build order payload
            Map<String, Object> orderPayload = new HashMap<>();
            orderPayload.put("intent", "CAPTURE");

            // Amount
            Map<String, Object> amount = new HashMap<>();
            amount.put("currency_code", "USD");
            amount.put("value", booking.getTotalPrice().setScale(2, RoundingMode.HALF_UP).toString());

            // Purchase unit
            Map<String, Object> purchaseUnit = new HashMap<>();
            purchaseUnit.put("amount", amount);
            purchaseUnit.put("description", "Hotel booking #" + booking.getId() +
                    " | Room " + booking.getRoom().getRoomNumber() +
                    " | " + booking.getCheckInDate() + " to " + booking.getCheckOutDate());

            orderPayload.put("purchase_units", List.of(purchaseUnit));

            // Application context (redirect URLs)
            Map<String, Object> appContext = new HashMap<>();
            appContext.put("return_url", returnUrl);
            appContext.put("cancel_url", cancelUrl);
            appContext.put("brand_name", "LuxeStay Hotel");
            appContext.put("landing_page", "NO_PREFERENCE");
            appContext.put("shipping_preference", "NO_SHIPPING");
            appContext.put("user_action", "PAY_NOW");
            orderPayload.put("application_context", appContext);

            // Call PayPal
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderPayload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    payPalConfig.getBaseUrl() + "/v2/checkout/orders",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            String orderId = (String) body.get("id");

            // Find approval URL
            String approvalUrl = ((List<Map<String, String>>) body.get("links"))
                    .stream()
                    .filter(l -> "approve".equals(l.get("rel")))
                    .findFirst()
                    .map(l -> l.get("href"))
                    .orElseThrow(() -> new BadRequestException("Failed to get PayPal approval URL"));

            // Save payment
            Payment payment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getTotalPrice())
                    .status(PaymentStatus.PENDING)
                    .method(PaymentMethod.PAYPAL)
                    .paypalOrderId(orderId)
                    .approvalUrl(approvalUrl)
                    .build();

            paymentRepository.save(payment);
            log.info("PayPal v2 order created for booking {}: {}", booking.getId(), orderId);

            return paymentMapper.toResponse(payment);

        } catch (Exception e) {
            log.error("PayPal payment creation failed: {}", e.getMessage());
            throw new BadRequestException("Payment creation failed: " + e.getMessage());
        }
    }

    // ── Execute/Capture Payment ──────────────────────────────────
    @Transactional
    public PaymentResponse executePayment(String token, String payerId) {
        // PayPal v2 uses 'token' param (which is the order ID) on redirect
        String orderId = token;

        Payment payment = paymentRepository.findByPaypalOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for order: " + orderId));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return paymentMapper.toResponse(payment);
        }

        try {
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    payPalConfig.getBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            String status = (String) body.get("status");

            if ("COMPLETED".equals(status)) {
                // Extract transaction ID
                String transactionId = orderId;
                try {
                    List<Map<String, Object>> purchaseUnits =
                            (List<Map<String, Object>>) body.get("purchase_units");
                    Map<String, Object> payments =
                            (Map<String, Object>) purchaseUnits.get(0).get("payments");
                    List<Map<String, Object>> captures =
                            (List<Map<String, Object>>) payments.get("captures");
                    transactionId = (String) captures.get(0).get("id");
                } catch (Exception e) {
                    log.warn("Could not extract transaction ID, using order ID");
                }

                payment.setStatus(PaymentStatus.PAID);
                payment.setTransactionId(transactionId);
                paymentRepository.save(payment);

                Booking booking = payment.getBooking();
                bookingService.confirmBooking(booking.getId());

                emailService.sendPaymentSuccess(
                        booking.getUser().getEmail(), booking, payment.getAmount());
                notificationService.sendToUser(
                        booking.getUser().getUsername(), "PAYMENT_SUCCESS",
                        "Payment of $" + payment.getAmount() +
                                " received for booking #" + booking.getId(), null);

                log.info("Payment captured for booking {}: tx={}",
                        booking.getId(), transactionId);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new BadRequestException("Payment not completed. Status: " + status);
            }

            return paymentMapper.toResponse(payment);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.error("PayPal capture failed: {}", e.getMessage());
            throw new BadRequestException("Payment capture failed: " + e.getMessage());
        }
    }
}