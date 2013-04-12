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

package org.springframework.websocket.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;
import org.springframework.websocket.HandlerProvider;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class EndpointConnectionManager extends AbstractEndpointConnectionManager implements BeanFactoryAware {

	private static Log logger = LogFactory.getLog(EndpointConnectionManager.class);

	private final ClientEndpointConfig.Builder configBuilder = ClientEndpointConfig.Builder.create();

	private final HandlerProvider<Endpoint> endpointProvider;


	public EndpointConnectionManager(Class<? extends Endpoint> endpointClass, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		Assert.notNull(endpointClass, "endpointClass is required");
		this.endpointProvider = new HandlerProvider<Endpoint>(endpointClass);
		this.endpointProvider.setLogger(logger);
	}

	public EndpointConnectionManager(Endpoint endpointBean, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		Assert.notNull(endpointBean, "endpointBean is required");
		this.endpointProvider = new HandlerProvider<Endpoint>(endpointBean);
		this.endpointProvider.setLogger(logger);
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

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.endpointProvider.setBeanFactory(beanFactory);
	}

	@Override
	protected Session connect() throws DeploymentException, IOException {
		Endpoint typedEndpoint = this.endpointProvider.getHandler();
		ClientEndpointConfig endpointConfig = this.configBuilder.build();
		return getWebSocketContainer().connectToServer(typedEndpoint, endpointConfig, getUri());
	}

}
