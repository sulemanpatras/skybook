package com.skybook.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.skybook.model.Ticket;
import com.skybook.repository.TicketRepository;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleCalendarService {

    @Value("${google.calendar.credentials.path}")
    private String credentialsPath;

    @Value("${google.calendar.tokens.path}")
    private String tokensPath;

    @Value("${google.calendar.application.name}")
    private String applicationName;

    @Value("${google.calendar.service.account.key.path:#{null}}")
    private String serviceAccountKeyPath;

    @Value("${google.calendar.id:primary}")
    private String calendarId;

    @Value("${google.calendar.account.email:#{null}}")
    private String calendarAccountEmail;

    private final ResourceLoader resourceLoader;
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public GoogleCalendarService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private String getTargetEmail(String userEmail) {
        return (calendarAccountEmail != null && !calendarAccountEmail.isEmpty()) ? calendarAccountEmail : userEmail;
    }

    public String[] createFlightEvent(Ticket ticket, String userEmail) {
        String targetEmail = getTargetEmail(userEmail);
        try {
            Calendar service;
            
            // Try Service Account first if available
            if (serviceAccountKeyPath != null && !serviceAccountKeyPath.isEmpty()) {
                try {
                    service = getCalendarService();
                    System.out.println("Using Service Account for calendar integration");
                } catch (Exception e) {
                    System.out.println("Service Account failed, falling back to OAuth: " + e.getMessage());
                    service = buildCalendarService(targetEmail);
                }
            } else {
                // Use OAuth
                service = buildCalendarService(targetEmail);
            }
            
            Event event = new Event()
                    .setSummary("✈ Flight: " + ticket.getFlight().getSource()
                                + " → " + ticket.getFlight().getDestination())
                    .setDescription(buildEventDescription(ticket))
                    .setLocation(ticket.getFlight().getSource() + " Airport");

            com.google.api.client.util.DateTime startDateTime = toGoogleDateTime(
                    ticket.getFlight().getDepartureTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            );
            com.google.api.client.util.DateTime endDateTime = toGoogleDateTime(
                    ticket.getFlight().getArrivalTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            );

            event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone("Asia/Karachi"));
            event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone("Asia/Karachi"));

            Event createdEvent = service.events().insert(calendarId, event).execute();
            
            System.out.println("Calendar event created - ID: " + createdEvent.getId());
            System.out.println("Calendar event URL: " + createdEvent.getHtmlLink());
            
            return new String[]{ createdEvent.getId(), createdEvent.getHtmlLink() };

        } catch (Exception e) {
            System.err.println("Google Calendar event creation failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Calendar getCalendarService() throws Exception {
        // Load service account credentials
        java.io.File keyFile = new java.io.File(serviceAccountKeyPath);
        if (!keyFile.exists()) {
            throw new RuntimeException("Service account key file not found: " + serviceAccountKeyPath);
        }
        
        GoogleCredential credential = GoogleCredential.fromStream(
                new java.io.FileInputStream(keyFile))
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
        
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
                .setApplicationName(applicationName)
                .build();
    }

    public boolean isAuthorized(String userEmail) {
        String targetEmail = getTargetEmail(userEmail);
        try {
            InputStream in = resourceLoader.getResource(credentialsPath).getInputStream();
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new java.io.File(tokensPath));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(dataStoreFactory)
                    .setAccessType("offline")
                    .build();

            Credential existing = flow.loadCredential(targetEmail);
            return existing != null
                    && (existing.getRefreshToken() != null
                        || existing.getExpiresInSeconds() == null
                        || existing.getExpiresInSeconds() > 60);
        } catch (Exception e) {
            System.err.println("isAuthorized check failed for user: " + targetEmail + ". Error: " + e.getMessage());
            return false;
        }
    }

    public String requestAuthorizationAndGetUrl(Ticket ticket, String userEmail, TicketRepository ticketRepository) {
        String targetEmail = getTargetEmail(userEmail);
        try {
            final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            InputStream in = resourceLoader.getResource(credentialsPath).getInputStream();
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new java.io.File(tokensPath));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    transport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(dataStoreFactory)
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                    .setPort(8889)
                    .build();
            
            String redirectUri = receiver.getRedirectUri(); // This starts the Jetty server
            String authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();

            CompletableFuture.runAsync(() -> {
                try {
                    System.out.println("Background OAuth listener active on port 8889. Waiting for code...");
                    String code = receiver.waitForCode();
                    System.out.println("Received OAuth authorization code. Exchanging for token...");
                    com.google.api.client.auth.oauth2.TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
                    flow.createAndStoreCredential(response, targetEmail);
                    System.out.println("OAuth token stored for " + targetEmail);

                    // Refresh ticket from DB in case it changed during auth
                    Ticket currentTicket = ticketRepository.findById(ticket.getId()).orElse(ticket);
                    String[] calResult = createFlightEvent(currentTicket, targetEmail);
                    if (calResult != null && calResult.length == 2) {
                        currentTicket.setGoogleEventId(calResult[0]);
                        currentTicket.setCalendarEventUrl(calResult[1]);
                        ticketRepository.save(currentTicket);
                        System.out.println("Google Calendar event created post-auth and saved to ticket " + currentTicket.getId());
                    }
                } catch (Exception e) {
                    System.err.println("Error in background OAuth processing: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        receiver.stop();
                        System.out.println("Background OAuth receiver stopped.");
                    } catch (Exception e) {
                        System.err.println("Failed to stop background receiver: " + e.getMessage());
                    }
                }
            });

            return authorizationUrl;
        } catch (Exception e) {
            System.err.println("Failed to initiate background OAuth: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Keep your existing methods
    private Calendar buildCalendarService(String userEmail) throws Exception {
        String targetEmail = getTargetEmail(userEmail);
        final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(transport, targetEmail);
        return new Calendar.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();
    }

    private Credential authorize(NetHttpTransport transport, String userEmail) throws Exception {
        String targetEmail = getTargetEmail(userEmail);
        InputStream in = resourceLoader.getResource(credentialsPath).getInputStream();
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new java.io.File(tokensPath));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();

        Credential existing = flow.loadCredential(targetEmail);
        if (existing != null
                && (existing.getRefreshToken() != null
                    || existing.getExpiresInSeconds() == null
                    || existing.getExpiresInSeconds() > 60)) {
            return existing;
        }

        System.out.println("No stored token for " + targetEmail + " — opening browser for one-time auth...");
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8889)
                .build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(targetEmail);
    }

    public void deleteFlightEvent(String eventId, String userEmail) {
        String targetEmail = getTargetEmail(userEmail);
        try {
            if (eventId == null || eventId.isBlank()) return;
            Calendar service = buildCalendarService(targetEmail);
            service.events().delete(calendarId, eventId).execute();
        } catch (Exception e) {
            System.err.println("Google Calendar event deletion failed: " + e.getMessage());
        }
    }

    private String buildEventDescription(Ticket ticket) {
        return String.format(
            "SkyBook Booking Confirmation\n\nTicket: %s\nPassenger: %s\nAirline: %s\nFlight: %s\nSeat: %s\nPrice: USD %s",
            ticket.getId(), ticket.getPassengerName(),
            ticket.getFlight().getAirline(), ticket.getFlight().getId(),
            ticket.getSeatNumber(), ticket.getFlight().getPrice()
        );
    }

    private com.google.api.client.util.DateTime toGoogleDateTime(long epochMilli) {
        return new com.google.api.client.util.DateTime(epochMilli);
    }
}