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

package org.springframework.web.socket.server.endpoint;

import java.util.Map;

import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * A {@link javax.websocket.server.ServerEndpointConfig.Configurator} for initializing
 * {@link ServerEndpoint}-annotated classes through Spring.
 *
 * <p>
 * <pre class="code">
 * &#064;ServerEndpoint(value = "/echo", configurator = SpringConfigurator.class)
 * public class EchoEndpoint {
 *     // ...
 * }
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see ServerEndpointExporter
 */
public class SpringConfigurator extends Configurator {

	private static Log logger = LogFactory.getLog(SpringConfigurator.class);


	@Override
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {

		WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
		if (wac == null) {
			String message = "Failed to find the root WebApplicationContext. Was ContextLoaderListener not used?";
			logger.error(message);
			throw new IllegalStateException(message);
		}

		Map<String, T> beans = wac.getBeansOfType(endpointClass);
		if (beans.isEmpty()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Creating new @ServerEndpoint instance of type " + endpointClass);
			}
			return wac.getAutowireCapableBeanFactory().createBean(endpointClass);
		}
		else if (beans.size() == 1) {
			if (logger.isTraceEnabled()) {
				logger.trace("Using @ServerEndpoint singleton " + beans.keySet().iterator().next());
			}
			return beans.values().iterator().next();
		}
		else {
			// Should not happen ..
			String message = "Found more than one matching @ServerEndpoint beans of type " + endpointClass;
			logger.error(message);
			throw new IllegalStateException(message);
		}
	}

}
