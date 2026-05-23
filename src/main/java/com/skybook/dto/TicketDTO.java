package com.skybook.dto;

import com.skybook.model.Ticket;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TicketDTO {

    @Data
    public static class BookRequest {
        @NotBlank private String flightId;
        @NotBlank private String passengerName;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
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
        private String calendarEventUrl;
        private String authUrl;
    }
}
