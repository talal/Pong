package paf_grp_i.pong.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import paf_grp_i.pong.game.Game;
import paf_grp_i.pong.game.GamePlayer;
import paf_grp_i.pong.game.GameState;
import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service for managing Pong game sessions, matchmaking, and real-time gameplay.
 * <p>
 * This service handles player matchmaking through a waiting queue, maintains active
 * game sessions, processes player input, runs the game physics loop at approximately
 * 60 FPS, and persists game results to the database. All game state updates are
 * broadcasted to clients via WebSocket.
 * </p>
 */
@Service
public class GameService {

    // Holds all active games, mapped by Game ID
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();

    // Map to quickly find which game a player (sessionId) belongs to
    private final Map<String, String> playerGameMap = new ConcurrentHashMap<>();

    // Simple matchmaking queue
    private final ConcurrentLinkedQueue<GamePlayer> waitingPlayers = new ConcurrentLinkedQueue<>();

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private UserRepository userRepository;

    /**
     * Attempts to join a player to a game through matchmaking.
     * <p>
     * If another player is waiting in the queue, creates a new game between them.
     * Otherwise, adds this player to the waiting queue. Players already in a game
     * are ignored to prevent duplicate joins.
     * </p>
     *
     * @param sessionId the WebSocket session ID of the player
     * @param username the username of the player
     * @return the created {@link Game} if matched, or {@code null} if added to waiting queue
     */
    public synchronized Game joinGame(String sessionId, String username) {
        // If player is already in a game, ignore
        if (playerGameMap.containsKey(sessionId)) {
            return activeGames.get(playerGameMap.get(sessionId));
        }

        GamePlayer newPlayer = new GamePlayer(sessionId, username);

        // Check if someone is waiting
        GamePlayer opponent = waitingPlayers.poll();

        if (opponent != null) {
            // Match found! Create a new game
            Game game = new Game();
            game.setPlayer1(opponent);
            game.setPlayer2(newPlayer);
            game.setState(GameState.PLAYING);

            // Store references
            activeGames.put(game.getId(), game);
            playerGameMap.put(opponent.getSessionId(), game.getId());
            playerGameMap.put(newPlayer.getSessionId(), game.getId());

            System.out.printf(
                    "Game Started: %s (%s vs %s)%n",
                    game.getId(), opponent.getUsername(), newPlayer.getUsername());
            return game;
        } else {
            // No opponent, add to queue
            waitingPlayers.add(newPlayer);
            System.out.println("Player added to waiting queue: " + username);
            return null; // Return null to indicate waiting
        }
    }

    /**
     * Handles paddle movement input from a client.
     * <p>
     * Updates the vertical position of the player's paddle in their active game.
     * Ignores input if the player is not in a game or the game is not in progress.
     * </p>
     *
     * @param sessionId the WebSocket session ID of the player
     * @param y the new vertical position of the paddle (0-100 coordinate system)
     */
    public void movePaddle(String sessionId, double y) {
        String gameId = playerGameMap.get(sessionId);
        if (gameId == null) return;

        Game game = activeGames.get(gameId);
        if (game == null || game.getState() != GameState.PLAYING) return;

        // Update the correct player's position
        if (game.getPlayer1().getSessionId().equals(sessionId)) {
            game.getPlayer1().setY(y);
        } else if (game.getPlayer2().getSessionId().equals(sessionId)) {
            game.getPlayer2().setY(y);
        }
    }

    /**
     * Handles player disconnection from a game or waiting queue.
     * <p>
     * If the player is waiting, removes them from the queue. If in an active game,
     * ends the game immediately, awards the remaining player a win (score set to 11),
     * persists the result, and notifies the opponent via WebSocket.
     * </p>
     *
     * @param sessionId the WebSocket session ID of the disconnected player
     */
    public void playerDisconnected(String sessionId) {
        // If waiting, just remove from queue
        waitingPlayers.removeIf(p -> p.getSessionId().equals(sessionId));

        // If in game, end the game
        String gameId = playerGameMap.remove(sessionId);
        if (gameId != null) {
            Game game = activeGames.remove(gameId);

            // Only proceed if the game wasn't already finished (prevents double counting)
            if (game != null && game.getState() != GameState.FINISHED) {
                game.setState(GameState.FINISHED);

                // If Player 1 quit, set Player 2's score to 11.
                // If Player 2 quit, set Player 1's score to 11.
                if (game.getPlayer1().getSessionId().equals(sessionId)) {
                    game.getPlayer2().setScore(11);
                } else if (game.getPlayer2() != null
                        && game.getPlayer2().getSessionId().equals(sessionId)) {
                    game.getPlayer1().setScore(11);
                }

                // Now save the result (Logic in persistGameResult uses the scores we just updated)
                persistGameResult(game);

                // Notify the other player
                messagingTemplate.convertAndSend("/topic/game/" + gameId, game);

                // Cleanup maps
                String p1 = game.getPlayer1().getSessionId();
                String p2 = game.getPlayer2() != null ? game.getPlayer2().getSessionId() : null;

                if (p1 != null) playerGameMap.remove(p1);
                if (p2 != null) playerGameMap.remove(p2);
            }
        }
    }

