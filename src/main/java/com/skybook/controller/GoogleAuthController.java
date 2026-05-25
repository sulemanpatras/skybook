package com.skybook.controller;

import com.skybook.model.Ticket;
import com.skybook.repository.TicketRepository;
import com.skybook.service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/google")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class GoogleAuthController {

    private final GoogleCalendarService calendarService;
    private final TicketRepository ticketRepository;

    // Step 1: Frontend calls this to get the Google auth URL
    @GetMapping("/authorize")
    public ResponseEntity<String> authorize(@RequestParam String ticketId) {
        try {
            String url = calendarService.buildAuthorizationUrl(ticketId);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to build auth URL: " + e.getMessage());
        }
    }

    // Step 2: Google redirects here after user grants permission
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam String state) {   // state = ticketId string e.g. "TK00001"
        try {
            calendarService.handleCallback(code, state);

            // state is the full ticket ID string e.g. "TK00001"
            Ticket ticket = ticketRepository.findById(state).orElseThrow();
            String[] result = calendarService.createFlightEvent(ticket, ticket.getUser().getEmail());

            if (result != null) {
                ticket.setGoogleEventId(result[0]);
                ticket.setCalendarEventUrl(result[1]);
                ticketRepository.save(ticket);
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "https://airplane-ticket-management.vercel.app/my-tickets")
                    .build();

        } catch (Exception e) {
            System.err.println("OAuth callback failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "https://airplane-ticket-management.vercel.app/my-tickets?calendarError=true")
                    .build();
        }
    }

    // Optional: check if calendar is already authorized (frontend can poll this)
    @GetMapping("/status")
    public ResponseEntity<Boolean> status() {
        return ResponseEntity.ok(calendarService.isAuthorized());
    }
}