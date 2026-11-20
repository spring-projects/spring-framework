/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.stomp.websocketstompserverconfig;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

// tag::snippet[]
@Configuration
@EnableWebSocketMessageBroker
public class JettyWebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/portfolio").setHandshakeHandler(handshakeHandler());
	}

	@Bean
	public DefaultHandshakeHandler handshakeHandler() {
		JettyRequestUpgradeStrategy strategy = new JettyRequestUpgradeStrategy();
		strategy.addWebSocketConfigurer(configurable -> {
			configurable.setInputBufferSize(4 * 8192);
			configurable.setIdleTimeout(Duration.ofSeconds(600));
		});
		return new DefaultHandshakeHandler(strategy);
	}
}
// end::snippet[]
