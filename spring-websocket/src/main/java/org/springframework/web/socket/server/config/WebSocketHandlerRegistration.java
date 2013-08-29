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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.DefaultHandshakeHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;


/**
 * A helper class for configuring {@link WebSocketHandler} request handling
 * including SockJS fallback options.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHandlerRegistration {

	private MultiValueMap<WebSocketHandler, String> handlerMap =
			new LinkedMultiValueMap<WebSocketHandler, String>();

	private final List<HandshakeInterceptor> interceptors = new ArrayList<HandshakeInterceptor>();

	private HandshakeHandler handshakeHandler;

	private SockJsServiceRegistration sockJsServiceRegistration;

	private final TaskScheduler defaultTaskScheduler;


	public WebSocketHandlerRegistration(TaskScheduler defaultTaskScheduler) {
		this.defaultTaskScheduler = defaultTaskScheduler;
	}

	public WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths) {
		Assert.notNull(handler);
		Assert.notEmpty(paths);
		this.handlerMap.put(handler, Arrays.asList(paths));
		return this;
	}

	public WebSocketHandlerRegistration setHandshakeHandler(HandshakeHandler handshakeHandler) {
		this.handshakeHandler = handshakeHandler;
		return this;
	}

	public HandshakeHandler getHandshakeHandler() {
		return handshakeHandler;
	}

	public void addInterceptors(HandshakeInterceptor... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	public SockJsServiceRegistration withSockJS() {
		this.sockJsServiceRegistration = new SockJsServiceRegistration(this.defaultTaskScheduler);
		this.sockJsServiceRegistration.setInterceptors(
				this.interceptors.toArray(new HandshakeInterceptor[this.interceptors.size()]));
		return this.sockJsServiceRegistration;
	}

	MultiValueMap<HttpRequestHandler, String> getMappings() {
		MultiValueMap<HttpRequestHandler, String> mappings = new LinkedMultiValueMap<HttpRequestHandler, String>();
		if (this.sockJsServiceRegistration == null) {
			HandshakeHandler handshakeHandler = getOrCreateHandshakeHandler();
			for (WebSocketHandler handler : this.handlerMap.keySet()) {
				for (String path : this.handlerMap.get(handler)) {
					WebSocketHttpRequestHandler httpHandler = new WebSocketHttpRequestHandler(handler, handshakeHandler);
					httpHandler.setHandshakeInterceptors(this.interceptors);
					mappings.add(httpHandler, path);
				}
			}
		}
		else {
			SockJsService sockJsService = this.sockJsServiceRegistration.getSockJsService(getAllPrefixes());
			for (WebSocketHandler handler : this.handlerMap.keySet()) {
				for (String path : this.handlerMap.get(handler)) {
					SockJsHttpRequestHandler httpHandler = new SockJsHttpRequestHandler(sockJsService, handler);
					mappings.add(httpHandler, path.endsWith("/") ? path + "**" : path + "/**");
				}
			}
		}
		return mappings;
	}

	private HandshakeHandler getOrCreateHandshakeHandler() {
		return (this.handshakeHandler != null) ? this.handshakeHandler : new DefaultHandshakeHandler();
	}

	private final String[] getAllPrefixes() {
		List<String> all = new ArrayList<String>();
		for (List<String> prefixes: this.handlerMap.values()) {
			all.addAll(prefixes);
		}
		return all.toArray(new String[all.size()]);
	}

}
