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
package org.springframework.websocket.client.endpoint;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.springframework.beans.factory.FactoryBean;


/**
 * A FactoryBean for creating and configuring a {@link javax.websocket.WebSocketContainer}
 * through Spring XML configuration. In Java configuration, ignore this class and use
 * {@code ContainerProvider.getWebSocketContainer()} instead.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketContainerFactoryBean implements FactoryBean<WebSocketContainer> {

	private final WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();


	public void setAsyncSendTimeout(long timeoutInMillis) {
		this.webSocketContainer.setAsyncSendTimeout(timeoutInMillis);
	}

	public long getAsyncSendTimeout() {
		return this.webSocketContainer.getDefaultAsyncSendTimeout();
	}

	public void setMaxSessionIdleTimeout(long timeoutInMillis) {
		this.webSocketContainer.setDefaultMaxSessionIdleTimeout(timeoutInMillis);
	}

	public long getMaxSessionIdleTimeout() {
		return this.webSocketContainer.getDefaultMaxSessionIdleTimeout();
	}

	public void setMaxTextMessageBufferSize(int bufferSize) {
		this.webSocketContainer.setDefaultMaxTextMessageBufferSize(bufferSize);
	}

	public int getMaxTextMessageBufferSize() {
		return this.webSocketContainer.getDefaultMaxTextMessageBufferSize();
	}

	public void setMaxBinaryMessageBufferSize(int bufferSize) {
		this.webSocketContainer.setDefaultMaxBinaryMessageBufferSize(bufferSize);
	}

	public int getMaxBinaryMessageBufferSize() {
		return this.webSocketContainer.getDefaultMaxBinaryMessageBufferSize();
	}

	@Override
	public WebSocketContainer getObject() throws Exception {
		return this.webSocketContainer;
	}

	@Override
	public Class<?> getObjectType() {
		return WebSocketContainer.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
