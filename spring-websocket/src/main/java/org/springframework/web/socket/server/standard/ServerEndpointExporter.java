/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * Detects beans of type {@link javax.websocket.server.ServerEndpointConfig} and registers
 * with the standard Java WebSocket runtime. Also detects beans annotated with
 * {@link ServerEndpoint} and registers them as well. Although not required, it is likely
 * annotated endpoints should have their {@code configurator} property set to
 * {@link SpringConfigurator}.
 *
 * <p>When this class is used, by declaring it in Spring configuration, it should be
 * possible to turn off a Servlet container's scan for WebSocket endpoints. This can be
 * done with the help of the {@code <absolute-ordering>} element in {@code web.xml}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 * @see ServerEndpointRegistration
 * @see SpringConfigurator
 * @see ServletServerContainerFactoryBean
 */
public class ServerEndpointExporter extends WebApplicationObjectSupport
		implements InitializingBean, SmartInitializingSingleton {

	private List<Class<?>> annotatedEndpointClasses;

	private ServerContainer serverContainer;


	/**
	 * Explicitly list annotated endpoint types that should be registered on startup. This
	 * can be done if you wish to turn off a Servlet container's scan for endpoints, which
	 * goes through all 3rd party jars in the, and rely on Spring configuration instead.
	 * @param annotatedEndpointClasses {@link ServerEndpoint}-annotated types
	 */
	public void setAnnotatedEndpointClasses(Class<?>... annotatedEndpointClasses) {
		this.annotatedEndpointClasses = Arrays.asList(annotatedEndpointClasses);
	}

	/**
	 * Set the JSR-356 {@link ServerContainer} to use for endpoint registration.
	 * If not set, the container is going to be retrieved via the {@code ServletContext}.
	 */
	public void setServerContainer(ServerContainer serverContainer) {
		this.serverContainer = serverContainer;
	}

	/**
	 * Return the JSR-356 {@link ServerContainer} to use for endpoint registration.
	 */
	protected ServerContainer getServerContainer() {
		return this.serverContainer;
	}

	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (this.serverContainer == null) {
			this.serverContainer =
					(ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");
		}
	}

	@Override
	protected boolean isContextRequired() {
		return false;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(getServerContainer() != null, "javax.websocket.server.ServerContainer not available");
	}

	@Override
	public void afterSingletonsInstantiated() {
		registerEndpoints();
	}


	/**
	 * Actually register the endpoints. Called by {@link #afterSingletonsInstantiated()}.
	 */
	protected void registerEndpoints() {
		Set<Class<?>> endpointClasses = new LinkedHashSet<>();
		if (this.annotatedEndpointClasses != null) {
			endpointClasses.addAll(this.annotatedEndpointClasses);
		}

		ApplicationContext context = getApplicationContext();
		if (context != null) {
			String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
			for (String beanName : endpointBeanNames) {
				endpointClasses.add(context.getType(beanName));
			}
		}

		for (Class<?> endpointClass : endpointClasses) {
			registerEndpoint(endpointClass);
		}

		if (context != null) {
			Map<String, ServerEndpointConfig> endpointConfigMap = context.getBeansOfType(ServerEndpointConfig.class);
			for (ServerEndpointConfig endpointConfig : endpointConfigMap.values()) {
				registerEndpoint(endpointConfig);
			}
		}
	}

	private void registerEndpoint(Class<?> endpointClass) {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Registering @ServerEndpoint class: " + endpointClass);
			}
			getServerContainer().addEndpoint(endpointClass);
		}
		catch (DeploymentException ex) {
			throw new IllegalStateException("Failed to register @ServerEndpoint class: " + endpointClass, ex);
		}
	}

	private void registerEndpoint(ServerEndpointConfig endpointConfig) {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Registering ServerEndpointConfig: " + endpointConfig);
			}
			getServerContainer().addEndpoint(endpointConfig);
		}
		catch (DeploymentException ex) {
			throw new IllegalStateException("Failed to register ServerEndpointConfig: " + endpointConfig, ex);
		}
	}

}
