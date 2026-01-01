package paf_grp_i.pong.game;

public class GamePlayer {
    private String sessionId; // WebSocket Session ID
    private String username;
    private int score;
    private double y; // Paddle vertical position

    public GamePlayer(String sessionId, String username) {
        this.sessionId = sessionId;
        this.username = username;
        this.score = 0;
        this.y = 50.0; // Start in the middle (0-100 scale)
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void incrementScore() {
        this.score++;
    }
}
