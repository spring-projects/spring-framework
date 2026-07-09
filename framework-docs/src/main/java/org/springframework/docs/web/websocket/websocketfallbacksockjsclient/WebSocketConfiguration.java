/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

