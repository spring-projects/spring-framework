/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.stomp.websocketstompdestinationseparator;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// tag::snippet[]
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

	// ...

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.setPathMatcher(new AntPathMatcher("."));
		registry.enableStompBrokerRelay("/queue", "/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}
}
// end::snippet[]
