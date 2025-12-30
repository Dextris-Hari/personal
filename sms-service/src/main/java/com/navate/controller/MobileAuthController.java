package com.navate.controller;


import com.navate.model.RefreshToken;
import com.navate.model.UserEntity;
import com.navate.repository.UserRepository;
import com.navate.request.MobileRequest;
import com.navate.request.MobileVerifyRequest;
import com.navate.response.AuthResponse;
import com.navate.response.RefreshTokenResponse;
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

    record RefreshRequest(String refreshToken){}
    record LogoutRequest(String refreshToken){}

    @PostMapping("/login")
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

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        var existing = refreshTokenService.findByToken(request.refreshToken());
        if (existing == null) return ResponseEntity.status(401).body("Invalid refresh token");
        if (refreshTokenService.isExpired(existing)) {
            // cleanup
            refreshTokenService.deleteByUser(existing.getUser());
            return ResponseEntity.status(401).body("Refresh token expired");
        }

        UserEntity user = existing.getUser();
        var roles = user.getRole().stream().map(Enum::name).collect(Collectors.toSet());
        var newAccess = jwtService.generateToken(user.getUsername(), roles);

        // rotate refresh token: delete old and create new
        refreshTokenService.deleteByUser(user);
        RefreshToken newRt = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new RefreshTokenResponse(newAccess, newRt.getToken()));
    }

    /**
     * Logout:
     * - If Authorization Bearer access token present -> extract username and revoke all refresh tokens for that user.
     * - Or accept JSON body with refreshToken to revoke its user.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) LogoutRequest body
    ) {
        // try Authorization header first
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                String username = jwtService.extractUsername(token);
                userRepo.findByUsername(username).ifPresent(u -> refreshTokenService.deleteByUser(u));
                return ResponseEntity.ok("Logged out");
            } catch (Exception e) {
                // fallthrough to check body
            }
        }

        if (body != null && body.refreshToken() != null) {
            var existing = refreshTokenService.findByToken(body.refreshToken());
            if (existing != null) {
                refreshTokenService.deleteByUser(existing.getUser());
                return ResponseEntity.ok("Logged out");
            } else {
                return ResponseEntity.status(400).body("Invalid refresh token");
            }
        }

        return ResponseEntity.badRequest().body("No token provided");
    }
}