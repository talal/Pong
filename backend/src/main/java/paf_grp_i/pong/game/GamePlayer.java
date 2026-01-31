package paf_grp_i.pong.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a player in the Pong game.
 */
@Getter
@Setter
public class GamePlayer {
    @Setter(AccessLevel.NONE)
    private String sessionId; // WebSocket Session ID

    @Setter(AccessLevel.NONE)
    private String username;

    private int score;
    private double y; // Paddle vertical position

    /**
     * Creates a new game player.
     *
     * @param sessionId the WebSocket session ID
     * @param username the player's username
     */
    public GamePlayer(String sessionId, String username) {
        this.sessionId = sessionId;
        this.username = username;
        this.score = 0;
        this.y = 50.0; // Start in the middle (0-100 scale)
    }

    public void incrementScore() {
        this.score++;
    }
}
