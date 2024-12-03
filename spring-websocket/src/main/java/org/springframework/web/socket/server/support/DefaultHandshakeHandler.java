/*
 * Copyright 2002-2024 the original author or authors.
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

import jakarta.servlet.ServletContext;

import org.springframework.util.ClassUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.StandardWebSocketUpgradeStrategy;

/**
 * A default {@link org.springframework.web.socket.server.HandshakeHandler} implementation,
 * extending {@link AbstractHandshakeHandler} with Servlet-specific initialization support.
 * As of 7.0, this class prefers {@link JettyRequestUpgradeStrategy} when Jetty WebSocket
 * is available on the classpath, using {@link StandardWebSocketUpgradeStrategy} otherwise.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultHandshakeHandler extends AbstractHandshakeHandler implements ServletContextAware {

	private static final boolean jettyWsPresent = ClassUtils.isPresent(
			"org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer",
			DefaultHandshakeHandler.class.getClassLoader());


	public DefaultHandshakeHandler() {
		super(jettyWsPresent ? new JettyRequestUpgradeStrategy() : new StandardWebSocketUpgradeStrategy());
	}

	public DefaultHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
		super(requestUpgradeStrategy);
	}


	@Override
	public void setServletContext(ServletContext servletContext) {
		RequestUpgradeStrategy strategy = getRequestUpgradeStrategy();
		if (strategy instanceof ServletContextAware servletContextAware) {
			servletContextAware.setServletContext(servletContext);
		}
	}

}
