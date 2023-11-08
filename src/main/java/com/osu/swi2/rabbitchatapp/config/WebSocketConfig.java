package com.osu.swi2.rabbitchatapp.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;


@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    private final ApplicationContext context;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        logger.info("Registering Stomp endpoints");
        registry.addEndpoint("/chat-app")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        AuthorizationManager<Message<?>> myAuthorizationRules = AuthenticatedAuthorizationManager.authenticated();
        AuthorizationChannelInterceptor authz = new AuthorizationChannelInterceptor(myAuthorizationRules);
        AuthorizationEventPublisher publisher = new SpringAuthorizationEventPublisher(this.context);
        authz.setAuthorizationEventPublisher(publisher);
        // TODO fix websocket security
        //registration.interceptors(new SecurityContextChannelInterceptor(), authz,);
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                return ChannelInterceptor.super.preSend(message, channel);
            }
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        logger.info("Configuring message broker");
        registry.setApplicationDestinationPrefixes("/chat-app");
        registry.enableSimpleBroker("/topic");
    }


}