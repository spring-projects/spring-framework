/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.client.standard;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;

import org.springframework.beans.factory.FactoryBean;

/**
 * A FactoryBean for creating and configuring a {@link jakarta.websocket.WebSocketContainer}
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
