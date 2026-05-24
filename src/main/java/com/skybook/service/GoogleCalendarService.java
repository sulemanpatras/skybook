package com.skybook.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.skybook.config.GoogleCredentialsConfig;
import com.skybook.model.GoogleToken;
import com.skybook.model.Ticket;
import com.skybook.repository.GoogleTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private String getCredentialsPath() {
        return GoogleCredentialsConfig.getCredentialsFilePath();
    }

    @Value("${google.calendar.application.name}")
    private String applicationName;

    @Value("${google.calendar.id:primary}")
    private String calendarId;

    @Value("${google.calendar.account.email}")
    private String calendarAccountEmail;

    @Value("${app.base.url}")
    private String appBaseUrl;

    private final ResourceLoader resourceLoader;
    private final GoogleTokenRepository tokenRepository;

    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC: Build the Google OAuth URL — send user here to authorize
    // ─────────────────────────────────────────────────────────────────────────
    public String buildAuthorizationUrl(Long ticketId) throws Exception {
        GoogleAuthorizationCodeFlow flow = buildFlow();
        String redirectUri = appBaseUrl + "/api/auth/google/callback";

        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(ticketId.toString())
                .setAccessType("offline")
                .set("prompt", "consent")   // forces refresh_token to be returned
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC: Handle Google's callback — exchange code for tokens, save to DB
    // ─────────────────────────────────────────────────────────────────────────
    public void handleCallback(String code, String state) throws Exception {
        GoogleAuthorizationCodeFlow flow = buildFlow();
        String redirectUri = appBaseUrl + "/api/auth/google/callback";

        TokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        // Persist tokens to PostgreSQL
        GoogleToken token = tokenRepository.findById(calendarAccountEmail)
                .orElse(new GoogleToken());

        token.setEmail(calendarAccountEmail);
        token.setAccessToken(response.getAccessToken());

        // Only overwrite refresh token if Google returned a new one
        if (response.getRefreshToken() != null) {
            token.setRefreshToken(response.getRefreshToken());
        }

        token.setExpirationTimeMs(
                response.getExpiresInSeconds() != null
                        ? System.currentTimeMillis() + (response.getExpiresInSeconds() * 1000)
                        : null
        );

        tokenRepository.save(token);
        System.out.println("Google tokens saved to DB for: " + calendarAccountEmail);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC: Check if we already have a valid stored token
    // ─────────────────────────────────────────────────────────────────────────
    public boolean isAuthorized() {
        return tokenRepository.findById(calendarAccountEmail)
                .map(t -> t.getRefreshToken() != null && !t.getRefreshToken().isBlank())
                .orElse(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC: Create a flight event on Google Calendar
    // ─────────────────────────────────────────────────────────────────────────
    public String[] createFlightEvent(Ticket ticket, String userEmail) {
        try {
            Calendar service = buildCalendarServiceFromDb();

            Event event = new Event()
                    .setSummary("✈ Flight: " + ticket.getFlight().getSource()
                                + " → " + ticket.getFlight().getDestination())
                    .setDescription(buildEventDescription(ticket))
                    .setLocation(ticket.getFlight().getSource() + " Airport");

            com.google.api.client.util.DateTime startDateTime = toGoogleDateTime(
                    ticket.getFlight().getDepartureTime()
                          .atZone(ZoneId.systemDefault())
                          .toInstant()
                          .toEpochMilli()
            );
            com.google.api.client.util.DateTime endDateTime = toGoogleDateTime(
                    ticket.getFlight().getArrivalTime()
                          .atZone(ZoneId.systemDefault())
                          .toInstant()
                          .toEpochMilli()
            );

            event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone("Asia/Karachi"));
            event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone("Asia/Karachi"));

            Event created = service.events().insert(calendarId, event).execute();

            System.out.println("Calendar event created — ID: " + created.getId());
            System.out.println("Calendar event URL: " + created.getHtmlLink());

            return new String[]{ created.getId(), created.getHtmlLink() };

        } catch (Exception e) {
            System.err.println("Google Calendar event creation failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC: Delete a flight event from Google Calendar
    // ─────────────────────────────────────────────────────────────────────────
    public void deleteFlightEvent(String eventId, String userEmail) {
        try {
            if (eventId == null || eventId.isBlank()) return;
            Calendar service = buildCalendarServiceFromDb();
            service.events().delete(calendarId, eventId).execute();
            System.out.println("Calendar event deleted — ID: " + eventId);
        } catch (Exception e) {
            System.err.println("Google Calendar event deletion failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Build the OAuth flow (no token storage — we use DB instead)
    // ─────────────────────────────────────────────────────────────────────────
    private GoogleAuthorizationCodeFlow buildFlow() throws Exception {
        InputStream in = resourceLoader.getResource(getCredentialsPath()).getInputStream();
        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

        return new GoogleAuthorizationCodeFlow.Builder(transport, JSON_FACTORY, secrets, SCOPES)
                .setAccessType("offline")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Build Calendar client using tokens from PostgreSQL
    // ─────────────────────────────────────────────────────────────────────────
    private Calendar buildCalendarServiceFromDb() throws Exception {
        GoogleToken stored = tokenRepository.findById(calendarAccountEmail)
                .orElseThrow(() -> new RuntimeException(
                        "No Google token found in DB. Visit /api/auth/google/authorize?ticketId=1 to authorize."));

        if (stored.getRefreshToken() == null || stored.getRefreshToken().isBlank()) {
            throw new RuntimeException("Refresh token is missing. Re-authorize at /api/auth/google/authorize?ticketId=1");
        }

        InputStream in = resourceLoader.getResource(getCredentialsPath()).getInputStream();
        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(transport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(
                        secrets.getDetails().getClientId(),
                        secrets.getDetails().getClientSecret()
                )
                .build();

        credential.setAccessToken(stored.getAccessToken());
        credential.setRefreshToken(stored.getRefreshToken());

        // Refresh access token if it's expiring within 60 seconds
        boolean isExpiringSoon = stored.getExpirationTimeMs() != null
                && stored.getExpirationTimeMs() - System.currentTimeMillis() < 60_000;

        if (isExpiringSoon) {
            System.out.println("Access token expiring soon — refreshing...");
            boolean refreshed = credential.refreshToken();

            if (refreshed) {
                stored.setAccessToken(credential.getAccessToken());
                stored.setExpirationTimeMs(
                        credential.getExpiresInSeconds() != null
                                ? System.currentTimeMillis() + credential.getExpiresInSeconds() * 1000
                                : null
                );
                tokenRepository.save(stored);
                System.out.println("Access token refreshed and saved to DB.");
            } else {
                System.err.println("Token refresh failed — user may need to re-authorize.");
            }
        }

        return new Calendar.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE: Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private String buildEventDescription(Ticket ticket) {
        return String.format(
                "SkyBook Booking Confirmation\n\nTicket: %s\nPassenger: %s\nAirline: %s\nFlight: %s\nSeat: %s\nPrice: USD %s",
                ticket.getId(),
                ticket.getPassengerName(),
                ticket.getFlight().getAirline(),
                ticket.getFlight().getId(),
                ticket.getSeatNumber(),
                ticket.getFlight().getPrice()
        );
    }

    private com.google.api.client.util.DateTime toGoogleDateTime(long epochMilli) {
        return new com.google.api.client.util.DateTime(epochMilli);
    }
}