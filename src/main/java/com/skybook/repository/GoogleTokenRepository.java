package com.skybook.repository;

import com.skybook.model.GoogleToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoogleTokenRepository extends JpaRepository<GoogleToken, String> {}