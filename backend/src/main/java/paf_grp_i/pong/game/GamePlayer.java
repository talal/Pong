package paf_grp_i.pong.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GamePlayer {
    @Setter(AccessLevel.NONE)
    private String sessionId; // WebSocket Session ID

    @Setter(AccessLevel.NONE)
    private String username;

    private int score;
    private double y; // Paddle vertical position

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
