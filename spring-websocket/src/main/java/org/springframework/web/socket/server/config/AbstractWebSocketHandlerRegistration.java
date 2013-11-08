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

package org.springframework.web.socket.server.config;

import java.util.Arrays;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.DefaultHandshakeHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;


/**
 * Base class for {@link WebSocketHandlerRegistration}s that gathers all the configuration
 * options but allows sub-classes to put together the actual HTTP request mappings.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractWebSocketHandlerRegistration<M> implements WebSocketHandlerRegistration {

	private MultiValueMap<WebSocketHandler, String> handlerMap = new LinkedMultiValueMap<WebSocketHandler, String>();

	private HandshakeInterceptor[] interceptors;

	private HandshakeHandler handshakeHandler;

	private SockJsServiceRegistration sockJsServiceRegistration;

	private final TaskScheduler sockJsTaskScheduler;


	public AbstractWebSocketHandlerRegistration(TaskScheduler defaultTaskScheduler) {
		this.sockJsTaskScheduler = defaultTaskScheduler;
	}

	@Override
	public WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths) {
		Assert.notNull(handler);
		Assert.notEmpty(paths);
		this.handlerMap.put(handler, Arrays.asList(paths));
		return this;
	}

	@Override
	public WebSocketHandlerRegistration setHandshakeHandler(HandshakeHandler handshakeHandler) {
		this.handshakeHandler = handshakeHandler;
		return this;
	}

	public HandshakeHandler getHandshakeHandler() {
		return handshakeHandler;
	}

	@Override
	public WebSocketHandlerRegistration addInterceptors(HandshakeInterceptor... interceptors) {
		this.interceptors = interceptors;
		return this;
	}

	protected HandshakeInterceptor[] getInterceptors() {
		return this.interceptors;
	}

	/**
	 * @param interceptors the interceptors to set
	 */
	public void setInterceptors(HandshakeInterceptor[] interceptors) {
		this.interceptors = interceptors;
	}

	@Override
	public SockJsServiceRegistration withSockJS() {

		this.sockJsServiceRegistration = new SockJsServiceRegistration(this.sockJsTaskScheduler);
		this.sockJsServiceRegistration.setInterceptors(this.interceptors);

		if (this.handshakeHandler != null) {
			WebSocketTransportHandler transportHandler = new WebSocketTransportHandler(this.handshakeHandler);
			this.sockJsServiceRegistration.setTransportHandlerOverrides(transportHandler);
		}

		return this.sockJsServiceRegistration;
	}

	final M getMappings() {

		M mappings = createMappings();

		if (this.sockJsServiceRegistration != null) {
			SockJsService sockJsService = this.sockJsServiceRegistration.getSockJsService();
			for (WebSocketHandler wsHandler : this.handlerMap.keySet()) {
				for (String path : this.handlerMap.get(wsHandler)) {
					String pathPattern = path.endsWith("/") ? path + "**" : path + "/**";
					addSockJsServiceMapping(mappings, sockJsService, wsHandler, pathPattern);
				}
			}
		}
		else {
			HandshakeHandler handshakeHandler = getOrCreateHandshakeHandler();
			for (WebSocketHandler wsHandler : this.handlerMap.keySet()) {
				for (String path : this.handlerMap.get(wsHandler)) {
					addWebSocketHandlerMapping(mappings, wsHandler, handshakeHandler, this.interceptors, path);
				}
			}
		}

		return mappings;
	}

	private HandshakeHandler getOrCreateHandshakeHandler() {
		return (this.handshakeHandler != null) ? this.handshakeHandler : new DefaultHandshakeHandler();
	}

	protected abstract M createMappings();

	protected abstract void addSockJsServiceMapping(M mappings, SockJsService sockJsService,
			WebSocketHandler handler, String pathPattern);

	protected abstract void addWebSocketHandlerMapping(M mappings, WebSocketHandler wsHandler,
			HandshakeHandler handshakeHandler, HandshakeInterceptor[] interceptors, String path);

}
