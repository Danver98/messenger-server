package com.danver.messengerserver.auth;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.utils.Constants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class JwtUtil {

    private final Environment env;
    private final SecretKey secret;
    @Autowired
    public JwtUtil(Environment env) {
        this.env = env;
        this.secret = Keys.hmacShaKeyFor(Objects.requireNonNull(env.getProperty("jwt.secret")).getBytes());
        //this.secret = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(env.getProperty("jwt.secret")));
    }
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.USER_JWT_LOGIN_KEY, user.getEmail());
        String subject = Long.toString(user.getId());
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .expiration(new Date(System.currentTimeMillis() + Long.parseLong(Objects.requireNonNull(env.getProperty("jwt.exp-in-millis")))))
                .issuedAt(new Date())
                .issuer(env.getProperty("jwt.iss"))
                .signWith(secret)
                .compact();
        //SignatureAlgorithm.valueOf(env.getProperty("jwt.sign-alg")))
    }
    public String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    public boolean validateToken(String token) {
        try {
            log.info("Checking token " + token + " for validity");
            Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parse(token);
            return true;
        } catch (SecurityException | ExpiredJwtException | UnsupportedJwtException | MalformedJwtException |
                 SignatureException | IllegalArgumentException e) {
            log.info("Token is invalid: " + e.getMessage());
            return false;
            // TODO: or generate exception?
        }
    }

    /**
     * Validates token and get its Claims
     * else returns
     *
     * @param token
     * @return Claims if success; else null
     */
    public Claims validateAndParse(String token) {
        try {
            return (Claims) Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parse(token)
                    .getPayload();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            log.info("Token is invalid: " + e.getMessage());
            return null;
        }
    }
    public Claims getClaims(String token) {
        return (Claims) Jwts.parser()
                .verifyWith(secret)
                .build()
                .parse(token)
                .getPayload();
    }
}
