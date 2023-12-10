package com.danver.messengerserver.auth;

import com.danver.messengerserver.models.User;
import com.danver.messengerserver.utils.Constants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.TextCodec;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class JwtUtil {

    private final Environment env;

    private final SigningKeyResolver signingKeyResolver = new SigningKeyResolverAdapter() {
        @Override
        public byte[] resolveSigningKeyBytes(JwsHeader header, Claims claims) {
            return TextCodec.BASE64.decode(env.getProperty("jwt.secret"));
        }
    };

    @Autowired
    public JwtUtil(Environment env) {
        this.env = env;
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.USER_JWT_LOGIN_KEY, user.getEmail());
        String subject = Long.toString(user.getId());
        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(env.getProperty("jwt.iss"))
                .setSubject(subject)
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(Objects.requireNonNull(env.getProperty("jwt.exp-in-millis")))))
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.valueOf(env.getProperty("jwt.sign-alg")), TextCodec.BASE64.decode(env.getProperty("jwt.secret")))
                .compact();
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
            Jwts.parser().setSigningKeyResolver(signingKeyResolver).parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            log.info("Token is invalid: " + e.getMessage());
            return false;
            // TODO: or generate exception?
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKeyResolver(signingKeyResolver)
                .parseClaimsJws(token)
                .getBody();
    }
}
