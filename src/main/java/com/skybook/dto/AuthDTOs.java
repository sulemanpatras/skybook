package com.skybook.dto;

import com.skybook.model.Ticket;
import com.skybook.model.User;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ── Auth DTOs ──────────────────────────────────────────────
public class AuthDTOs {

    @Data
    public static class RegisterRequest {
        @NotBlank private String fullName;
        @Email @NotBlank private String email;
        @NotBlank @Size(min = 6) private String password;
    }

    @Data
    public static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data @Builder
    public static class AuthResponse {
        private String token;
        private Long userId;
        private String fullName;
        private String email;
        private User.Role role;
    }
}

// ── Flight DTOs ────────────────────────────────────────────
class FlightDTOs {

    @Data
    public static class FlightRequest {
        @NotBlank private String airline;
        @NotBlank private String source;
        @NotBlank private String destination;
        @NotNull  private LocalDateTime departureTime;
        @NotNull  private LocalDateTime arrivalTime;
        @NotNull @Positive private BigDecimal price;
        @NotNull @Min(1)   private int seatsAvailable;
        @NotNull @Min(1)   private int totalSeats;
    }

    @Data @Builder
    public static class FlightResponse {
        private String id;
        private String airline;
        private String source;
        private String destination;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private BigDecimal price;
        private int seatsAvailable;
        private int totalSeats;
    }
}

// ── Ticket DTOs ────────────────────────────────────────────
class TicketDTOs {

    @Data
    public static class BookingRequest {
        @NotBlank private String flightId;
        @NotBlank private String passengerName;
    }

    @Data @Builder
    public static class TicketResponse {
        private String id;
        private String passengerName;
        private String flightId;
        private String airline;
        private String source;
        private String destination;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private String seatNumber;
        private Ticket.Status status;
        private LocalDateTime bookedAt;
        private BigDecimal price;
    }
}
