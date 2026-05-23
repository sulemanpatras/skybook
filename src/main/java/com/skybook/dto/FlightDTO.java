package com.skybook.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FlightDTO {

    @Data
    public static class Request {
        @NotBlank private String airline;
        @NotBlank private String source;
        @NotBlank private String destination;
        @NotNull  private LocalDateTime departureTime;
        @NotNull  private LocalDateTime arrivalTime;
        @NotNull @Positive private BigDecimal price;
        @NotNull @Min(1) private int seatsAvailable;
        @NotNull @Min(1) private int totalSeats;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
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
