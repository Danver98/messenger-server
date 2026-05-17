package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.auth.AuthDTO;
import com.danver.messengerserver.auth.AuthData;
import com.danver.messengerserver.auth.JwtUtil;
import com.danver.messengerserver.exceptions.AuthenticationException;
import com.danver.messengerserver.models.RefreshToken;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.repositories.interfaces.RefreshTokenRepository;
import com.danver.messengerserver.services.interfaces.UserService;
import com.danver.messengerserver.utils.Constants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.danver.messengerserver.utils.Constants.ACCESS_TOKEN_BLACKLIST_KEY;

@Slf4j
@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    private final UserService userService;
    private final RefreshTokenRepository tokenRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int REFRESH_TOKEN_BATCH_SIZE = 2000;

    @Autowired
    public AuthService(JwtUtil jwtUtil, AuthenticationManager authenticationManager, UserService userService, RefreshTokenRepository tokenRepository, RedisTemplate<String, String> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.redisTemplate = redisTemplate;
    }

    public AuthData login(AuthDTO authDTO) {
        User user = userService.getUserByEmail(authDTO.getLogin());
        if (user == null) {
            throw new BadCredentialsException("Cannot find user got login %s".formatted(authDTO.getLogin()));
        }
        String existingRefreshToken = tokenRepository.findTokenByUserIdAndDeviceId(user.getId(), authDTO.getDeviceId());
        if (existingRefreshToken != null) {
            throw new AuthenticationException("user %d is already logged".formatted(user.getId()));
        }
        // Check password
        authenticationManager.authenticate
                (new UsernamePasswordAuthenticationToken(authDTO.getLogin(), authDTO.getPassword()));
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        tokenRepository.save(new RefreshToken(user.getId(), refreshToken, authDTO.getDeviceId()));
        return AuthData.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(User user, HttpServletResponse response, String accessToken, String deviceId) {
        // Delete refresh token form db
        tokenRepository.deleteByUserIdAndDevice(user.getId(), deviceId);
        // Add access token to blacklist
        Claims claims = jwtUtil.getClaims(accessToken);
        Date expiration = claims.getExpiration();
        Date now = new Date();
        if (expiration.after(now)) {
            // Note: Redis supports hash field expiration only since 7.4 version.
            redisTemplate.opsForHash().put(ACCESS_TOKEN_BLACKLIST_KEY, accessToken, "1");
            long millisToExpire = expiration.getTime() - now.getTime();
            redisTemplate.opsForHash().expire(ACCESS_TOKEN_BLACKLIST_KEY, Duration.ofMillis(millisToExpire), List.of(accessToken));
        }
        SecurityContextHolder.getContext().setAuthentication(null);
        if (response.containsHeader(HttpHeaders.AUTHORIZATION)) {
            response.setHeader(HttpHeaders.AUTHORIZATION, null);
        }
    }

    public AuthData getNewRefreshToken(String refreshToken, String deviceId) {
        Claims claims = jwtUtil.validateAndParseRefreshTokenIfExpired(refreshToken);
        long userId = Long.parseLong(claims.getSubject());

        String dbRefreshToken = this.tokenRepository.findTokenByUserIdAndDeviceId(userId, deviceId);
        if (dbRefreshToken == null || !dbRefreshToken.equals(refreshToken)) {
            throw new AuthenticationException("Wrong refresh token provided for user %d".formatted(userId));
        }
        User user = (User) userService.loadUserByUsername((String) claims.get(Constants.USER_JWT_LOGIN_KEY));
        // Delete old token from db
        tokenRepository.deleteByUserIdAndDevice(user.getId(), deviceId);
        String accessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        tokenRepository.save(new RefreshToken(userId, newRefreshToken, deviceId));
        return AuthData.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Scheduled(fixedDelay = 3_600_000) // hourly
    public void deleteCorruptedRefreshTokens() {
        log.info("Starting deletion of corrupted refresh tokens");
        int page = 0;
        int totalDeleted = 0;
        int totalProcessed = 0;
        boolean hasNext = true;
        List<Long> idsToDelete = new ArrayList<>();

        while (hasNext) {
            Pageable pageable = PageRequest.of(page, REFRESH_TOKEN_BATCH_SIZE);
            Page<RefreshToken> refreshTokensPage = tokenRepository.findAll(pageable);

            List<RefreshToken> refreshTokens = refreshTokensPage.getContent();
            totalProcessed += refreshTokens.size();

            // Find invalid tokens
            for (RefreshToken token : refreshTokens) {
                try {
                    if (!jwtUtil.validateToken(token.getToken())) {
                        idsToDelete.add(token.getId());
                    }
                } catch (Exception e) {
                    idsToDelete.add(token.getId());
                }
            }

            // Batch delete if we have accumulated enough IDs
            if (idsToDelete.size() >= REFRESH_TOKEN_BATCH_SIZE) {
                totalDeleted += batchDeleteTokens(idsToDelete);
                idsToDelete.clear();
            }

            hasNext = refreshTokensPage.hasNext();
            page++;
        }

        // Delete remaining IDs
        if (!idsToDelete.isEmpty()) {
            totalDeleted += batchDeleteTokens(idsToDelete);
        }

        log.info("Completed deletion. Processed: {}, Deleted: {}", totalProcessed, totalDeleted);
    }

    /**
     * Batch delete tokens by IDs
     */
    private int batchDeleteTokens(List<Long> ids) {
        if (ids.isEmpty()) return 0;
        return tokenRepository.deleteAllByIds(ids);
    }
}
