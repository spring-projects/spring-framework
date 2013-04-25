/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.websocket.server.endpoint;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * BeanPostProcessor that detects beans of type
 * {@link javax.websocket.server.ServerEndpointConfig} and registers the provided
 * {@link javax.websocket.Endpoint} with a standard Java WebSocket runtime.
 *
 * <p>If the runtime is a Servlet container, use {@link ServletEndpointExporter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class EndpointExporter implements InitializingBean, BeanPostProcessor, ApplicationContextAware {

	private static final boolean isServletApiPresent =
			ClassUtils.isPresent("javax.servlet.ServletContext", EndpointExporter.class.getClassLoader());

	private static Log logger = LogFactory.getLog(EndpointExporter.class);

	private final List<Class<?>> annotatedEndpointClasses = new ArrayList<Class<?>>();

	private final List<Class<?>> annotatedEndpointBeanTypes = new ArrayList<Class<?>>();

	private ApplicationContext applicationContext;

	private ServerContainer serverContainer;

	/**
	 * TODO
	 * @param annotatedEndpointClasses
	 */
	public void setAnnotatedEndpointClasses(Class<?>... annotatedEndpointClasses) {
		this.annotatedEndpointClasses.clear();
		this.annotatedEndpointClasses.addAll(Arrays.asList(annotatedEndpointClasses));
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {

		this.applicationContext = applicationContext;

		this.serverContainer = getServerContainer();

		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ServerEndpoint.class);
		for (String beanName : beans.keySet()) {
			Class<?> beanType = applicationContext.getType(beanName);
			if (logger.isInfoEnabled()) {
				logger.info("Detected @ServerEndpoint bean '" + beanName + "', registering it as an endpoint by type");
			}
			this.annotatedEndpointBeanTypes.add(beanType);
		}
	}

	protected ServerContainer getServerContainer() {
		if (isServletApiPresent) {
			try {
				Method getter = ReflectionUtils.findMethod(this.applicationContext.getClass(), "getServletContext");
				Object servletContext = getter.invoke(this.applicationContext);

				Method attrMethod = ReflectionUtils.findMethod(servletContext.getClass(), "getAttribute", String.class);
				return (ServerContainer) attrMethod.invoke(servletContext, "javax.websocket.server.ServerContainer");
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"Failed to get javax.websocket.server.ServerContainer via ServletContext attribute", ex);
			}
		}
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(serverContainer, "javax.websocket.server.ServerContainer not available");

		List<Class<?>> allClasses = new ArrayList<Class<?>>(this.annotatedEndpointClasses);
		allClasses.addAll(this.annotatedEndpointBeanTypes);

		for (Class<?> clazz : allClasses) {
			try {
				logger.info("Registering @ServerEndpoint type " + clazz);
				this.serverContainer.addEndpoint(clazz);
			}
			catch (DeploymentException e) {
				throw new IllegalStateException("Failed to register @ServerEndpoint type " + clazz, e);
			}
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ServerEndpointConfig) {
			ServerEndpointConfig sec = (ServerEndpointConfig) bean;
			try {
				if (logger.isInfoEnabled()) {
					logger.info("Registering bean '" + beanName
							+ "' as javax.websocket.Endpoint under path " + sec.getPath());
				}
				getServerContainer().addEndpoint(sec);
			}
			catch (DeploymentException e) {
				throw new IllegalStateException("Failed to deploy Endpoint bean " + bean, e);
			}
		}
		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
