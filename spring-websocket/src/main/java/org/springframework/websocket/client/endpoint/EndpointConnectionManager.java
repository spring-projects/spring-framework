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

import java.util.Arrays;
import java.util.List;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;
import org.springframework.websocket.client.ConnectionManagerSupport;
import org.springframework.websocket.support.BeanCreatingHandlerProvider;

/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class EndpointConnectionManager extends ConnectionManagerSupport implements BeanFactoryAware {

	private final Endpoint endpoint;

	private final BeanCreatingHandlerProvider<Endpoint> endpointProvider;

	private final ClientEndpointConfig.Builder configBuilder = ClientEndpointConfig.Builder.create();

	private WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

	private Session session;


	public EndpointConnectionManager(Endpoint endpoint, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		Assert.notNull(endpoint, "endpoint is required");
		this.endpointProvider = null;
		this.endpoint = endpoint;
	}

	public EndpointConnectionManager(Class<? extends Endpoint> endpointClass, String uriTemplate, Object... uriVars) {
		super(uriTemplate, uriVars);
		Assert.notNull(endpointClass, "endpointClass is required");
		this.endpointProvider = new BeanCreatingHandlerProvider<Endpoint>(endpointClass);
		this.endpoint = null;
	}


	public void setSubProtocols(String... subprotocols) {
		this.configBuilder.preferredSubprotocols(Arrays.asList(subprotocols));
	}

	public void setExtensions(Extension... extensions) {
		this.configBuilder.extensions(Arrays.asList(extensions));
	}

	public void setEncoders(List<Class<? extends Encoder>> encoders) {
		this.configBuilder.encoders(encoders);
	}

	public void setDecoders(List<Class<? extends Decoder>> decoders) {
		this.configBuilder.decoders(decoders);
	}

	public void setConfigurator(Configurator configurator) {
		this.configBuilder.configurator(configurator);
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
		Endpoint endpoint = (this.endpoint != null) ? this.endpoint : this.endpointProvider.getHandler();
		ClientEndpointConfig endpointConfig = this.configBuilder.build();
		this.session = getWebSocketContainer().connectToServer(endpoint, endpointConfig, getUri());
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
