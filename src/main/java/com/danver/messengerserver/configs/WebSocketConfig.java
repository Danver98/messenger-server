package com.danver.messengerserver.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import com.danver.messengerserver.utils.Constants;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //registry.addEndpoint(Constants.WS_MESSAGE_SERVICE_STOMP_ENDPOINT);
        registry.addEndpoint(Constants.WS_MESSAGE_SERVICE_STOMP_ENDPOINT)
                //.setAllowedOrigins("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(Constants.WS_MESSAGE_SERVICE_TOPIC);
        registry.setApplicationDestinationPrefixes(Constants.WS_MESSAGE_SERVICE_APP_PREFIX);
        //registry.setUserDestinationPrefix(Constants.WS_MESSAGE_SERVICE_USER_DESTINATION_PREFIX);
    }
}
