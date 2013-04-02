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

import java.util.Map;

import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;


/**
 * This should be used in conjuction with {@link ServerEndpoint @ServerEndpoint} classes.
 *
 * <p>For {@link javax.websocket.Endpoint}, see {@link EndpointExporter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SpringConfigurator extends Configurator {


	@Override
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {

		WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
		if (wac == null) {
			throw new IllegalStateException("Failed to find WebApplicationContext. "
					+ "Was org.springframework.web.context.ContextLoader used to load the WebApplicationContext?");
		}

		Map<String, T> beans = wac.getBeansOfType(endpointClass);
		if (beans.isEmpty()) {
			// Initialize a new bean instance
			return wac.getAutowireCapableBeanFactory().createBean(endpointClass);
		}
		if (beans.size() == 1) {
			// Return the matching bean instance
			return beans.values().iterator().next();
		}
		else {
			// This should never happen (@ServerEndpoint has a single path mapping) ..
			throw new IllegalStateException("Found more than one matching beans of type " + endpointClass);
		}
	}

}
