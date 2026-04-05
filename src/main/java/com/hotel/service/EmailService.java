package com.hotel.service;

import com.hotel.entity.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Async
    public void sendOtpEmail(String to, String otp, String purpose) {
        String subject = switch (purpose) {
            case "REGISTRATION" -> "Verify Your Email - Hotel Management";
            case "PASSWORD_RESET" -> "Password Reset OTP - Hotel Management";
            case "GOOGLE_LOGIN" -> "Verify Your Google Login - Hotel Management";
            default -> "OTP Verification - Hotel Management";
        };
        String body = buildOtpEmailBody(otp, purpose);
        sendHtmlEmail(to, subject, body);
    }

    @Async
    public void sendBookingConfirmation(String to, Booking booking) {
        String subject = "Booking Confirmed #" + booking.getId() + " - Hotel Management";
        String body = buildBookingConfirmationBody(booking);
        sendHtmlEmail(to, subject, body);
    }

    @Async
    public void sendBookingCancellation(String to, Booking booking) {
        String subject = "Booking Cancelled #" + booking.getId() + " - Hotel Management";
        String body = buildBookingCancellationBody(booking);
        sendHtmlEmail(to, subject, body);
    }

    @Async
    public void sendPaymentSuccess(String to, Booking booking, BigDecimal amount) {
        String subject = "Payment Successful - Booking #" + booking.getId();
        String body = buildPaymentSuccessBody(booking, amount);
        sendHtmlEmail(to, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildOtpEmailBody(String otp, String purpose) {
        String purposeText = switch (purpose) {
            case "REGISTRATION" -> "complete your registration";
            case "PASSWORD_RESET" -> "reset your password";
            case "GOOGLE_LOGIN" -> "verify your Google login";
            default -> "verify your identity";
        };
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f5f5f5;">
              <div style="background: white; border-radius: 12px; padding: 40px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="text-align: center; margin-bottom: 30px;">
                  <h1 style="color: #1a1a2e; margin: 0;">🏨 Hotel Management</h1>
                </div>
                <h2 style="color: #333; margin-bottom: 10px;">Email Verification</h2>
                <p style="color: #666; font-size: 16px;">Use the code below to %s:</p>
                <div style="background: #f0f4ff; border: 2px dashed #4361ee; border-radius: 12px; padding: 30px; text-align: center; margin: 24px 0;">
                  <span style="font-size: 42px; font-weight: bold; letter-spacing: 12px; color: #4361ee;">%s</span>
                </div>
                <p style="color: #999; font-size: 14px;">⏰ This code expires in <strong>10 minutes</strong>.</p>
                <p style="color: #999; font-size: 14px;">If you didn't request this, please ignore this email.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                <p style="color: #ccc; font-size: 12px; text-align: center;">Hotel Management System &copy; 2024</p>
              </div>
            </body>
            </html>
            """.formatted(purposeText, otp);
    }

    private String buildBookingConfirmationBody(Booking booking) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f5f5f5;">
              <div style="background: white; border-radius: 12px; padding: 40px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="text-align: center; margin-bottom: 30px;">
                  <h1 style="color: #1a1a2e;">🏨 Hotel Management</h1>
                  <div style="background: #d4edda; color: #155724; padding: 12px 24px; border-radius: 50px; display: inline-block; font-weight: bold;">
                    ✅ Booking Confirmed!
                  </div>
                </div>
                <p style="color: #555;">Dear <strong>%s</strong>,</p>
                <p style="color: #555;">Your booking has been confirmed. Here are your details:</p>
                <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                  <tr style="background: #f8f9fa;"><td style="padding: 12px; border: 1px solid #dee2e6; font-weight: bold;">Booking ID</td><td style="padding: 12px; border: 1px solid #dee2e6;">#%d</td></tr>
                  <tr><td style="padding: 12px; border: 1px solid #dee2e6; font-weight: bold;">Room</td><td style="padding: 12px; border: 1px solid #dee2e6;">%s (%s)</td></tr>
                  <tr style="background: #f8f9fa;"><td style="padding: 12px; border: 1px solid #dee2e6; font-weight: bold;">Check-in</td><td style="padding: 12px; border: 1px solid #dee2e6;">%s</td></tr>
                  <tr><td style="padding: 12px; border: 1px solid #dee2e6; font-weight: bold;">Check-out</td><td style="padding: 12px; border: 1px solid #dee2e6;">%s</td></tr>
                  <tr style="background: #f8f9fa;"><td style="padding: 12px; border: 1px solid #dee2e6; font-weight: bold;">Total</td><td style="padding: 12px; border: 1px solid #dee2e6; color: #4361ee; font-weight: bold;">$%s</td></tr>
                </table>
                <p style="color: #999; font-size: 14px; text-align: center;">Thank you for choosing us. We look forward to welcoming you!</p>
              </div>
            </body>
            </html>
            """.formatted(
                booking.getUser().getName(),
                booking.getId(),
                booking.getRoom().getRoomNumber(), booking.getRoom().getType(),
                booking.getCheckInDate().format(DATE_FMT),
                booking.getCheckOutDate().format(DATE_FMT),
                booking.getTotalPrice()
        );
    }

    private String buildBookingCancellationBody(Booking booking) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f5f5f5;">
              <div style="background: white; border-radius: 12px; padding: 40px;">
                <h1 style="color: #1a1a2e; text-align: center;">🏨 Hotel Management</h1>
                <div style="background: #f8d7da; color: #721c24; padding: 12px 24px; border-radius: 50px; text-align: center; margin: 20px 0; font-weight: bold;">
                  ❌ Booking Cancelled
                </div>
                <p>Dear <strong>%s</strong>, your booking <strong>#%d</strong> for Room <strong>%s</strong> has been cancelled.</p>
                <p style="color: #999; font-size: 14px;">If you have any questions, please contact our support team.</p>
              </div>
            </body>
            </html>
            """.formatted(booking.getUser().getName(), booking.getId(), booking.getRoom().getRoomNumber());
    }

    private String buildPaymentSuccessBody(Booking booking, BigDecimal amount) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f5f5f5;">
              <div style="background: white; border-radius: 12px; padding: 40px;">
                <h1 style="color: #1a1a2e; text-align: center;">🏨 Hotel Management</h1>
                <div style="background: #d4edda; color: #155724; padding: 12px 24px; border-radius: 50px; text-align: center; margin: 20px 0; font-weight: bold;">
                  💳 Payment Successful!
                </div>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your payment of <strong style="color: #4361ee;">$%s</strong> for Booking <strong>#%d</strong> has been processed successfully.</p>
                <p>Your room <strong>%s</strong> is ready for check-in on <strong>%s</strong>.</p>
                <p style="color: #999; font-size: 14px;">Thank you for your payment!</p>
              </div>
            </body>
            </html>
            """.formatted(
                booking.getUser().getName(),
                amount,
                booking.getId(),
                booking.getRoom().getRoomNumber(),
                booking.getCheckInDate().format(DATE_FMT)
        );
    }
}
