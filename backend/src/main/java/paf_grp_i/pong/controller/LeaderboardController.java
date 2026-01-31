package paf_grp_i.pong.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

import java.util.List;
import java.util.Map;

/**
 * REST controller for retrieving game leaderboard statistics.
 * <p>
 * Provides endpoints to view player rankings sorted by win rate,
 * displaying games played, games won, and win percentage for all users.
 * </p>
 */
@RestController
@RequestMapping("/api")
public class LeaderboardController {

    @Autowired private UserRepository userRepository;

    /**
     * Retrieves the leaderboard with all users sorted by win rate in descending order.
     * <p>
     * Each entry includes the username (email), games played, games won, and win rate
     * formatted as a percentage. Users with no games played have a win rate of 0%.
     * The sorting is performed in-memory after fetching all users from the database.
     * </p>
     *
     * @return a list of leaderboard entries, each containing user statistics as a map
     */
    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        // Fetch all users and sort in memory by Win Rate (Descending)
        return userRepository.findAll().stream()
                .sorted((u1, u2) -> Double.compare(getWinRate(u2), getWinRate(u1)))
                .map(this::mapUserToLeaderboardEntry)
                .toList();
    }

    /**
     * Calculates the win rate for a user.
     * <p>
     * Win rate is computed as games won divided by games played.
     * Returns 0.0 if the user has not played any games to avoid division by zero.
     * </p>
     *
     * @param user the user whose win rate to calculate
     * @return the win rate as a decimal between 0.0 and 1.0
     */
    private double getWinRate(User user) {
        if (user.getGamesPlayed() == 0) return 0.0;
        return (double) user.getGamesWon() / user.getGamesPlayed();
    }

    /**
     * Maps a user entity to a leaderboard entry representation.
     * <p>
     * Creates a map containing the username (email), games won, games played,
     * and win rate formatted as a percentage string with one decimal place.
     * </p>
     *
     * @param user the user to map
     * @return a map containing the user's leaderboard statistics
     */
    private Map<String, Object> mapUserToLeaderboardEntry(User user) {
        double rate = getWinRate(user);
        return Map.of(
                "username", user.getEmail(),
                "gamesWon", user.getGamesWon(),
                "gamesPlayed", user.getGamesPlayed(),
                // Format as percentage (e.g., "75.0%")
                "winRate", String.format("%.1f%%", rate * 100));
    }
}
