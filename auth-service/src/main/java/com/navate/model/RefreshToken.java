package com.navate.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "tbl_refresh_tokens")
@Data
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

//    // getters / setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//    public String getToken() { return token; }
//    public void setToken(String token) { this.token = token; }
//    public Instant getExpiryDate() { return expiryDate; }
//    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
//    public UserEntity getUser() { return user; }
//    public void setUser(UserEntity user) { this.user = user; }
}
