package paf_grp_i.pong.security;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtBlacklistService {

    // thread-safe set for revoked tokens (esp. after logout)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    // add token to blacklist
    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    // optional
    public void removeToken(String token) {
        blacklistedTokens.remove(token);
    }
}
