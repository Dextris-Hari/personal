package com.navate.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "tbl_mobile_otps", indexes = @Index(name = "idx_mobile", columnList = "mobile_number"))
@Data
public class MobileOtp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_number", nullable = false, length = 50)
    private String mobileNumber;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "expiry_at", nullable = false)
    private Instant expiryAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "used", nullable = false)
    private boolean used = false;


}