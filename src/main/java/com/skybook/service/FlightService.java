package com.skybook.service;

import com.skybook.dto.FlightDTO;
import com.skybook.model.Flight;
import com.skybook.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    public List<FlightDTO.Response> getAll() {
        return flightRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public FlightDTO.Response getById(String id) {
        return flightRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + id));
    }

    public List<FlightDTO.Response> search(String source, String destination) {
        List<Flight> flights;
        if (source != null && !source.isBlank() && destination != null && !destination.isBlank()) {
            flights = flightRepository.findBySourceContainingIgnoreCaseAndDestinationContainingIgnoreCase(source, destination);
        } else if (source != null && !source.isBlank()) {
            flights = flightRepository.findBySourceContainingIgnoreCase(source);
        } else if (destination != null && !destination.isBlank()) {
            flights = flightRepository.findByDestinationContainingIgnoreCase(destination);
        } else {
            flights = flightRepository.findAll();
        }
        return flights.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FlightDTO.Response create(FlightDTO.Request req) {
        String id = "FL" + String.format("%03d", flightRepository.count() + 1);
        Flight flight = Flight.builder()
                .id(id)
                .airline(req.getAirline())
                .source(req.getSource())
                .destination(req.getDestination())
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .price(req.getPrice())
                .seatsAvailable(req.getSeatsAvailable())
                .totalSeats(req.getTotalSeats())
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(flightRepository.save(flight));
    }

    public FlightDTO.Response update(String id, FlightDTO.Request req) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + id));
        flight.setAirline(req.getAirline());
        flight.setSource(req.getSource());
        flight.setDestination(req.getDestination());
        flight.setDepartureTime(req.getDepartureTime());
        flight.setArrivalTime(req.getArrivalTime());
        flight.setPrice(req.getPrice());
        flight.setSeatsAvailable(req.getSeatsAvailable());
        flight.setTotalSeats(req.getTotalSeats());
        return toResponse(flightRepository.save(flight));
    }

    public void delete(String id) {
        flightRepository.deleteById(id);
    }

    public FlightDTO.Response toResponse(Flight f) {
        return FlightDTO.Response.builder()
                .id(f.getId())
                .airline(f.getAirline())
                .source(f.getSource())
                .destination(f.getDestination())
                .departureTime(f.getDepartureTime())
                .arrivalTime(f.getArrivalTime())
                .price(f.getPrice())
                .seatsAvailable(f.getSeatsAvailable())
                .totalSeats(f.getTotalSeats())
                .build();
    }

    public Flight findEntity(String id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + id));
    }
}
