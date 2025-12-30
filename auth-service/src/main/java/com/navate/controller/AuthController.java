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

                   String refreshToken = jwtService.generateRefreshToken(user.getUsername());

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

}
