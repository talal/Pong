package paf_grp_i.pong.game;

/** Represents the current state of a Pong game. */
public enum GameState {
    /** Waiting for a second player to join. */
    WAITING_FOR_PLAYER,
    /** Game is actively being played. */
    PLAYING,
    /** Game has ended. */
    FINISHED
}
