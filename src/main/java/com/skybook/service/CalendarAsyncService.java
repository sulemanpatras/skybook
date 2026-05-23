package com.skybook.service;

import com.skybook.model.Ticket;
import com.skybook.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarAsyncService {

    private final GoogleCalendarService calendarService;
    private final TicketRepository ticketRepository;

    @Async("taskExecutor")
    public void createAndSaveCalendarEvent(String ticketId, String userEmail) {
        try {
                System.out.println("INSIDE ASYNC METHOD");

            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Ticket not found"));

            System.out.println("🟡 Starting calendar event creation for ticket: " + ticket.getId());

            String[] calResult = calendarService.createFlightEvent(ticket, userEmail);

            if (calResult != null && calResult.length == 2) {

                ticket.setGoogleEventId(calResult[0]);
                ticket.setCalendarEventUrl(calResult[1]);

                ticketRepository.save(ticket);

                System.out.println("✅ Calendar URL saved: " + calResult[1]);

            } else {
                System.err.println("❌ Calendar service returned null");
            }

        } catch (Exception e) {
            System.err.println("❌ Calendar integration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}