    /**
     * The main game loop that updates physics for all active games.
     * <p>
     * Runs every approximately 17ms (about 60 FPS) to update ball position,
     * detect collisions, handle scoring, and broadcast updated game state
     * to all subscribed clients via WebSocket.
     * </p>
     */
    @Scheduled(fixedRate = 17)
    public void gameLoop() {
        for (Game game : activeGames.values()) {
            if (game.getState() == GameState.PLAYING) {
                updatePhysics(game);
                // Broadcast state to subscribers of this specific game
                messagingTemplate.convertAndSend("/topic/game/" + game.getId(), game);
            }
        }
    }

    /**
     * Checks if either player has reached the winning score of 11 points.
     * <p>
     * If a win condition is met, sets the game state to FINISHED and
     * persists the game result to the database.
     * </p>
     *
     * @param game the game to check
     */
    private void checkWinCondition(Game game) {
        if (game.getPlayer1().getScore() >= 11 || game.getPlayer2().getScore() >= 11) {
            game.setState(GameState.FINISHED);
            persistGameResult(game);
        }
    }

    /**
     * Persists game results to the database by updating player statistics.
     * <p>
     * Increments games played for both players and games won for the winner.
     * This method runs in a transaction to ensure data consistency.
     * </p>
     *
     * @param game the completed game whose results should be saved
     */
    @Transactional
    protected void persistGameResult(Game game) {
        User p1 = userRepository.findByEmail(game.getPlayer1().getUsername());
        User p2 = userRepository.findByEmail(game.getPlayer2().getUsername());

        if (p1 != null && p2 != null) {
            p1.setGamesPlayed(p1.getGamesPlayed() + 1);
            p2.setGamesPlayed(p2.getGamesPlayed() + 1);

            if (game.getPlayer1().getScore() > game.getPlayer2().getScore()) {
                p1.setGamesWon(p1.getGamesWon() + 1);
            } else {
                p2.setGamesWon(p2.getGamesWon() + 1);
            }

            userRepository.save(p1);
            userRepository.save(p2);
        }
    }

    /**
     * Updates game physics including ball movement, collision detection, and scoring.
     * <p>
     * Handles ball-wall collisions, ball-paddle collisions, goal detection,
     * and win condition checking. Ball speed increases slightly with each paddle hit.
     * </p>
     *
     * @param game the game to update
     */
    private void updatePhysics(Game game) {
        // Move Ball
        game.setBallX(game.getBallX() + game.getBallDX());
        game.setBallY(game.getBallY() + game.getBallDY());

        // 1. Collision with Top/Bottom Walls (0 to 100 coordinates)
        if (game.getBallY() <= 0 || game.getBallY() >= 100) {
            game.setBallDY(game.getBallDY() * -1);
        }

        // 2. Collision with Paddles (Simplified)
        // Player 1 (Left)
        if (game.getBallX() <= 2) {
            if (Math.abs(game.getBallY() - game.getPlayer1().getY()) < 10) {
                game.setBallDX(Math.abs(game.getBallDX())); // Bounce right
                increaseSpeed(game);
            } else if (game.getBallX() < 0) {
                // Goal for Player 2
                game.getPlayer2().incrementScore();
                checkWinCondition(game);
                resetBall(game);
            }
        }

        // Player 2 (Right)
        if (game.getBallX() >= 98) {
            if (Math.abs(game.getBallY() - game.getPlayer2().getY()) < 10) {
                game.setBallDX(-Math.abs(game.getBallDX())); // Bounce left
                increaseSpeed(game);
            } else if (game.getBallX() > 100) {
                // Goal for Player 1
                game.getPlayer1().incrementScore();
                checkWinCondition(game);
                resetBall(game);
            }
        }
    }

    /**
     * Resets the ball to center position after a goal.
     * <p>
     * Places the ball at coordinates (50, 50) and resets velocity to initial values.
     * The horizontal direction is reversed from the previous serve.
     * </p>
     *
     * @param game the game whose ball should be reset
     */
    private void resetBall(Game game) {
        game.setBallX(50);
        game.setBallY(50);
        // Reset speed but keep direction random-ish or alternate?
        // For simplicity, just serve to the loser or random
        game.setBallDX(game.getBallDX() > 0 ? -0.5 : 0.5);
        game.setBallDY(0.3);
    }

    private void increaseSpeed(Game game) {
        // Slight speed up on every hit to make it interesting
        game.setBallDX(game.getBallDX() * 1.05);
        game.setBallDY(game.getBallDY() * 1.05);
    }
}
