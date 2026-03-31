package com.mo.mediaodyssey.socialFeature.models;


import jakarta.persistence.*;

@Entity
@Table(name="friendships")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId; // sender

    @Column(name="friend_id", nullable = false)
    private Long friendId; // recipient

    @Column(nullable=false)
    private boolean accepted = false;

    public Friendship() {}

    public Friendship(Long userId, Long friendId) {
        this.userId = userId;
        this.friendId = friendId;
    }


    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getFriendId() { return friendId; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}