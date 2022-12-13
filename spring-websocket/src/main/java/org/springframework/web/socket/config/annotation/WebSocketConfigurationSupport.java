/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
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


	@Bean
	public HandlerMapping webSocketHandlerMapping(
		@Qualifier("defaultSockJsSchedulerContainer") DefaultSockJsSchedulerContainer schedulerContainer) {

		ServletWebSocketHandlerRegistry registry = initHandlerRegistry();
		if (registry.requiresTaskScheduler()) {
			TaskScheduler scheduler = schedulerContainer.getScheduler();
			Assert.notNull(scheduler, "TaskScheduler is required but not initialized");
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
	 * A container of the default TaskScheduler to use if none was registered
	 * explicitly via {@link SockJsServiceRegistration#setTaskScheduler} as
	 * follows:
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
	DefaultSockJsSchedulerContainer defaultSockJsSchedulerContainer() {
		return (initHandlerRegistry().requiresTaskScheduler() ?
				new DefaultSockJsSchedulerContainer(initDefaultSockJsScheduler()) :
				new DefaultSockJsSchedulerContainer(null));
	}

	private ThreadPoolTaskScheduler initDefaultSockJsScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("SockJS-");
		scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
		scheduler.setRemoveOnCancelPolicy(true);
		return scheduler;
	}


	static class DefaultSockJsSchedulerContainer implements InitializingBean, DisposableBean {

		@Nullable
		private final ThreadPoolTaskScheduler scheduler;

		DefaultSockJsSchedulerContainer(@Nullable ThreadPoolTaskScheduler scheduler) {
			this.scheduler = scheduler;
		}

		@Nullable
		public ThreadPoolTaskScheduler getScheduler() {
			return this.scheduler;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			if (this.scheduler != null) {
				this.scheduler.afterPropertiesSet();
			}
		}

		@Override
		public void destroy() throws Exception {
			if (this.scheduler != null) {
				this.scheduler.destroy();
			}
		}

	}


}
