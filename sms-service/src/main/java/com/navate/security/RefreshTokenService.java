package com.navate.security;


import com.navate.model.RefreshToken;
import com.navate.model.UserEntity;
import com.navate.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${navate.app.jwtRefreshExpirationMs}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken createRefreshToken(UserEntity user) {
        // remove existing tokens for the user (optional)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        rt.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        return refreshTokenRepository.save(rt);
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElse(null);
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }

    public void deleteByUser(UserEntity user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
