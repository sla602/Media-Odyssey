package com.mo.mediaodyssey.models;


import jakarta.persistence.*;


import jakarta.persistence.*;

@Entity
@Table(name="friendships")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="user_id", nullable = false)
    private Integer userId; // sender

    @Column(name="friend_id", nullable = false)
    private Integer friendId; // recipient

    @Column(nullable=false)
    private boolean accepted = false;

    public Friendship() {}

    public Friendship(Integer userId, Integer friendId) {
        this.userId = userId;
        this.friendId = friendId;
    }


    public Integer getId() { return id; }
    public Integer getUserId() { return userId; }
    public Integer getFriendId() { return friendId; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}