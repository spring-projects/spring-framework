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

package org.springframework.websocket.server.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.websocket.endpoint.WebSocketHandlerEndpoint;


/**
 * An implementation of {@link javax.websocket.server.ServerEndpointConfig} that also
 * holds the target {@link javax.websocket.Endpoint} as a reference or a bean name.
 * The target can also be {@link org.springframework.websocket.WebSocketHandler}, in
 * which case it will be adapted via {@link WebSocketHandlerEndpoint}.
 *
 * <p>
 * Beans of this type are detected by {@link EndpointExporter} and
 * registered with a Java WebSocket runtime at startup.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class EndpointRegistration implements ServerEndpointConfig, BeanFactoryAware {

	private final String path;

	private final Class<? extends Endpoint> endpointClass;

	private final Object endpointBean;

    private List<Class<? extends Encoder>> encoders = new ArrayList<Class<? extends Encoder>>();

    private List<Class<? extends Decoder>> decoders = new ArrayList<Class<? extends Decoder>>();

	private List<String> subprotocols = new ArrayList<String>();

	private List<Extension> extensions = new ArrayList<Extension>();

	private final Map<String, Object> userProperties = new HashMap<String, Object>();

	private Configurator configurator = new Configurator() {};

	private BeanFactory beanFactory;


	/**
	 * Class constructor with the {@code javax.webscoket.Endpoint} class.
	 * TODO
	 *
	 * @param path
	 * @param endpointClass
	 */
	public EndpointRegistration(String path, Class<? extends Endpoint> endpointClass) {
		this(path, endpointClass, null);
	}

	public EndpointRegistration(String path, Object bean) {
		this(path, null, bean);
	}

	public EndpointRegistration(String path, String beanName) {
		this(path, null, beanName);
	}

	private EndpointRegistration(String path, Class<? extends Endpoint> endpointClass, Object bean) {
		Assert.hasText(path, "path must not be empty");
		Assert.isTrue((endpointClass != null || bean != null), "Neither endpoint class nor endpoint bean provided");
		this.path = path;
		this.endpointClass = endpointClass;
		this.endpointBean = bean;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends Endpoint> getEndpointClass() {
		if (this.endpointClass != null) {
			return this.endpointClass;
		}
		Class<?> beanClass = this.endpointBean.getClass();
		if (beanClass.equals(String.class)) {
			beanClass = this.beanFactory.getType((String) this.endpointBean);
		}
		beanClass = ClassUtils.getUserClass(beanClass);
		if (Endpoint.class.isAssignableFrom(beanClass)) {
			return (Class<? extends Endpoint>) beanClass;
		}
		else {
			throw new IllegalStateException("Invalid endpoint bean: must be of type ... TODO ");
		}
	}

	public Endpoint getEndpoint() {
		if (this.endpointClass != null) {
			WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
			if (wac == null) {
				throw new IllegalStateException("Failed to find WebApplicationContext. "
						+ "Was org.springframework.web.context.ContextLoader used to load the WebApplicationContext?");
			}
			return wac.getAutowireCapableBeanFactory().createBean(this.endpointClass);
		}
		Object bean = this.endpointBean;
		if (this.endpointBean instanceof String) {
			bean = this.beanFactory.getBean((String) this.endpointBean);
		}
		return (Endpoint) bean;
	}

	public void setSubprotocols(List<String> subprotocols) {
		this.subprotocols = subprotocols;
	}

	@Override
	public List<String> getSubprotocols() {
		return this.subprotocols;
	}

	public void setExtensions(List<Extension> extensions) {
		// TODO: verify against ServerContainer.getInstalledExtensions()
		this.extensions = extensions;
	}

	@Override
	public List<Extension> getExtensions() {
		return this.extensions;
	}

	public void setUserProperties(Map<String, Object> userProperties) {
		this.userProperties.clear();
		this.userProperties.putAll(userProperties);
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return this.userProperties;
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

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * The {@link Configurator#getEndpointInstance(Class)} method is always ignored.
	 */
	public void setConfigurator(Configurator configurator) {
		this.configurator = configurator;
	}

	@Override
	public Configurator getConfigurator() {
		return new Configurator() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
				return (T) EndpointRegistration.this.getEndpoint();
			}
			@Override
			public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
				EndpointRegistration.this.configurator.modifyHandshake(sec, request, response);
			}
			@Override
			public boolean checkOrigin(String originHeaderValue) {
				return EndpointRegistration.this.configurator.checkOrigin(originHeaderValue);
			}
			@Override
			public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
				return EndpointRegistration.this.configurator.getNegotiatedSubprotocol(supported, requested);
			}
			@Override
			public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {
				return EndpointRegistration.this.configurator.getNegotiatedExtensions(installed, requested);
			}
		};
	}

}
