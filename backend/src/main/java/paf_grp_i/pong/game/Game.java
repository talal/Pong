package paf_grp_i.pong.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Game {
    @Setter(AccessLevel.NONE)
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
}
