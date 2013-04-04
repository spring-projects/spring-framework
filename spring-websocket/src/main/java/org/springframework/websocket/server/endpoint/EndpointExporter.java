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

import java.util.Map;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * BeanPostProcessor that detects beans of type
 * {@link javax.websocket.server.ServerEndpointConfig} and registers the corresponding
 * {@link javax.websocket.Endpoint} with a standard Java WebSocket runtime.
 *
 * <p>If the runtime is a Servlet container, use {@link ServletEndpointExporter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class EndpointExporter implements InitializingBean, BeanPostProcessor, BeanFactoryAware {

	private static Log logger = LogFactory.getLog(EndpointExporter.class);

	private Class<?>[] annotatedEndpointClasses;

	private Long maxSessionIdleTimeout;

	private Integer maxTextMessageBufferSize;

	private Integer maxBinaryMessageBufferSize;


	/**
	 * TODO
	 * @param annotatedEndpointClasses
	 */
	public void setAnnotatedEndpointClasses(Class<?>... annotatedEndpointClasses) {
		this.annotatedEndpointClasses = annotatedEndpointClasses;
	}

	/**
	 * If this property set it is in turn used to configure
	 * {@link ServerContainer#setDefaultMaxSessionIdleTimeout(long)}.
	 */
	public void setMaxSessionIdleTimeout(long maxSessionIdleTimeout) {
		this.maxSessionIdleTimeout = maxSessionIdleTimeout;
	}

	public Long getMaxSessionIdleTimeout() {
		return this.maxSessionIdleTimeout;
	}

	/**
	 * If this property set it is in turn used to configure
	 * {@link ServerContainer#setDefaultMaxTextMessageBufferSize(int)}
	 */
	public void setMaxTextMessageBufferSize(int maxTextMessageBufferSize) {
		this.maxTextMessageBufferSize = maxTextMessageBufferSize;
	}

	public Integer getMaxTextMessageBufferSize() {
		return this.maxTextMessageBufferSize;
	}

	/**
	 * If this property set it is in turn used to configure
	 * {@link ServerContainer#setDefaultMaxBinaryMessageBufferSize(int)}.
	 */
	public void setMaxBinaryMessageBufferSize(int maxBinaryMessageBufferSize) {
		this.maxBinaryMessageBufferSize = maxBinaryMessageBufferSize;
	}

	public Integer getMaxBinaryMessageBufferSize() {
		return this.maxBinaryMessageBufferSize;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ListableBeanFactory) {
			ListableBeanFactory lbf = (ListableBeanFactory) beanFactory;
			Map<String, Object> annotatedEndpoints = lbf.getBeansWithAnnotation(ServerEndpoint.class);
			for (String beanName : annotatedEndpoints.keySet()) {
				Class<?> beanType = lbf.getType(beanName);
				try {
					if (logger.isInfoEnabled()) {
						logger.info("Detected @ServerEndpoint bean '" + beanName + "', registering it as an endpoint by type");
					}
					getServerContainer().addEndpoint(beanType);
				}
				catch (DeploymentException e) {
					throw new IllegalStateException("Failed to register @ServerEndpoint bean type " + beanName, e);
				}
			}
		}
	}

	/**
	 * Return the {@link ServerContainer} instance, a process which is undefined outside
	 * of standalone containers (section 6.4 of the spec).
	 */
	protected abstract ServerContainer getServerContainer();

	@Override
	public void afterPropertiesSet() throws Exception {

		ServerContainer serverContainer = getServerContainer();
		Assert.notNull(serverContainer, "javax.websocket.server.ServerContainer not available");

		if (this.maxSessionIdleTimeout != null) {
			serverContainer.setDefaultMaxSessionIdleTimeout(this.maxSessionIdleTimeout);
		}
		if (this.maxTextMessageBufferSize != null) {
			serverContainer.setDefaultMaxTextMessageBufferSize(this.maxTextMessageBufferSize);
		}
		if (this.maxBinaryMessageBufferSize != null) {
			serverContainer.setDefaultMaxBinaryMessageBufferSize(this.maxBinaryMessageBufferSize);
		}

		if (!ObjectUtils.isEmpty(this.annotatedEndpointClasses)) {
			for (Class<?> clazz : this.annotatedEndpointClasses) {
				try {
					logger.info("Registering @ServerEndpoint type " + clazz);
					serverContainer.addEndpoint(clazz);
				}
				catch (DeploymentException e) {
					throw new IllegalStateException("Failed to register @ServerEndpoint type " + clazz, e);
				}
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
