/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

	private static final boolean JETTY_WS_PRESENT = ClassUtils.isPresent(
			"org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServerContainer",
			DefaultHandshakeHandler.class.getClassLoader());


	public DefaultHandshakeHandler() {
		super(JETTY_WS_PRESENT ? new JettyRequestUpgradeStrategy() : new StandardWebSocketUpgradeStrategy());
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
