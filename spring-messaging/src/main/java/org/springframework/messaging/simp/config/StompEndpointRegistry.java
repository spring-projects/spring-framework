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
import java.util.Arrays;
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
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;


/**
 * A helper class for configuring STOMP protocol handling over WebSocket.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompEndpointRegistry {

	private final SubProtocolWebSocketHandler wsHandler;

	private final StompProtocolHandler stompHandler;

	private final List<StompEndpointRegistration> registrations = new ArrayList<StompEndpointRegistration>();

	private int order = 1;

	private final TaskScheduler defaultSockJsTaskScheduler;


	public StompEndpointRegistry(SubProtocolWebSocketHandler webSocketHandler,
			MutableUserQueueSuffixResolver userQueueSuffixResolver, TaskScheduler defaultSockJsTaskScheduler) {

		Assert.notNull(webSocketHandler);
		Assert.notNull(userQueueSuffixResolver);

		this.wsHandler = webSocketHandler;
		this.stompHandler = new StompProtocolHandler();
		this.stompHandler.setUserQueueSuffixResolver(userQueueSuffixResolver);
		this.defaultSockJsTaskScheduler = defaultSockJsTaskScheduler;
	}


	public StompEndpointRegistration addEndpoint(String... paths) {
		this.wsHandler.addProtocolHandler(this.stompHandler);
		StompEndpointRegistration r = new StompEndpointRegistration(
				Arrays.asList(paths), this.wsHandler, this.defaultSockJsTaskScheduler);
		this.registrations.add(r);
		return r;
	}

	/**
	 * Specify the order to use for the STOMP endpoint {@link HandlerMapping} relative to
	 * other handler mappings configured in the Spring MVC configuration. The default
	 * value is 1.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Returns a handler mapping with the mapped ViewControllers; or {@code null} in case of no registrations.
	 */
	protected AbstractHandlerMapping getHandlerMapping() {
		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
		for (StompEndpointRegistration registration : this.registrations) {
			MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
			for (HttpRequestHandler httpHandler : mappings.keySet()) {
				for (String pattern : mappings.get(httpHandler)) {
					urlMap.put(pattern, httpHandler);
				}
			}
		}
		SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
		hm.setOrder(this.order);
		hm.setUrlMap(urlMap);
		return hm;
	}

}
