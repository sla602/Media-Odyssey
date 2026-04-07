package com.mo.mediaodyssey.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mo.mediaodyssey.auth.model.VerificationToken;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    // Repository containing VerificationToken for email verification during sign up
    // in authentication.
    Optional<VerificationToken> findByToken(String token);
}