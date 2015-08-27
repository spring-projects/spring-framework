/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.server.support;

import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * A default {@link org.springframework.web.socket.server.HandshakeHandler} implementation.
 * Performs initial validation of the WebSocket handshake request -- possibly rejecting it
 * through the appropriate HTTP status code -- while also allowing sub-classes to override
 * various parts of the negotiation process (e.g. origin validation, sub-protocol negotiation,
 * extensions negotiation, etc).
 *
 * <p>If the negotiation succeeds, the actual upgrade is delegated to a server-specific
 * {@link org.springframework.web.socket.server.RequestUpgradeStrategy}, which will update
 * the response as necessary and initialize the WebSocket. Currently supported servers are
 * Tomcat 7 and 8, Jetty 9, and GlassFish 4.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultHandshakeHandler extends AbstractHandshakeHandler implements ServletContextAware {

	public DefaultHandshakeHandler() {
	}

	public DefaultHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
		super(requestUpgradeStrategy);
	}


	@Override
	public void setServletContext(ServletContext servletContext) {
		RequestUpgradeStrategy strategy = getRequestUpgradeStrategy();
		if (strategy instanceof ServletContextAware) {
			((ServletContextAware) strategy).setServletContext(servletContext);
		}
	}

}
