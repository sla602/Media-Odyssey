package com.mo.mediaodyssey.layout.models;

import java.util.ArrayList;
import java.util.List;

import com.mo.mediaodyssey.shared.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "boards")
public class Boards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String board_name;
    private String board_description;
    private String board_type;

    // Connect to User (Determine which users own a board. A board must be owned by
    // a user.)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Connect to BoardMedia (store the media objects that were put in this board)
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardMedia> medias = new ArrayList<>();

    public Boards() {
    }

    public Boards(Long id, String board_name, String board_description, String board_type, User user,
            List<BoardMedia> medias) {
        this.id = id;
        this.board_name = board_name;
        this.board_description = board_description;
        this.board_type = board_type;
        this.user = user;
        this.medias = medias;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBoard_name() {
        return board_name;
    }

    public void setBoard_name(String board_name) {
        this.board_name = board_name;
    }

    public String getBoard_description() {
        return board_description;
    }

    public void setBoard_description(String board_description) {
        this.board_description = board_description;
    }

    public String getBoard_type() {
        return board_type;
    }

    public void setBoard_type(String board_type) {
        this.board_type = board_type;
    }

    // =========== FOR USER CONNECTION ===========

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // =========== For BoardMedia Connection ========
    public List<BoardMedia> getMedias() {
        return medias;
    }

    public void setMedias(List<BoardMedia> medias) {
        this.medias = medias;
    }

}
