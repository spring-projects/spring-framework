/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.server.standard;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * Detects beans of type {@link jakarta.websocket.server.ServerEndpointConfig} and registers
 * with the standard Jakarta WebSocket runtime. Also detects beans annotated with
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

	private @Nullable List<Class<?>> annotatedEndpointClasses;

	private @Nullable ServerContainer serverContainer;


	/**
	 * Explicitly list annotated endpoint types that should be registered on startup. This
	 * can be done if you wish to turn off a Servlet container's scan for endpoints, which
	 * goes through all 3rd party jars in the classpath, and rely on Spring configuration instead.
	 * @param annotatedEndpointClasses {@link ServerEndpoint}-annotated types
	 */
	public void setAnnotatedEndpointClasses(Class<?>... annotatedEndpointClasses) {
		this.annotatedEndpointClasses = Arrays.asList(annotatedEndpointClasses);
	}

	/**
	 * Set the JSR-356 {@link ServerContainer} to use for endpoint registration.
	 * If not set, the container is going to be retrieved via the {@code ServletContext}.
	 */
	public void setServerContainer(@Nullable ServerContainer serverContainer) {
		this.serverContainer = serverContainer;
	}

	/**
	 * Return the JSR-356 {@link ServerContainer} to use for endpoint registration.
	 */
	protected @Nullable ServerContainer getServerContainer() {
		return this.serverContainer;
	}

	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (this.serverContainer == null) {
			this.serverContainer =
					(ServerContainer) servletContext.getAttribute("jakarta.websocket.server.ServerContainer");
		}
	}

	@Override
	protected boolean isContextRequired() {
		return false;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(getServerContainer() != null, "jakarta.websocket.server.ServerContainer not available");
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
		ServerContainer serverContainer = getServerContainer();
		Assert.state(serverContainer != null,
				"No ServerContainer set. Most likely the server's own WebSocket ServletContainerInitializer " +
				"has not run yet. Was the Spring ApplicationContext refreshed through a " +
				"org.springframework.web.context.ContextLoaderListener, " +
				"i.e. after the ServletContext has been fully initialized?");
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering @ServerEndpoint class: " + endpointClass);
			}
			serverContainer.addEndpoint(endpointClass);
		}
		catch (DeploymentException ex) {
			throw new IllegalStateException("Failed to register @ServerEndpoint class: " + endpointClass, ex);
		}
	}

	private void registerEndpoint(ServerEndpointConfig endpointConfig) {
		ServerContainer serverContainer = getServerContainer();
		Assert.state(serverContainer != null, "No ServerContainer set");
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering ServerEndpointConfig: " + endpointConfig);
			}
			serverContainer.addEndpoint(endpointConfig);
		}
		catch (DeploymentException ex) {
			throw new IllegalStateException("Failed to register ServerEndpointConfig: " + endpointConfig, ex);
		}
	}

}
