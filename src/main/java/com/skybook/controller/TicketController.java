package com.skybook.controller;

import com.skybook.dto.TicketDTO;
import com.skybook.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/book")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<TicketDTO.Response> book(
            @Valid @RequestBody TicketDTO.BookRequest req,
            Authentication auth) {
        return ResponseEntity.ok(ticketService.bookTicket(req, auth.getName()));
    }

    @GetMapping("/my")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<List<TicketDTO.Response>> myTickets(Authentication auth) {
        return ResponseEntity.ok(ticketService.getMyTickets(auth.getName()));
    }

    @GetMapping("/all")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketDTO.Response>> allTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @PutMapping("/{id}/cancel")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<TicketDTO.Response> cancel(
            @PathVariable String id,
            Authentication auth) {
        return ResponseEntity.ok(ticketService.cancelTicket(id, auth.getName()));
    }

    @GetMapping("/{id}/pdf")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable String id,
            Authentication auth) {
        byte[] pdf = ticketService.downloadTicketPdf(id, auth.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
