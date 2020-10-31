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

package org.springframework.web.socket.server.support;

import javax.servlet.ServletContext;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * An extension of {@link SimpleUrlHandlerMapping} that is also a
 * {@link SmartLifecycle} container and propagates start and stop calls to any
 * handlers that implement {@link Lifecycle}. The handlers are typically expected
 * to be {@code WebSocketHttpRequestHandler} or {@code SockJsHttpRequestHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class WebSocketHandlerMapping extends SimpleUrlHandlerMapping implements SmartLifecycle {

	private volatile boolean running;


	@Override
	protected void initServletContext(ServletContext servletContext) {
		for (Object handler : getUrlMap().values()) {
			if (handler instanceof ServletContextAware) {
				((ServletContextAware) handler).setServletContext(servletContext);
			}
		}
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			for (Object handler : getUrlMap().values()) {
				if (handler instanceof Lifecycle) {
					((Lifecycle) handler).start();
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			for (Object handler : getUrlMap().values()) {
				if (handler instanceof Lifecycle) {
					((Lifecycle) handler).stop();
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
