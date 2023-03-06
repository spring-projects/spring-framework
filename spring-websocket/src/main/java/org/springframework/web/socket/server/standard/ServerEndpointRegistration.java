/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.server.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.socket.handler.BeanCreatingHandlerProvider;

/**
 * An implementation of {@link jakarta.websocket.server.ServerEndpointConfig} for use in
 * Spring-based applications. A {@link ServerEndpointRegistration} bean is detected by
 * {@link ServerEndpointExporter} and registered with a Jakarta WebSocket runtime at startup.
 *
 * <p>Class constructors accept a singleton {@link jakarta.websocket.Endpoint} instance
 * or an Endpoint specified by type {@link Class}. When specified by type, the endpoint
 * will be instantiated and initialized through the Spring ApplicationContext before
 * each client WebSocket connection.
 *
 * <p>This class also extends
 * {@link jakarta.websocket.server.ServerEndpointConfig.Configurator} to make it easier
 * to override methods for customizing the handshake process.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 * @see ServerEndpointExporter
 */
public class ServerEndpointRegistration extends ServerEndpointConfig.Configurator
		implements ServerEndpointConfig, BeanFactoryAware {

	private final String path;

	@Nullable
	private final Endpoint endpoint;

	@Nullable
	private final BeanCreatingHandlerProvider<Endpoint> endpointProvider;

	private List<String> subprotocols = new ArrayList<>(0);

	private List<Extension> extensions = new ArrayList<>(0);

	private List<Class<? extends Encoder>> encoders = new ArrayList<>(0);

	private List<Class<? extends Decoder>> decoders = new ArrayList<>(0);

	private final Map<String, Object> userProperties = new HashMap<>(4);


	/**
	 * Create a new {@link ServerEndpointRegistration} instance from an
	 * {@code jakarta.websocket.Endpoint} instance.
	 * @param path the endpoint path
	 * @param endpoint the endpoint instance
	 */
	public ServerEndpointRegistration(String path, Endpoint endpoint) {
		Assert.hasText(path, "Path must not be empty");
		Assert.notNull(endpoint, "Endpoint must not be null");
		this.path = path;
		this.endpoint = endpoint;
		this.endpointProvider = null;
	}

	/**
	 * Create a new {@link ServerEndpointRegistration} instance from an
	 * {@code jakarta.websocket.Endpoint} class.
	 * @param path the endpoint path
	 * @param endpointClass the endpoint class
	 */
	public ServerEndpointRegistration(String path, Class<? extends Endpoint> endpointClass) {
		Assert.hasText(path, "Path must not be empty");
		Assert.notNull(endpointClass, "Endpoint Class must not be null");
		this.path = path;
		this.endpoint = null;
		this.endpointProvider = new BeanCreatingHandlerProvider<>(endpointClass);
	}


	// ServerEndpointConfig implementation

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public Class<? extends Endpoint> getEndpointClass() {
		if (this.endpoint != null) {
			return this.endpoint.getClass();
		}
		else {
			Assert.state(this.endpointProvider != null, "No endpoint set");
			return this.endpointProvider.getHandlerType();
		}
	}

	public Endpoint getEndpoint() {
		if (this.endpoint != null) {
			return this.endpoint;
		}
		else {
			Assert.state(this.endpointProvider != null, "No endpoint set");
			return this.endpointProvider.getHandler();
		}
	}

	public void setSubprotocols(List<String> subprotocols) {
		this.subprotocols = subprotocols;
	}

	@Override
	public List<String> getSubprotocols() {
		return this.subprotocols;
	}

	public void setExtensions(List<Extension> extensions) {
		this.extensions = extensions;
	}

	@Override
	public List<Extension> getExtensions() {
		return this.extensions;
	}

	public void setEncoders(List<Class<? extends Encoder>> encoders) {
		this.encoders = encoders;
	}

	@Override
	public List<Class<? extends Encoder>> getEncoders() {
		return this.encoders;
	}

	public void setDecoders(List<Class<? extends Decoder>> decoders) {
		this.decoders = decoders;
	}

	@Override
	public List<Class<? extends Decoder>> getDecoders() {
		return this.decoders;
	}

	public void setUserProperties(Map<String, Object> userProperties) {
		this.userProperties.clear();
		this.userProperties.putAll(userProperties);
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return this.userProperties;
	}

	@Override
	public Configurator getConfigurator() {
		return this;
	}


	// ServerEndpointConfig.Configurator implementation

	@SuppressWarnings("unchecked")
	@Override
	public final <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
		return (T) getEndpoint();
	}

	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		super.modifyHandshake(this, request, response);
	}


	// Remaining methods

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.endpointProvider != null) {
			this.endpointProvider.setBeanFactory(beanFactory);
		}
	}

	@Override
	public String toString() {
		return "ServerEndpointRegistration for path '" + getPath() + "': " + getEndpointClass();
	}

}
