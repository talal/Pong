package paf_grp_i.pong.security;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing a blacklist of revoked JWT tokens.
 *
 * This service maintains an in-memory, thread-safe set of blacklisted tokens,
 * primarily used to invalidate tokens after logout or when revocation is required.
 * Tokens added to the blacklist are considered invalid for authentication purposes.
 *
 */
@Service
public class JwtBlacklistService {

    // thread-safe set for revoked tokens (esp. after logout)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    // add token to blacklist
    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    /**
     * Checks whether a JWT token has been blacklisted.
     *
     * @param token the JWT token to check
     * @return {@code true} if the token is blacklisted, {@code false} otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    /**
     * Removes a JWT token from the blacklist.
     *
     * This is an optional operation that allows tokens to be un-revoked if needed.
     * This operation is thread-safe.
     *
     *
     * @param token the JWT token to remove from the blacklist
     */
    public void removeToken(String token) {
        blacklistedTokens.remove(token);
    }
}
