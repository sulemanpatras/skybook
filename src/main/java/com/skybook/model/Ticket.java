package com.skybook.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @Column(length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @Column(name = "passenger_name", nullable = false, length = 100)
    private String passengerName;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.CONFIRMED;

    @Column(name = "booked_at")
    private LocalDateTime bookedAt = LocalDateTime.now();

    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @Column(name = "calendar_event_url", length = 500)
    private String calendarEventUrl; 

    public enum Status {
        CONFIRMED, CANCELLED
    }

    // Add these methods
    public String getId() { return id; }
    public User getUser() { return user; }
    public Flight getFlight() { return flight; }
    public String getPassengerName() { return passengerName; }
    public String getSeatNumber() { return seatNumber; }
    public Status getStatus() { return status; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public String getGoogleEventId() { return googleEventId; }
    public String getCalendarEventUrl() { return calendarEventUrl; }

    // Setters
    public void setUser(User user) { this.user = user; }
    public void setFlight(Flight flight) { this.flight = flight; }
    public void setStatus(Status status) { this.status = status; }
    public void setGoogleEventId(String googleEventId) { this.googleEventId = googleEventId; }
    public void setCalendarEventUrl(String calendarEventUrl) { this.calendarEventUrl = calendarEventUrl; }
}
