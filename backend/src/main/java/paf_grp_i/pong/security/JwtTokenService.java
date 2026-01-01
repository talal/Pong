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

@Service
public class JwtTokenService {
    @Value("${jwt.secret}") // see application.properties, min length 256 bit for HS256
    private String secretBase64;

    @Value("${jwt.expirationSeconds:3600}")
    private long expirationSeconds;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String generateToken(UserDetails user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }
}
