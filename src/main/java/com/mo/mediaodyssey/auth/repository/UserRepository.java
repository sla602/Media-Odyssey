package com.mo.mediaodyssey.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mo.mediaodyssey.shared.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // used by auth
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

}