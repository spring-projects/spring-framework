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

package org.springframework.web.socket.messaging.config;

import java.util.*;

import org.springframework.messaging.simp.handler.UserSessionRegistry;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.support.WebSocketHandlerDecorator;

/**
 * A registry for STOMP over WebSocket endpoints that maps the endpoints with a
 * {@link SimpleUrlHandlerMapping} for use in Spring MVC.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebMvcStompEndpointRegistry implements StompEndpointRegistry {

	private final WebSocketHandler webSocketHandler;

	private final SubProtocolWebSocketHandler subProtocolWebSocketHandler;

	private final StompSubProtocolHandler stompHandler;

	private final List<WebMvcStompWebSocketEndpointRegistration> registrations =
			new ArrayList<WebMvcStompWebSocketEndpointRegistration>();

	private final TaskScheduler sockJsScheduler;

	private int order = 1;


	public WebMvcStompEndpointRegistry(WebSocketHandler webSocketHandler,
			UserSessionRegistry userSessionRegistry, TaskScheduler defaultSockJsTaskScheduler) {
		Assert.notNull(webSocketHandler);
		Assert.notNull(userSessionRegistry);
		this.webSocketHandler = webSocketHandler;
		this.subProtocolWebSocketHandler = unwrapSubProtocolWebSocketHandler(webSocketHandler);
		this.stompHandler = new StompSubProtocolHandler();
		this.stompHandler.setUserSessionRegistry(userSessionRegistry);
		this.sockJsScheduler = defaultSockJsTaskScheduler;
	}

	private static SubProtocolWebSocketHandler unwrapSubProtocolWebSocketHandler(WebSocketHandler webSocketHandler) {
		WebSocketHandler actual = (webSocketHandler instanceof WebSocketHandlerDecorator) ?
				((WebSocketHandlerDecorator) webSocketHandler).getLastHandler() : webSocketHandler;
		Assert.isInstanceOf(SubProtocolWebSocketHandler.class, actual,
						"No SubProtocolWebSocketHandler found: " + webSocketHandler);
		return (SubProtocolWebSocketHandler) actual;
	}


	@Override
	public StompWebSocketEndpointRegistration addEndpoint(String... paths) {
		this.subProtocolWebSocketHandler.addProtocolHandler(this.stompHandler);
		WebMvcStompWebSocketEndpointRegistration registration = new WebMvcStompWebSocketEndpointRegistration(
				paths, this.webSocketHandler, this.sockJsScheduler);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Set the order for the resulting {@link SimpleUrlHandlerMapping} relative to
	 * other handler mappings configured in Spring MVC.
	 * <p>The default value is 1.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}

	/**
	 * Returns a handler mapping with the mapped ViewControllers; or {@code null} in case of no registrations.
	 */
	protected AbstractHandlerMapping getHandlerMapping() {
		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
		for (WebMvcStompWebSocketEndpointRegistration registration : this.registrations) {
			MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
			for (HttpRequestHandler httpHandler : mappings.keySet()) {
				for (String pattern : mappings.get(httpHandler)) {
					urlMap.put(pattern, httpHandler);
				}
			}
		}
		SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
		hm.setUrlMap(urlMap);
		hm.setOrder(this.order);
		return hm;
	}

}
