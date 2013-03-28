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

import org.apache.tomcat.websocket.server.WsServerContainer;
import org.springframework.web.context.ServletContextAware;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServletServerEndpointExporter extends ServerEndpointExporter implements ServletContextAware {

	private ServletContext servletContext;


	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO: remove hard dependency on Tomcat (see Tomcat's WsListener)
		WsServerContainer sc = WsServerContainer.getServerContainer();
        sc.setServletContext(this.servletContext);

        super.afterPropertiesSet();
	}

}
