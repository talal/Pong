package paf_grp_i.pong.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import paf_grp_i.pong.model.ChatMessage;

/** 
 * WebSocket/STOMP controller for chat messages
 */
@Controller
public class ChatController {
	/**
	 * Endpoint for messages to "/app/chat" (see prefix "/app" specified in WebSocketConfig)
	 * Sends a chat message to all subscribers of "/topic/messages". 
	 * @param message The chat message to be sent.
	 * @param principal The author/sender of the message.
	 * @return The chat message to be sent, prefixed by the author's name
	 */
	@MessageMapping("/chat")
	@SendTo("/topic/messages")
	public ChatMessage sendChatMessage(ChatMessage message, Principal principal) {
		String content = message.getContent();
		content = principal.getName() + ": " + content;
		message.setContent(content);
		return message;
	}
}
