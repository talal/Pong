package paf_grp_i.pong.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

/**
 * Service for generating, parsing, and validating JWT tokens.
 * <p>
 * This service uses HMAC-SHA256 (HS256) for token signing and requires a
 * base64-encoded secret key of at least 256 bits configured in application properties.
 * Tokens include the username as the subject and have a configurable expiration time.
 * </p>
 */
@Service
public class JwtTokenService {
    @Value("${jwt.secret}") // see application.properties, min length 256 bit for HS256
    private String secretBase64;

    @Value("${jwt.expirationSeconds:3600}")
    private long expirationSeconds;

    /**
     * Creates the HMAC secret key from the base64-encoded configuration.
     *
     * @return the decoded {@link SecretKey} for JWT signing and verification
     */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    /**
     * Gets the configured token expiration time in seconds.
     *
     * @return the number of seconds until a token expires
     */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    /**
     * Generates a new JWT token for the specified user.
     * <p>
     * The token includes the username as the subject, current timestamp as issued-at,
     * and an expiration time based on the configured duration. The token is signed
     * using HS256 with the configured secret key.
     * </p>
     *
     * @param user the user details containing the username
     * @return a compact, signed JWT token string
     */
    public String generateToken(UserDetails user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses and validates a JWT token, returning its claims.
     * <p>
     * This method verifies the token signature and expiration. If the token
     * is invalid, expired, or malformed, a {@link JwtException} is thrown.
     * </p>
     *
     * @param token the JWT token string to parse
     * @return the {@link Claims} extracted from the token
     * @throws JwtException if the token is invalid, expired, or malformed
     */
    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Validates whether a JWT token is properly signed and not expired.
     *
     * @param token the JWT token string to validate
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT token string
     * @return the username stored in the token's subject claim
     * @throws JwtException if the token is invalid or cannot be parsed
     */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }
}
