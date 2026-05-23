package com.skybook.repository;

import com.skybook.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByUserId(Long userId);
    List<Ticket> findByFlightId(String flightId);
    long countByFlightIdAndStatus(String flightId, Ticket.Status status);
}
