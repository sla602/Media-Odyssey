package com.mo.mediaodyssey.auth.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import com.mo.mediaodyssey.shared.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "verification_tokens")
public class VerificationToken implements Serializable {

    // ** Data Members **

    // Must be fixed. Randomly generated causes mismatch.
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false)
    private Date expiryDate;

    @Column(nullable = false)
    private Date calculateExpiryDate(int expiryTimeInMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTimeInMillis());
    }

    // ** Constructors **

    /**
     * JPA requires a no-arg public or protected constructor.
     * This protected constructor is not used anywhere else in the app, but must be
     * present for JPA to function properly.
     *
     * See following link for details:
     * https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html#:~:text=The%20entity%20class%20must%20have%20a%20public%20or%20protected%20constructor%20with%20no%20parameters
     **/
    protected VerificationToken() {
    }

    public VerificationToken(String token, User user, int expiryTimeInMinutes) {
        this.token = token;
        this.user = user;
        this.expiryDate = calculateExpiryDate(expiryTimeInMinutes);
    }

    // ** Getters and Setters **

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }
}
