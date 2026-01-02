package paf_grp_i.pong.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LeaderboardController {

    @Autowired private UserRepository userRepository;

    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        // Fetch all users and sort in memory by Win Rate (Descending)
        return userRepository.findAll().stream()
                .sorted((u1, u2) -> Double.compare(getWinRate(u2), getWinRate(u1)))
                .map(this::mapUserToLeaderboardEntry)
                .toList();
    }

    private double getWinRate(User user) {
        if (user.getGamesPlayed() == 0) return 0.0;
        return (double) user.getGamesWon() / user.getGamesPlayed();
    }

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
