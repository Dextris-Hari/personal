package com.navate.service;


import com.navate.model.MobileOtp;
import com.navate.model.UserEntity;
import com.navate.repository.MobileOtpRepository;
import com.navate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;

@Service
public class MobileOtpService {

    private final MobileOtpRepository otpRepo;
    private final UserRepository userRepo;

    @Value("${navate.app.otp-expiration-ms}") // default 5 min
    private long otpTtlMs;

    public MobileOtpService(MobileOtpRepository otpRepo, UserRepository userRepo) {
        this.otpRepo = otpRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public boolean requestOtp(String mobile) {
        // ensure a user exists with this mobile (or optionally allow registration flow)
        UserEntity user = userRepo.findByMobileNumber(mobile).orElse(null);
        if (user == null) return false;

        String code = generateCode();
        MobileOtp otp = new MobileOtp();
        otp.setMobileNumber(mobile);
        otp.setCode(code);
        otp.setExpiryAt(Instant.now().plusMillis(otpTtlMs));
        otpRepo.saveAndFlush(otp);

        // TODO: integrate SMS gateway. For now we log/print.
        System.out.printf("DEBUG: OTP for %s = %s (expires in %d ms)%n", mobile, code, otpTtlMs);
        return true;
    }

    @Transactional(readOnly = true)
    public MobileOtp findLatest(String mobile) {
        return otpRepo.findTopByMobileNumberOrderByCreatedAtDesc(mobile).orElse(null);
    }

    @Transactional
    public boolean verifyOtp(String mobile, String code) {
        MobileOtp otp = findLatest(mobile);
        if (otp == null) return false;
        if (otp.isUsed()) return false;
        if (otp.getExpiryAt().isBefore(Instant.now())) return false;
        if (!otp.getCode().equals(code)) return false;
        // mark used
        otp.setUsed(true);
        otpRepo.save(otp);
        return true;
    }

    private String generateCode() {
        int n = 100_000 + new Random().nextInt(900_000);
        return Integer.toString(n);
    }
}
