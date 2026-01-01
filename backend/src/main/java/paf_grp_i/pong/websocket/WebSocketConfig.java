package paf_grp_i.pong.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompChannelInterceptor jwtInterceptor;

    public WebSocketConfig(JwtStompChannelInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtInterceptor); // JWT for STOMP
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // public messages via /topic, private messages via /queue
        registry.enableSimpleBroker("/topic", "/queue");
        // prefix(es) for messages that have to be processed by a controller
        registry.setApplicationDestinationPrefixes("/app");
        // prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/websocket") // has to match brokerURL in chat(-jwt).js
                .setAllowedOriginPatterns("*"); // restrict in production use
        ;
    }
}
