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

package org.springframework.websocket.server.endpoint;

import javax.servlet.ServletContext;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerContainerProvider;

import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;


/**
 * A sub-class of {@link EndpointExporter} for use with a Servlet container runtime.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServletEndpointExporter extends EndpointExporter implements ServletContextAware {

	/**
	 *
	 */
	private static final String SERVER_CONTAINER_ATTR_NAME = "javax.websocket.server.ServerContainer";
	private ServletContext servletContext;


	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	protected ServerContainer getServerContainer() {
		Assert.notNull(this.servletContext, "A ServletContext is needed to access the WebSocket ServerContainer");
		ServerContainer container = (ServerContainer) this.servletContext.getAttribute(SERVER_CONTAINER_ATTR_NAME);
		if (container == null) {
			// Remove when Tomcat has caught up to http://java.net/jira/browse/WEBSOCKET_SPEC-165
			return ServerContainerProvider.getServerContainer();
		}
		return container;
	}

}
