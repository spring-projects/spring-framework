/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.websocket.SubProtocolWebSocketHandler;
import org.springframework.messaging.simp.handler.MutableUserQueueSuffixResolver;
import org.springframework.messaging.simp.stomp.StompProtocolHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.support.WebSocketHandlerDecorator;


/**
 * A helper class for configuring STOMP protocol handling over WebSocket.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServletStompEndpointRegistry implements StompEndpointRegistry {

	private final WebSocketHandler webSocketHandler;

	private final SubProtocolWebSocketHandler subProtocolWebSocketHandler;

	private final StompProtocolHandler stompHandler;

	private final List<ServletStompEndpointRegistration> registrations = new ArrayList<ServletStompEndpointRegistration>();

	private final TaskScheduler sockJsScheduler;


	public ServletStompEndpointRegistry(WebSocketHandler webSocketHandler,
			MutableUserQueueSuffixResolver userQueueSuffixResolver, TaskScheduler defaultSockJsTaskScheduler) {

		Assert.notNull(webSocketHandler);
		Assert.notNull(userQueueSuffixResolver);

		this.webSocketHandler = webSocketHandler;
		this.subProtocolWebSocketHandler = findSubProtocolWebSocketHandler(webSocketHandler);
		this.stompHandler = new StompProtocolHandler();
		this.stompHandler.setUserQueueSuffixResolver(userQueueSuffixResolver);
		this.sockJsScheduler = defaultSockJsTaskScheduler;
	}

	private static SubProtocolWebSocketHandler findSubProtocolWebSocketHandler(WebSocketHandler webSocketHandler) {

		WebSocketHandler actual = (webSocketHandler instanceof WebSocketHandlerDecorator) ?
				((WebSocketHandlerDecorator) webSocketHandler).getLastHandler() : webSocketHandler;

		Assert.isInstanceOf(SubProtocolWebSocketHandler.class, actual,
						"No SubProtocolWebSocketHandler found: " + webSocketHandler);

		return (SubProtocolWebSocketHandler) actual;
	}


	@Override
	public StompEndpointRegistration addEndpoint(String... paths) {
		this.subProtocolWebSocketHandler.addProtocolHandler(this.stompHandler);
		ServletStompEndpointRegistration r = new ServletStompEndpointRegistration(
				paths, this.webSocketHandler, this.sockJsScheduler);
		this.registrations.add(r);
		return r;
	}

	/**
	 * Returns a handler mapping with the mapped ViewControllers; or {@code null} in case of no registrations.
	 */
	protected AbstractHandlerMapping getHandlerMapping() {
		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
		for (ServletStompEndpointRegistration registration : this.registrations) {
			MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
			for (HttpRequestHandler httpHandler : mappings.keySet()) {
				for (String pattern : mappings.get(httpHandler)) {
					urlMap.put(pattern, httpHandler);
				}
			}
		}
		SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
		hm.setUrlMap(urlMap);
		return hm;
	}

}
