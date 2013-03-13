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

package org.springframework.websocket.support;

import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.websocket.WebSocketHandler;


/**
 * An implementation of {@link javax.websocket.server.ServerEndpointConfig} that also
 * holds the target {@link javax.websocket.Endpoint} as a reference or a bean name.
 * The target can also be {@link org.springframework.websocket.WebSocketHandler}, in
 * which case it will be adapted via {@link StandardWebSocketHandlerAdapter}.
 *
 * <p>
 * Beans of this type are detected by {@link ServerEndpointPostProcessor} and
 * registered with a Java WebSocket runtime at startup.
 *
 * @author Rossen Stoyanchev
 */
public class ServerEndpointRegistration implements ServerEndpointConfig, BeanFactoryAware {

	private final String path;

	private final Object bean;

	private List<String> subprotocols = new ArrayList<String>();

	private List<Extension> extensions = new ArrayList<Extension>();

	private Map<String, Object> userProperties = new HashMap<String, Object>();

	private BeanFactory beanFactory;

	private final Configurator configurator = new Configurator() {};


	public ServerEndpointRegistration(String path, String beanName) {
		Assert.hasText(path, "path must not be empty");
		Assert.notNull(beanName, "beanName is required");
		this.path = path;
		this.bean = beanName;
	}

	public ServerEndpointRegistration(String path, Object bean) {
		Assert.hasText(path, "path must not be empty");
		Assert.notNull(bean, "bean is required");
		this.path = path;
		this.bean = bean;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends Endpoint> getEndpointClass() {
		Class<?> beanClass = this.bean.getClass();
		if (beanClass.equals(String.class)) {
			beanClass = this.beanFactory.getType((String) this.bean);
		}
		beanClass = ClassUtils.getUserClass(beanClass);
		if (WebSocketHandler.class.isAssignableFrom(beanClass)) {
			return StandardWebSocketHandlerAdapter.class;
		}
		else {
			return (Class<? extends Endpoint>) beanClass;
		}
	}

	protected Endpoint getEndpoint() {
		Object bean = this.bean;
		if (this.bean instanceof String) {
			bean = this.beanFactory.getBean((String) this.bean);
		}
		if (bean instanceof WebSocketHandler) {
			return new StandardWebSocketHandlerAdapter((WebSocketHandler) bean);
		}
		else {
			return (Endpoint) bean;
		}
	}

	@Override
	public List<String> getSubprotocols() {
		return this.subprotocols;
	}

	public void setSubprotocols(List<String> subprotocols) {
		this.subprotocols = subprotocols;
	}

	@Override
	public List<Extension> getExtensions() {
		return this.extensions;
	}

	public void setExtensions(List<Extension> extensions) {
		// TODO: verify against ServerContainer.getInstalledExtensions()
		this.extensions = extensions;
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return this.userProperties;
	}

	public void setUserProperties(Map<String, Object> userProperties) {
		this.userProperties = userProperties;
	}

	@Override
	public List<Class<? extends Encoder>> getEncoders() {
		return Collections.emptyList();
	}

	@Override
	public List<Class<? extends Decoder>> getDecoders() {
		return Collections.emptyList();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Configurator getConfigurator() {
		return new Configurator() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
				return (T) ServerEndpointRegistration.this.getEndpoint();
			}
			@Override
			public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
				ServerEndpointRegistration.this.modifyHandshake(request, response);
			}
			@Override
			public boolean checkOrigin(String originHeaderValue) {
				return ServerEndpointRegistration.this.checkOrigin(originHeaderValue);
			}
			@Override
			public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
				return ServerEndpointRegistration.this.selectSubProtocol(requested);
			}
			@Override
			public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {
				return ServerEndpointRegistration.this.selectExtensions(requested);
			}
		};
	}

	protected void modifyHandshake(HandshakeRequest request, HandshakeResponse response) {
		this.configurator.modifyHandshake(this, request, response);
	}

	protected boolean checkOrigin(String originHeaderValue) {
		return this.configurator.checkOrigin(originHeaderValue);
	}

	protected String selectSubProtocol(List<String> requested) {
		return this.configurator.getNegotiatedSubprotocol(getSubprotocols(), requested);
	}

	protected List<Extension> selectExtensions(List<Extension> requested) {
		return this.configurator.getNegotiatedExtensions(getExtensions(), requested);
	}

}
