package com.mo.mediaodyssey.models;


import jakarta.persistence.*;

import java.time.Instant;


@Entity
@Table(name = "community",
indexes = {
        @Index(name="idx_community_name", columnList = "name", unique = true)
})
public class Community {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer createdByUserId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Community() {}

    public Community(String name, String description, Integer createdByUserId) {
        this.name = name;
        this.description = description;
        this.createdByUserId = createdByUserId;
    }

    public Integer getId(){
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
