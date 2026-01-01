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

@Controller
public class GameController {

    @Autowired private GameService gameService;

    @Autowired private SimpMessagingTemplate messagingTemplate;

    /**
     * Client sends to: /app/game.join
     * Server replies to: /user/queue/match
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
     * Client sends to: /app/game.move
     * Payload: { "y": 50.0 }
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
     * Handle WebSocket Disconnects
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        gameService.playerDisconnected(sessionId);
    }
}
