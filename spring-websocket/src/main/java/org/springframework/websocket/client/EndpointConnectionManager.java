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


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class EndpointConnectionManager extends AbstractEndpointConnectionManager {

	private final ClientEndpointConfig.Builder configBuilder = ClientEndpointConfig.Builder.create();


	public EndpointConnectionManager(Class<? extends Endpoint> endpointClass, String uriTemplate, Object... uriVariables) {
		super(endpointClass, uriTemplate, uriVariables);
	}

	public EndpointConnectionManager(Endpoint endpointBean, String uriTemplate, Object... uriVariables) {
		super(endpointBean, uriTemplate, uriVariables);
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
	protected Session connect(Object endpoint) throws DeploymentException, IOException {
		Endpoint typedEndpoint = (Endpoint) endpoint;
		ClientEndpointConfig endpointConfig = this.configBuilder.build();
		return getWebSocketContainer().connectToServer(typedEndpoint, endpointConfig, getUri());
	}

}
