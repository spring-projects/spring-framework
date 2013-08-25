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

import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
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

		String beanName = ClassUtils.getShortNameAsProperty(endpointClass);
		if (wac.containsBean(beanName)) {
			T endpoint = wac.getBean(beanName, endpointClass);
			if (logger.isTraceEnabled()) {
				logger.trace("Using @ServerEndpoint singleton " + endpoint);
			}
			return endpoint;
		}

		Component annot = AnnotationUtils.findAnnotation(endpointClass, Component.class);
		if ((annot != null) && wac.containsBean(annot.value())) {
			T endpoint = wac.getBean(annot.value(), endpointClass);
			if (logger.isTraceEnabled()) {
				logger.trace("Using @ServerEndpoint singleton " + endpoint);
			}
			return endpoint;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Creating new @ServerEndpoint instance of type " + endpointClass);
		}
		return wac.getAutowireCapableBeanFactory().createBean(endpointClass);
	}

}
