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

package org.springframework.web.socket.config.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

/**
 * Configuration support for WebSocket request handling.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketConfigurationSupport {

	@Bean
	public HandlerMapping webSocketHandlerMapping() {
		ServletWebSocketHandlerRegistry registry = new ServletWebSocketHandlerRegistry(defaultSockJsTaskScheduler());
		registerWebSocketHandlers(registry);
		AbstractHandlerMapping hm = registry.getHandlerMapping();
		hm.setOrder(1);
		return hm;
	}

	protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
	}

	/**
	 * The default TaskScheduler to use if none is configured via
	 * {@link SockJsServiceRegistration#setTaskScheduler}, i.e.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableWebSocket
	 * public class WebSocketConfig implements WebSocketConfigurer {
	 *
	 *   public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
	 *     registry.addHandler(myWsHandler(), "/echo").withSockJS().setTaskScheduler(myScheduler());
	 *   }
	 *
	 *   // ...
	 *
	 * }
	 * </pre>
	 */
	@Bean
	public ThreadPoolTaskScheduler defaultSockJsTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("SockJS-");
		return scheduler;
	}

}
