/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.config.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Configuration support for WebSocket request handling.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class WebSocketConfigurationSupport {

	@Nullable
	private ServletWebSocketHandlerRegistry handlerRegistry;

	@Nullable
	private TaskScheduler scheduler;


	@Bean
	public HandlerMapping webSocketHandlerMapping(@Nullable TaskScheduler defaultSockJsTaskScheduler) {
		ServletWebSocketHandlerRegistry registry = initHandlerRegistry();
		if (registry.requiresTaskScheduler()) {
			TaskScheduler scheduler = defaultSockJsTaskScheduler;
			Assert.notNull(scheduler, "Expected default TaskScheduler bean");
			registry.setTaskScheduler(scheduler);
		}
		return registry.getHandlerMapping();
	}

	private ServletWebSocketHandlerRegistry initHandlerRegistry() {
		if (this.handlerRegistry == null) {
			this.handlerRegistry = new ServletWebSocketHandlerRegistry();
			registerWebSocketHandlers(this.handlerRegistry);
		}
		return this.handlerRegistry;
	}

	protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
	}

	/**
	 * The default TaskScheduler to use if none is registered explicitly via
	 * {@link SockJsServiceRegistration#setTaskScheduler}:
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableWebSocket
	 * public class WebSocketConfig implements WebSocketConfigurer {
	 *
	 *   public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
	 *     registry.addHandler(myHandler(), "/echo")
	 *             .withSockJS()
	 *             .setTaskScheduler(myScheduler());
	 *   }
	 *
	 *   // ...
	 * }
	 * </pre>
	 */
	@Bean
	@Nullable
	public TaskScheduler defaultSockJsTaskScheduler() {
		if (initHandlerRegistry().requiresTaskScheduler()) {
			ThreadPoolTaskScheduler threadPoolScheduler = new ThreadPoolTaskScheduler();
			threadPoolScheduler.setThreadNamePrefix("SockJS-");
			threadPoolScheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
			threadPoolScheduler.setRemoveOnCancelPolicy(true);
			this.scheduler = threadPoolScheduler;
		}
		return this.scheduler;
	}

}
