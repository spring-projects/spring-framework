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
package org.springframework.websocket.server.support;

import javax.websocket.WebSocketContainer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;


/**
*
* @author Rossen Stoyanchev
* @since 4.0
*/
public abstract class AbstractEndpointContainerFactoryBean implements FactoryBean<WebSocketContainer>, InitializingBean {

	private WebSocketContainer container;


	public void setAsyncSendTimeout(long timeoutInMillis) {
		this.container.setAsyncSendTimeout(timeoutInMillis);
	}

	public long getAsyncSendTimeout() {
		return this.container.getDefaultAsyncSendTimeout();
	}

	public void setMaxSessionIdleTimeout(long timeoutInMillis) {
		this.container.setDefaultMaxSessionIdleTimeout(timeoutInMillis);
	}

	public long getMaxSessionIdleTimeout() {
		return this.container.getDefaultMaxSessionIdleTimeout();
	}

	public void setMaxTextMessageBufferSize(int bufferSize) {
		this.container.setDefaultMaxTextMessageBufferSize(bufferSize);
	}

	public int getMaxTextMessageBufferSize() {
		return this.container.getDefaultMaxTextMessageBufferSize();
	}

	public void setMaxBinaryMessageBufferSize(int bufferSize) {
		this.container.setDefaultMaxBinaryMessageBufferSize(bufferSize);
	}

	public int getMaxBinaryMessageBufferSize() {
		return this.container.getDefaultMaxBinaryMessageBufferSize();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.container = getContainer();
	}

	protected abstract WebSocketContainer getContainer();

	@Override
	public WebSocketContainer getObject() throws Exception {
		return this.container;
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
