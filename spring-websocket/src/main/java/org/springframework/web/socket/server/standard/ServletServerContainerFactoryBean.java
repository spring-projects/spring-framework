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

import javax.servlet.ServletContext;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerContainer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

/**
 * A {@link FactoryBean} for configuring {@link javax.websocket.server.ServerContainer}.
 * Since there is usually only one {@code ServerContainer} instance accessible under a
 * well-known {@code javax.servlet.ServletContext} attribute, simply declaring this
 * FactoryBean and using its setters allows for configuring the {@code ServerContainer}
 * through Spring configuration.
 *
 * <p>This is useful even if the {@code ServerContainer} is not injected into any other
 * bean within the Spring application context. For example, an application can configure
 * a {@link org.springframework.web.socket.server.support.DefaultHandshakeHandler},
 * a {@link org.springframework.web.socket.sockjs.SockJsService}, or
 * {@link ServerEndpointExporter}, and separately declare this FactoryBean in order
 * to customize the properties of the (one and only) {@code ServerContainer} instance.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class ServletServerContainerFactoryBean
		implements FactoryBean<WebSocketContainer>, ServletContextAware, InitializingBean {

	private Long asyncSendTimeout;

	private Long maxSessionIdleTimeout;

	private Integer maxTextMessageBufferSize;

	private Integer maxBinaryMessageBufferSize;

	private ServletContext servletContext;

	private ServerContainer serverContainer;


	public void setAsyncSendTimeout(long timeoutInMillis) {
		this.asyncSendTimeout = timeoutInMillis;
	}

	public long getAsyncSendTimeout() {
		return this.asyncSendTimeout;
	}

	public void setMaxSessionIdleTimeout(long timeoutInMillis) {
		this.maxSessionIdleTimeout = timeoutInMillis;
	}

	public Long getMaxSessionIdleTimeout() {
		return this.maxSessionIdleTimeout;
	}

	public void setMaxTextMessageBufferSize(int bufferSize) {
		this.maxTextMessageBufferSize = bufferSize;
	}

	public Integer getMaxTextMessageBufferSize() {
		return this.maxTextMessageBufferSize;
	}

	public void setMaxBinaryMessageBufferSize(int bufferSize) {
		this.maxBinaryMessageBufferSize = bufferSize;
	}

	public Integer getMaxBinaryMessageBufferSize() {
		return this.maxBinaryMessageBufferSize;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.state(this.servletContext != null,
				"A ServletContext is required to access the javax.websocket.server.ServerContainer instance");
		this.serverContainer = (ServerContainer) this.servletContext.getAttribute(
				"javax.websocket.server.ServerContainer");
		Assert.state(this.serverContainer != null,
				"Attribute 'javax.websocket.server.ServerContainer' not found in ServletContext");

		if (this.asyncSendTimeout != null) {
			this.serverContainer.setAsyncSendTimeout(this.asyncSendTimeout);
		}
		if (this.maxSessionIdleTimeout != null) {
			this.serverContainer.setDefaultMaxSessionIdleTimeout(this.maxSessionIdleTimeout);
		}
		if (this.maxTextMessageBufferSize != null) {
			this.serverContainer.setDefaultMaxTextMessageBufferSize(this.maxTextMessageBufferSize);
		}
		if (this.maxBinaryMessageBufferSize != null) {
			this.serverContainer.setDefaultMaxBinaryMessageBufferSize(this.maxBinaryMessageBufferSize);
		}
	}


	@Override
	public ServerContainer getObject() {
		return this.serverContainer;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.serverContainer != null ? this.serverContainer.getClass() : ServerContainer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
