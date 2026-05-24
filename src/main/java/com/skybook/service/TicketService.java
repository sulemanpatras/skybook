package com.skybook.service;

import com.skybook.dto.TicketDTO;
import com.skybook.model.Flight;
import com.skybook.model.Ticket;
import com.skybook.model.User;
import com.skybook.repository.FlightRepository;
import com.skybook.repository.TicketRepository;
import com.skybook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final GoogleCalendarService calendarService;
    private final PdfService pdfService;

    // ── removed CalendarAsyncService — no longer needed ──

    @Transactional
    public TicketDTO.Response bookTicket(TicketDTO.BookRequest req, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Flight flight = flightRepository.findById(req.getFlightId())
                .orElseThrow(() -> new RuntimeException("Flight not found: " + req.getFlightId()));

        if (flight.getSeatsAvailable() <= 0) {
            throw new RuntimeException("No seats available on this flight.");
        }

        int taken = flight.getTotalSeats() - flight.getSeatsAvailable();
        int seatNum = taken + 1;
        int row = (int) Math.ceil(seatNum / 6.0);
        char col = (char) ('A' + ((seatNum - 1) % 6));

        String ticketId = "TK" + String.format("%05d", ticketRepository.count() + 1);

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .user(user)
                .flight(flight)
                .passengerName(req.getPassengerName())
                .seatNumber(row + String.valueOf(col))
                .status(Ticket.Status.CONFIRMED)
                .bookedAt(LocalDateTime.now())
                .build();

        flight.setSeatsAvailable(flight.getSeatsAvailable() - 1);
        flightRepository.save(flight);

        Ticket saved = ticketRepository.save(ticket);

        String authUrl = null;

        if (calendarService.isAuthorized()) {
            // Token already in DB — create event immediately
            String[] calResult = calendarService.createFlightEvent(saved, userEmail);
            if (calResult != null && calResult.length == 2) {
                saved.setGoogleEventId(calResult[0]);
                saved.setCalendarEventUrl(calResult[1]);
                ticketRepository.save(saved);
                System.out.println("Calendar event created: " + calResult[1]);
            } else {
                System.err.println("Calendar service returned null");
            }
        } else {
            // No token in DB yet — send user to authorize
            // Frontend will redirect user to this URL
            try {
                authUrl = calendarService.buildAuthorizationUrl(saved.getId());

            } catch (Exception e) {
                System.err.println("Failed to build auth URL: " + e.getMessage());
            }
        }

        emailService.sendBookingConfirmation(saved, user.getEmail());

        TicketDTO.Response response = toResponse(saved);
        response.setAuthUrl(authUrl);
        return response;
    }

    @Transactional
    public void updateCalendarInfo(String ticketId, String googleEventId, String calendarEventUrl) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        ticket.setGoogleEventId(googleEventId);
        ticket.setCalendarEventUrl(calendarEventUrl);
        ticketRepository.save(ticket);
        System.out.println("Calendar info saved for ticket " + ticketId + ": " + calendarEventUrl);
    }

    @Transactional
    public TicketDTO.Response cancelTicket(String ticketId, String userEmail) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        if (!ticket.getUser().getEmail().equals(userEmail)
                && !ticket.getUser().getRole().name().equals("ADMIN")) {
            throw new RuntimeException("Unauthorized to cancel this ticket.");
        }

        if (ticket.getStatus() == Ticket.Status.CANCELLED) {
            throw new RuntimeException("Ticket already cancelled.");
        }

        ticket.setStatus(Ticket.Status.CANCELLED);
        ticketRepository.save(ticket);

        Flight flight = ticket.getFlight();
        flight.setSeatsAvailable(flight.getSeatsAvailable() + 1);
        flightRepository.save(flight);

        emailService.sendCancellationEmail(ticket, ticket.getUser().getEmail());

        if (ticket.getGoogleEventId() != null) {
            calendarService.deleteFlightEvent(
                    ticket.getGoogleEventId(),
                    ticket.getUser().getEmail()
            );
        }

        return toResponse(ticket);
    }

    public List<TicketDTO.Response> getMyTickets(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ticketRepository.findByUserId(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TicketDTO.Response> getAllTickets() {
        return ticketRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public byte[] downloadTicketPdf(String ticketId, String userEmail) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        if (!ticket.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized.");
        }
        return pdfService.generateTicketPdf(ticket);
    }

    private TicketDTO.Response toResponse(Ticket t) {
        return TicketDTO.Response.builder()
                .id(t.getId())
                .passengerName(t.getPassengerName())
                .flightId(t.getFlight().getId())
                .airline(t.getFlight().getAirline())
                .source(t.getFlight().getSource())
                .destination(t.getFlight().getDestination())
                .departureTime(t.getFlight().getDepartureTime())
                .arrivalTime(t.getFlight().getArrivalTime())
                .seatNumber(t.getSeatNumber())
                .status(t.getStatus())
                .bookedAt(t.getBookedAt())
                .price(t.getFlight().getPrice())
                .calendarEventUrl(t.getCalendarEventUrl())
                .build();
    }
}