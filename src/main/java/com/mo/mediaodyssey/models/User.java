package com.mo.mediaodyssey.models;

import jakarta.persistence.*;

import java.time.Instant;


@Entity
@Table(name = "users",
        indexes = {
                @Index(name="idx_user_username", columnList = "username", unique = true)
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="user_name", nullable = false, unique=true, length = 50)
    private String username;


    @Column(nullable = false, unique=true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public User(){}


    public User(String userName, String email, String password) {
        this.username = userName;
        this.email = email;
        this.password = password;
    }

    public int getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }
}
