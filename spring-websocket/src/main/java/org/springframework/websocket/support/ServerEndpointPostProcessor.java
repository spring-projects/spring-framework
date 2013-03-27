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
package org.springframework.websocket.support;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerContainerProvider;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;

/**
 * BeanPostProcessor that registers {@link javax.websocket.server.ServerEndpointConfig}
 * beans with a standard Java WebSocket runtime and also configures the underlying
 * {@link javax.websocket.server.ServerContainer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServerEndpointPostProcessor implements BeanPostProcessor, InitializingBean {

	private static Log logger = LogFactory.getLog(ServerEndpointPostProcessor.class);

	private Long maxSessionIdleTimeout;

	private Integer maxTextMessageBufferSize;

	private Integer maxBinaryMessageBufferSize;


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
	public void afterPropertiesSet() throws Exception {
		ServerContainer serverContainer = ServerContainerProvider.getServerContainer();
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
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ServerEndpointConfig) {
			ServerEndpointConfig sec = (ServerEndpointConfig) bean;
			ServerContainer serverContainer = ServerContainerProvider.getServerContainer();
			try {
				logger.debug("Registering javax.websocket.Endpoint for path " + sec.getPath());
				serverContainer.addEndpoint(sec);
			}
			catch (DeploymentException e) {
				throw new IllegalStateException("Failed to deploy Endpoint " + bean, e);
			}
		}
		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
