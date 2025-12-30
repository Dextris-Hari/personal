package com.navate.repository;


import com.navate.model.MobileOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MobileOtpRepository extends JpaRepository<MobileOtp, Long> {
    Optional<MobileOtp> findTopByMobileNumberOrderByCreatedAtDesc(String mobileNumber);
}