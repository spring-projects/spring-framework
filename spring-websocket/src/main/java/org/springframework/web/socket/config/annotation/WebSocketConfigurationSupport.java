/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.config.annotation;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Configuration support for WebSocket request handling.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketConfigurationSupport {

	private ServletWebSocketHandlerRegistry handlerRegistry;

	private TaskScheduler scheduler;


	@Bean
	public HandlerMapping webSocketHandlerMapping() {
		ServletWebSocketHandlerRegistry registry = initHandlerRegistry();
		if (registry.requiresTaskScheduler()) {
			registry.setTaskScheduler(initTaskScheduler());
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
	 * }
	 * </pre>
	 */
	@Bean
	public TaskScheduler defaultSockJsTaskScheduler() {
		return initTaskScheduler();
	}

	private TaskScheduler initTaskScheduler() {
		if (this.scheduler == null) {
			ServletWebSocketHandlerRegistry registry = initHandlerRegistry();
			if (registry.requiresTaskScheduler()) {
				ThreadPoolTaskScheduler threadPoolScheduler = new ThreadPoolTaskScheduler();
				threadPoolScheduler.setThreadNamePrefix("SockJS-");
				threadPoolScheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
				threadPoolScheduler.setRemoveOnCancelPolicy(true);
				this.scheduler = threadPoolScheduler;
			}
			else {
				this.scheduler = new NoOpScheduler();
			}
		}
		return scheduler;
	}


	private static class NoOpScheduler implements TaskScheduler {

		@Override
		public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
			throw new IllegalStateException("Unexpected use of scheduler.");
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
			throw new IllegalStateException("Unexpected use of scheduler.");
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
			throw new IllegalStateException("Unexpected use of scheduler.");
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
			throw new IllegalStateException("Unexpected use of scheduler.");
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
			throw new IllegalStateException("Unexpected use of scheduler.");
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
			throw new IllegalStateException("Unexpected use of scheduler.");
		}
	}
}
