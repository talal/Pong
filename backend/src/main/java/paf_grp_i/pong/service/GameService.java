package paf_grp_i.pong.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import paf_grp_i.pong.game.Game;
import paf_grp_i.pong.game.GamePlayer;
import paf_grp_i.pong.game.GameState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class GameService {

    // Holds all active games, mapped by Game ID
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();

    // Map to quickly find which game a player (sessionId) belongs to
    private final Map<String, String> playerGameMap = new ConcurrentHashMap<>();

    // Simple matchmaking queue
    private final ConcurrentLinkedQueue<GamePlayer> waitingPlayers = new ConcurrentLinkedQueue<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Attempts to join a player to a game.
     * If a player is waiting, matches them. Otherwise, puts this player in the waiting queue.
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

            System.out.println("Game Started: " + game.getId() + " (" + opponent.getUsername() + " vs " + newPlayer.getUsername() + ")");
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
     * Handles player disconnection.
     */
    public void playerDisconnected(String sessionId) {
        // If waiting, just remove from queue
        waitingPlayers.removeIf(p -> p.getSessionId().equals(sessionId));

        // If in game, end the game
        String gameId = playerGameMap.remove(sessionId);
        if (gameId != null) {
            Game game = activeGames.remove(gameId);
            if (game != null) {
                game.setState(GameState.FINISHED);
                // Notify the other player (if any)
                messagingTemplate.convertAndSend("/topic/game/" + gameId, game);

                // Cleanup the other player's mapping
                String p1 = game.getPlayer1().getSessionId();
                String p2 = game.getPlayer2() != null ? game.getPlayer2().getSessionId() : null;

                if (p1 != null) playerGameMap.remove(p1);
                if (p2 != null) playerGameMap.remove(p2);
            }
        }
    }

    /**
     * THE GAME LOOP
     * Runs every ~17ms (approx 60 FPS) to update physics.
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

    private void updatePhysics(Game game) {
        // Move Ball
        game.setBallX(game.getBallX() + game.getBallDX());
        game.setBallY(game.getBallY() + game.getBallDY());

        // 1. Collision with Top/Bottom Walls (0 to 100 coordinates)
        if (game.getBallY() <= 0 || game.getBallY() >= 100) {
            game.setBallDY(game.getBallDY() * -1);
        }

        // 2. Collision with Paddles (Simplified)
        // Player 1 is on Left (X=0), Player 2 is on Right (X=100)
        // Assume Paddle is roughly 15 units high (from y-7.5 to y+7.5) and 2 units wide

        // Check Left Paddle (Player 1)
        if (game.getBallX() <= 2) {
            if (Math.abs(game.getBallY() - game.getPlayer1().getY()) < 10) {
                game.setBallDX(Math.abs(game.getBallDX())); // Bounce right
                increaseSpeed(game);
            } else if (game.getBallX() < 0) {
                // Goal for Player 2
                game.getPlayer2().incrementScore();
                resetBall(game);
            }
        }

        // Check Right Paddle (Player 2)
        if (game.getBallX() >= 98) {
            if (Math.abs(game.getBallY() - game.getPlayer2().getY()) < 10) {
                game.setBallDX(-Math.abs(game.getBallDX())); // Bounce left
                increaseSpeed(game);
            } else if (game.getBallX() > 100) {
                // Goal for Player 1
                game.getPlayer1().incrementScore();
                resetBall(game);
            }
        }
    }

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
