package com.skybook.repository;

import com.skybook.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, String> {
    List<Flight> findBySourceContainingIgnoreCaseAndDestinationContainingIgnoreCase(String source, String destination);
    List<Flight> findBySourceContainingIgnoreCase(String source);
    List<Flight> findByDestinationContainingIgnoreCase(String destination);
}
