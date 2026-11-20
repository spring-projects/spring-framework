/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.websocketfallbacksockjsclient;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;

// tag::snippet[]
@Configuration
public class WebSocketConfiguration extends WebSocketMessageBrokerConfigurationSupport {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/sockjs").withSockJS()
			// Set the streamBytesLimit property to 512KB (the default is 128KB -- 128 * 1024)
			.setStreamBytesLimit(512 * 1024)
			// Set the httpMessageCacheSize property to 1,000 (the default is 100)
			.setHttpMessageCacheSize(1000)
			// Set the disconnectDelay property to 30 property seconds (the default is five seconds -- 5 * 1000)
			.setDisconnectDelay(30 * 1000);
	}

	// ...
}
// end::snippet[]

