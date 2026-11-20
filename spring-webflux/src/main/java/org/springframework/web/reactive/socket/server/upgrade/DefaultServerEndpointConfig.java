/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.server.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerEndpointConfig;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link jakarta.websocket.server.ServerEndpointConfig}
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
	 * Constructor with a path and an {@code jakarta.websocket.Endpoint}.
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
