package paf_grp_i.pong.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import paf_grp_i.pong.security.JwtTokenService;

@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {

	private final JwtTokenService jwt;
	private final UserDetailsService uds;

	public JwtStompChannelInterceptor(JwtTokenService jwt, UserDetailsService uds) {
		this.jwt = jwt;
		this.uds = uds;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
			String bearer = accessor.getFirstNativeHeader("Authorization");

			if (bearer != null && bearer.startsWith("Bearer ")) {
				String token = bearer.substring(7);
				if (jwt.isValid(token)) {
					String username = jwt.getUsername(token);
					var user = uds.loadUserByUsername(username);
					var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
					accessor.setUser(auth);
				} else {
					throw new IllegalArgumentException("Invalid JWT");
				}
			}
		}
		return message;
	}
}
