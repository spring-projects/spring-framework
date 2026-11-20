/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.stomp.websocketstomphandlebrokerrelayconfigure;

import java.net.InetSocketAddress;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// tag::snippet[]
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

	// ...

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableStompBrokerRelay("/queue/", "/topic/").setTcpClient(createTcpClient());
		registry.setApplicationDestinationPrefixes("/app");
	}

	private ReactorNettyTcpClient<byte[]> createTcpClient() {
		return new ReactorNettyTcpClient<>(
				client -> client.remoteAddress(() -> new InetSocketAddress(0)),
				new StompReactorNettyCodec());
	}
}
// end::snippet[]
