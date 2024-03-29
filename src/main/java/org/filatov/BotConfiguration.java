package org.filatov;


import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import org.filatov.listener.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BotConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BotConfiguration.class);

    @Value("${token}")
    private String token;

    @Bean
    public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<EventListener<T>> eventListeners) {

        GatewayDiscordClient client = null;

        try {

            client = DiscordClientBuilder.create(token)
                    .build()
                    .login()
                    .block();

            assert client != null;

            client.getEventDispatcher().on(ReadyEvent.class)
                    .subscribe(event -> {
                        User self = event.getSelf();
                        log.info("Logged in as {}#{}", self.getUsername(), self.getDiscriminator());
                    });

            for (EventListener<T> listener : eventListeners) {
                client.on(listener.getEventType())
                        .flatMap(listener::execute)
                        .onErrorResume(listener::handleError)
                        .subscribe();
            }
            client.onDisconnect().block();
        } catch (Exception exception) {
            log.error("Be sure to use a valid bot token!", exception);
        }

        return client;
    }
}