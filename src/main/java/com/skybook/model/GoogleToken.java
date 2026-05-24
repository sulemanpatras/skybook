package com.skybook.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "google_tokens")
@Data
public class GoogleToken {

    @Id
    private String email;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private Long expirationTimeMs;
}