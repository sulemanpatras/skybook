package com.skybook.service;

import com.skybook.model.Ticket;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final PdfService pdfService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.admin.email}")
    private String adminEmail;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Async
    public void sendBookingConfirmation(Ticket ticket, String passengerEmail) {
        try {
            byte[] pdf = pdfService.generateTicketPdf(ticket);

            // ── Email to Passenger ─────────────────────────────
            sendEmail(
                passengerEmail,
                "✈ Booking Confirmed — " + ticket.getId(),
                buildPassengerHtml(ticket),
                pdf,
                "ticket-" + ticket.getId() + ".pdf"
            );

            // ── Email to Admin ─────────────────────────────────
            sendEmail(
                adminEmail,
                "New Booking: " + ticket.getId() + " | " + ticket.getPassengerName(),
                buildAdminHtml(ticket),
                pdf,
                "ticket-" + ticket.getId() + ".pdf"
            );

        } catch (Exception e) {
            System.err.println("Email sending failed: " + e.getMessage());
        }
    }

    @Async
    public void sendCancellationEmail(Ticket ticket, String passengerEmail) {
        try {
            sendEmail(
                passengerEmail,
                "Booking Cancelled — " + ticket.getId(),
                buildCancellationHtml(ticket),
                null, null
            );
        } catch (Exception e) {
            System.err.println("Cancellation email failed: " + e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String html,
                           byte[] attachment, String attachmentName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        if (attachment != null) {
            helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
        }
        mailSender.send(message);
    }

    private String buildPassengerHtml(Ticket t) {
        return """
            <!DOCTYPE html><html><body style="font-family:Arial,sans-serif;background:#f3f4f6;padding:20px">
            <div style="max-width:600px;margin:auto;background:#fff;border-radius:12px;overflow:hidden">
              <div style="background:#1e40af;padding:24px;color:#fff;text-align:center">
                <h1 style="margin:0;font-size:24px">✈ SkyBook</h1>
                <p style="margin:4px 0 0;opacity:.85">Your booking is confirmed!</p>
              </div>
              <div style="padding:28px">
                <p>Hi <strong>%s</strong>,</p>
                <p>Your flight has been booked. Here are the details:</p>
                <table style="width:100%%;border-collapse:collapse;margin:16px 0">
                  <tr><td style="padding:8px;background:#f9fafb;font-weight:bold;width:40%%">Ticket No.</td><td style="padding:8px;border-bottom:1px solid #e5e7eb">%s</td></tr>
                  <tr><td style="padding:8px;background:#f9fafb;font-weight:bold">Route</td><td style="padding:8px;border-bottom:1px solid #e5e7eb">%s → %s</td></tr>
                  <tr><td style="padding:8px;background:#f9fafb;font-weight:bold">Airline</td><td style="padding:8px;border-bottom:1px solid #e5e7eb">%s</td></tr>
                  <tr><td style="padding:8px;background:#f9fafb;font-weight:bold">Departure</td><td style="padding:8px;border-bottom:1px solid #e5e7eb">%s</td></tr>
                  <tr><td style="padding:8px;background:#f9fafb;font-weight:bold">Seat</td><td style="padding:8px;border-bottom:1px solid #e5e7eb">%s</td></tr>
                  <tr><td style="padding:8px;background:#f9fafb;font-weight:bold">Price</td><td style="padding:8px">USD %s</td></tr>
                </table>
                <p>Your ticket PDF is attached. Have a safe journey! 🛫</p>
              </div>
              <div style="background:#f9fafb;padding:16px;text-align:center;font-size:12px;color:#6b7280">
                SkyBook Flight Management System
              </div>
            </div></body></html>
            """.formatted(
                t.getPassengerName(), t.getId(),
                t.getFlight().getSource(), t.getFlight().getDestination(),
                t.getFlight().getAirline(),
                t.getFlight().getDepartureTime().format(FMT),
                t.getSeatNumber(),
                t.getFlight().getPrice()
            );
    }

    private String buildAdminHtml(Ticket t) {
        return """
            <!DOCTYPE html><html><body style="font-family:Arial,sans-serif">
            <h2>New Booking Received</h2>
            <p><b>Ticket:</b> %s</p>
            <p><b>Passenger:</b> %s</p>
            <p><b>Flight:</b> %s — %s → %s</p>
            <p><b>Departure:</b> %s</p>
            <p><b>Seat:</b> %s</p>
            <p><b>Price:</b> USD %s</p>
            </body></html>
            """.formatted(
                t.getId(), t.getPassengerName(),
                t.getFlight().getId(), t.getFlight().getSource(), t.getFlight().getDestination(),
                t.getFlight().getDepartureTime().format(FMT),
                t.getSeatNumber(), t.getFlight().getPrice()
            );
    }

    private String buildCancellationHtml(Ticket t) {
        return """
            <!DOCTYPE html><html><body style="font-family:Arial,sans-serif">
            <h2>Booking Cancelled</h2>
            <p>Hi %s, your ticket <b>%s</b> for flight %s → %s has been cancelled.</p>
            </body></html>
            """.formatted(
                t.getPassengerName(), t.getId(),
                t.getFlight().getSource(), t.getFlight().getDestination()
            );
    }
}
