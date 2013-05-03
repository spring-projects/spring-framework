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

package org.springframework.websocket.client.endpoint;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.websocket.client.ConnectionManagerSupport;
import org.springframework.websocket.support.BeanCreatingHandlerProvider;

/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AnnotatedEndpointConnectionManager extends ConnectionManagerSupport implements BeanFactoryAware {

	private final Object endpoint;

	private final BeanCreatingHandlerProvider<Object> endpointProvider;

	private WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

	private Session session;


	public AnnotatedEndpointConnectionManager(Object endpoint, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		this.endpointProvider = null;
		this.endpoint = endpoint;
	}

	public AnnotatedEndpointConnectionManager(Class<?> endpointClass, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		this.endpointProvider = new BeanCreatingHandlerProvider<Object>(endpointClass);
		this.endpoint = null;
	}


	public void setWebSocketContainer(WebSocketContainer webSocketContainer) {
		this.webSocketContainer = webSocketContainer;
	}

	public WebSocketContainer getWebSocketContainer() {
		return this.webSocketContainer;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (this.endpointProvider != null) {
			this.endpointProvider.setBeanFactory(beanFactory);
		}
	}

	@Override
	protected void openConnection() throws Exception {
		Object endpoint = (this.endpoint != null) ? this.endpoint : this.endpointProvider.getHandler();
		this.session = this.webSocketContainer.connectToServer(endpoint, getUri());
	}

	@Override
	protected void closeConnection() throws Exception {
		try {
			if (isConnected()) {
				this.session.close();
			}
		}
		finally {
			this.session = null;
		}
	}

	@Override
	protected boolean isConnected() {
		return ((this.session != null) && this.session.isOpen());
	}

}
