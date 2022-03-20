package com.danver.messengerserver.utils;

import com.danver.messengerserver.MessengerServerApplication;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.TextCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Component
public class AuthUtil {

    private static final Logger logger = LoggerFactory.getLogger(MessengerServerApplication.class.getName());

    private final Environment env;

    private enum HashingAlgorithm {

        SHA512("SHA-512"), PBKDF2("PBKDF2");

        private final String value;

        HashingAlgorithm(String alg) {
            this.value = alg;
        }

        String getValue() {
            return this.value;
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private final SigningKeyResolver signingKeyResolver = new SigningKeyResolverAdapter() {
        @Override
        public byte[] resolveSigningKeyBytes(JwsHeader header, Claims claims) {
            return TextCodec.BASE64.decode(env.getProperty("jwt.secret"));
        }
    };

    @Autowired
    AuthUtil(Environment env) {
        this.env = env;
    }

    /**
     * @param password - password to be hashed
     * @return password hash and salt
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static Object[] hashPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return new Object[]{generateHash(password, salt), salt};
    }

    public static String hashPasswordWithSalt(String password, byte[] salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        return generateHash(password, salt);
    }

    private static String generateHash(String password, byte[] salt, HashingAlgorithm... algorithm) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        byte [] hash;
        if (algorithm.length > 0 && algorithm[0] == HashingAlgorithm.PBKDF2) {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 655536, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            hash = factory.generateSecret(spec).getEncoded();
        } else {
            MessageDigest messageDigest = MessageDigest.getInstance(HashingAlgorithm.SHA512.getValue());
            messageDigest.update(salt);
            hash = messageDigest.digest(password.getBytes(StandardCharsets.UTF_8));
        }
        return bytesToHex(hash);
    }

    private static String bytesToHex(byte[] hash) {
        char[] hexChars = new char[hash.length * 2];
        for (int j = 0; j < hash.length; j++) {
            int v = hash[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public boolean tokenIsValid(String token) {
        try {
            logger.info("Checking token " + token + " for validity");
            Jwts.parser().setSigningKeyResolver(signingKeyResolver).parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            logger.info("Token is invalid: " + e.getMessage());
            return false;
        }
    }

    /**
     *
     * @param token
     * @return <b>Jws&lt;Claims&gt;</b> object if validation passed, otherwise <b>null</b>
     */
    public Jws<Claims> getParsedAndValidatedToken(String token) {
        try {
            logger.info("Checking token " + token + " for validity");
             return Jwts.parser().setSigningKeyResolver(signingKeyResolver).parseClaimsJws(token);
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            logger.info("Token is invalid: " + e.getMessage());
            return null;
        }
    }

    public String generateJWTToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(env.getProperty("jwt.iss"))
                .setSubject(subject)
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(Objects.requireNonNull(env.getProperty("jwt.exp-in-millis")))))
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.valueOf(env.getProperty("jwt.sign-alg")), TextCodec.BASE64.decode(env.getProperty("jwt.secret")))
                .compact();
    }
}