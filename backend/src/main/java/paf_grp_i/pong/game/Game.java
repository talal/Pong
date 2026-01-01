package paf_grp_i.pong.game;

import java.util.UUID;

public class Game {
    private String id;
    private GamePlayer player1;
    private GamePlayer player2;
    private GameState state;

    // Ball state (coordinates 0.0 to 100.0)
    private double ballX = 50.0;
    private double ballY = 50.0;
    private double ballDX = 0.5; // Horizontal speed
    private double ballDY = 0.3; // Vertical speed

    public Game() {
        this.id = UUID.randomUUID().toString();
        this.state = GameState.WAITING_FOR_PLAYER;
    }

    public String getId() {
        return id;
    }

    public GamePlayer getPlayer1() {
        return player1;
    }

    public void setPlayer1(GamePlayer player1) {
        this.player1 = player1;
    }

    public GamePlayer getPlayer2() {
        return player2;
    }

    public void setPlayer2(GamePlayer player2) {
        this.player2 = player2;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public double getBallX() {
        return ballX;
    }

    public void setBallX(double ballX) {
        this.ballX = ballX;
    }

    public double getBallY() {
        return ballY;
    }

    public void setBallY(double ballY) {
        this.ballY = ballY;
    }

    public double getBallDX() {
        return ballDX;
    }

    public void setBallDX(double ballDX) {
        this.ballDX = ballDX;
    }

    public double getBallDY() {
        return ballDY;
    }

    public void setBallDY(double ballDY) {
        this.ballDY = ballDY;
    }
}
