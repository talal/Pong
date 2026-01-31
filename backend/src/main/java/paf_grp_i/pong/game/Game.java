package paf_grp_i.pong.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a Pong game instance.
 *
 * <p>This class encapsulates the complete state of a Pong game, including both players, the ball
 * position and velocity, and the current game state. Each game is assigned a unique identifier upon
 * creation.
 *
 * <p>Coordinate system: All positions use a normalized range from 0.0 to 100.0 for both X and Y
 * axes.
 */
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

    /**
     * Constructs a new {@code Game} instance.
     *
     * <p>Initializes the game with a randomly generated unique identifier and sets the initial
     * state to {@link GameState#WAITING_FOR_PLAYER}.
     */
    public Game() {
        this.id = UUID.randomUUID().toString();
        this.state = GameState.WAITING_FOR_PLAYER;
    }
}
