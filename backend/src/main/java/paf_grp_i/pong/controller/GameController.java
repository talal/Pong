package paf_grp_i.pong.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import paf_grp_i.pong.game.Game;
import paf_grp_i.pong.service.GameService;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket controller for handling real-time Pong game interactions.
 * <p>
 * This controller manages game matchmaking requests, paddle movement updates,
 * and player disconnection events via STOMP over WebSocket protocol.
 * </p>
 */
@Controller
public class GameController {

    @Autowired private GameService gameService;

    @Autowired private SimpMessagingTemplate messagingTemplate;

    /**
     * Handles game join requests from clients attempting to find a match.
     * <p>
     * When a client sends a message to {@code /app/game.join}, this method attempts
     * to match them with another waiting player. If a match is found, both players
     * receive a private message at {@code /user/queue/match} containing the game ID
     * they should subscribe to for game updates.
     * </p>
     *
     * @param headerAccessor accessor for WebSocket session metadata
     * @param principal the authenticated user principal, or null for anonymous users
     */
    @MessageMapping("/game.join")
    public void joinGame(SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        String sessionId = headerAccessor.getSessionId();
        String username = (principal != null) ? principal.getName() : "Anonymous";

        Game game = gameService.joinGame(sessionId, username);

        if (game != null) {
            // Match found! Notify both players privately.
            // We send the 'gameId' so they can subscribe to /topic/game/{gameId}

            Map<String, String> response = Map.of("gameId", game.getId());

            // Notify Player 1
            messagingTemplate.convertAndSendToUser(
                    game.getPlayer1().getUsername(), "/queue/match", response);

            // Notify Player 2
            messagingTemplate.convertAndSendToUser(
                    game.getPlayer2().getUsername(), "/queue/match", response);
        }
    }

    /**
     * Handles paddle movement updates from clients.
     * <p>
     * When a client sends a message to {@code /app/game.move} with a payload
     * containing the new paddle Y-coordinate, this method updates the player's
     * paddle position in their active game. The payload format is:
     * {@code { "y": 50.0 }}
     * </p>
     *
     * @param payload the movement data containing the Y-coordinate
     * @param headerAccessor accessor for WebSocket session metadata
     */
    @MessageMapping("/game.move")
    public void movePaddle(
            @Payload Map<String, Double> payload, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (payload.containsKey("y")) {
            gameService.movePaddle(sessionId, payload.get("y"));
        }
    }

    /**
     * Handles WebSocket disconnection events.
     * <p>
     * When a player disconnects, this method notifies the game service to handle
     * cleanup, end the game if in progress, and award the remaining player a win.
     * This event is triggered automatically by the Spring WebSocket framework.
     * </p>
     *
     * @param event the session disconnect event containing session information
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        gameService.playerDisconnected(sessionId);
    }
}
