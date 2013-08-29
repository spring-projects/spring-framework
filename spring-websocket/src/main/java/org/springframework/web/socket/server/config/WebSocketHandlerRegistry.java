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

package org.springframework.web.socket.server.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;


/**
 * A helper class for configuring {@link WebSocketHandler} request handling.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHandlerRegistry {

	private final List<WebSocketHandlerRegistration> registrations = new ArrayList<WebSocketHandlerRegistration>();

	private int order = 1;

	private TaskScheduler defaultTaskScheduler;


	public WebSocketHandlerRegistry(ThreadPoolTaskScheduler defaultSockJsTaskScheduler) {
		this.defaultTaskScheduler = defaultSockJsTaskScheduler;
	}

	public WebSocketHandlerRegistration addHandler(WebSocketHandler wsHandler, String... paths) {
		WebSocketHandlerRegistration r = new WebSocketHandlerRegistration(this.defaultTaskScheduler);
		r.addHandler(wsHandler, paths);
		this.registrations.add(r);
		return r;
	}

	protected List<WebSocketHandlerRegistration> getRegistrations() {
		return this.registrations;
	}

	/**
	 * Specify the order to use for WebSocket {@link HandlerMapping} relative to other
	 * handler mappings configured in the Spring MVC configuration. The default value is 1.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Returns a handler mapping with the mapped ViewControllers; or {@code null} in case of no registrations.
	 */
	AbstractHandlerMapping getHandlerMapping() {
		Map<String, Object> urlMap = new LinkedHashMap<String, Object>();
		for (WebSocketHandlerRegistration registration : this.registrations) {
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
