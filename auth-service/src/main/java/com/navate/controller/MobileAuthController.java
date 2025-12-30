package com.navate.controller;


import com.navate.model.RefreshToken;
import com.navate.model.UserEntity;
import com.navate.repository.UserRepository;
import com.navate.request.MobileRequest;
import com.navate.request.MobileVerifyRequest;
import com.navate.response.AuthResponse;
import com.navate.security.JwtService;
import com.navate.security.RefreshTokenService;
import com.navate.service.MobileOtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/mobile")
public class MobileAuthController {

    private final MobileOtpService otpService;
    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public MobileAuthController(MobileOtpService otpService,
                                UserRepository userRepo,
                                JwtService jwtService,
                                RefreshTokenService refreshTokenService) {
        this.otpService = otpService;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/request")
    public ResponseEntity<?> requestOtp(@RequestBody MobileRequest req) {
        boolean ok = otpService.requestOtp(req.getMobile());
        if (!ok) return ResponseEntity.badRequest().body("Mobile number not registered");
        return ResponseEntity.ok("OTP sent");
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody MobileVerifyRequest req) {
        boolean valid = otpService.verifyOtp(req.getMobile(), req.getCode());
        if (!valid) return ResponseEntity.status(401).body("Invalid or expired OTP");

        UserEntity user = userRepo.findByMobileNumber(req.getMobile()).orElse(null);
        if (user == null) return ResponseEntity.status(404).body("User not found");

        Set<String> roles = user.getRole().stream().map(Enum::name).collect(Collectors.toSet());
        String access = jwtService.generateToken(user.getUsername(), roles);

        RefreshToken rt = refreshTokenService.createRefreshToken(user);
        return ResponseEntity.ok(new AuthResponse(access, rt.getToken()));
    }
}