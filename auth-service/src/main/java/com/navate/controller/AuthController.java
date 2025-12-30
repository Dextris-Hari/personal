package com.navate.controller;

import com.navate.model.RefreshToken;
import com.navate.model.UserEntity;
import com.navate.repository.UserRepository;
import com.navate.request.AuthRequest;
import com.navate.response.AuthResponse;
import com.navate.response.RefreshTokenResponse;
import com.navate.security.JwtService;
import com.navate.security.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserRepository repo, BCryptPasswordEncoder encoder, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.repo = repo; this.encoder = encoder; this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    //record AuthRequest(String username, String password){}
    //record AuthResponse(String token){}
    record RefreshRequest(String refreshToken){}
    record LogoutRequest(String refreshToken){}

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {

        return repo.findByUsername(req.getUserName())
                .filter(user -> encoder.matches(req.getPassword(), user.getPassword()))
                .map(user -> {

                    Set<String> roles = user.getRole()
                            .stream()
                            .map(role -> role.name())
                            .collect(Collectors.toSet());

                    String accessToken = jwtService.generateToken(
                            user.getUsername(), roles
                    );

                    // Persist refresh token in DB and return the stored token string
                    RefreshToken savedRt = refreshTokenService.createRefreshToken(user);
                    String refreshToken = savedRt.getToken();

                    return ResponseEntity.ok(
                            new AuthResponse(accessToken,refreshToken)
                    );
                })
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
                );
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
                repo.findByUsername(username).ifPresent(u -> refreshTokenService.deleteByUser(u));
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
