/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.socket.server.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link javax.websocket.server.ServerEndpointConfig}
 * for use in {@code RequestUpgradeStrategy} implementations.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultServerEndpointConfig extends ServerEndpointConfig.Configurator
		implements ServerEndpointConfig {

	private final String path;

	private final Endpoint endpoint;

	private List<String> protocols = new ArrayList<>();


	/**
	 * Constructor with a path and an {@code javax.websocket.Endpoint}.
	 * @param path the endpoint path
	 * @param endpoint the endpoint instance
	 */
	public DefaultServerEndpointConfig(String path, Endpoint endpoint) {
		Assert.hasText(path, "path must not be empty");
		Assert.notNull(endpoint, "endpoint must not be null");
		this.path = path;
		this.endpoint = endpoint;
	}


	@Override
	public List<Class<? extends Encoder>> getEncoders() {
		return new ArrayList<>();
	}

	@Override
	public List<Class<? extends Decoder>> getDecoders() {
		return new ArrayList<>();
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return new HashMap<>();
	}

	@Override
	public Class<?> getEndpointClass() {
		return this.endpoint.getClass();
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public void setSubprotocols(List<String> protocols) {
		this.protocols = protocols;
	}

	@Override
	public List<String> getSubprotocols() {
		return this.protocols;
	}

	@Override
	public List<Extension> getExtensions() {
		return new ArrayList<>();
	}

	@Override
	public Configurator getConfigurator() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
		return (T) this.endpoint;
	}

	@Override
	public String toString() {
		return "DefaultServerEndpointConfig for path '" + getPath() + "': " + getEndpointClass();
	}

}
