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

	private SockJsServiceRegistration sockJsServiceRegistration;

	private TaskScheduler defaultTaskScheduler;


	public WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths) {
		Assert.notNull(handler);
		Assert.notEmpty(paths);
		this.handlerMap.put(handler, Arrays.asList(paths));
		return this;
	}

	protected MultiValueMap<WebSocketHandler, String> getHandlerMap() {
		return this.handlerMap;
	}

	public void addInterceptors(HandshakeInterceptor... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	protected List<HandshakeInterceptor> getInterceptors() {
		return this.interceptors;
	}

	public SockJsServiceRegistration withSockJS() {
		this.sockJsServiceRegistration = new SockJsServiceRegistration(this.defaultTaskScheduler);
		this.sockJsServiceRegistration.setInterceptors(
				getInterceptors().toArray(new HandshakeInterceptor[getInterceptors().size()]));
		return this.sockJsServiceRegistration;
	}

	protected SockJsServiceRegistration getSockJsServiceRegistration() {
		return this.sockJsServiceRegistration;
	}

	protected void setDefaultTaskScheduler(TaskScheduler defaultTaskScheduler) {
		this.defaultTaskScheduler = defaultTaskScheduler;
	}

	protected TaskScheduler getDefaultTaskScheduler() {
		return this.defaultTaskScheduler;
	}

	protected MultiValueMap<HttpRequestHandler, String> getMappings() {
		MultiValueMap<HttpRequestHandler, String> mappings = new LinkedMultiValueMap<HttpRequestHandler, String>();
		if (getSockJsServiceRegistration() == null) {
			HandshakeHandler handshakeHandler = createHandshakeHandler();
			for (WebSocketHandler handler : getHandlerMap().keySet()) {
				for (String path : getHandlerMap().get(handler)) {
					WebSocketHttpRequestHandler httpHandler = new WebSocketHttpRequestHandler(handler, handshakeHandler);
					httpHandler.setHandshakeInterceptors(getInterceptors());
					mappings.add(httpHandler, path);
				}
			}
		}
		else {
			SockJsService sockJsService = getSockJsServiceRegistration().getSockJsService(getAllPrefixes());
			for (WebSocketHandler handler : getHandlerMap().keySet()) {
				for (String path : getHandlerMap().get(handler)) {
					SockJsHttpRequestHandler httpHandler = new SockJsHttpRequestHandler(sockJsService, handler);
					mappings.add(httpHandler, path.endsWith("/") ? path + "**" : path + "/**");
				}
			}

		}
		return mappings;
	}

	protected DefaultHandshakeHandler createHandshakeHandler() {
		return new DefaultHandshakeHandler();
	}

	protected final String[] getAllPrefixes() {
		List<String> all = new ArrayList<String>();
		for (List<String> prefixes: this.handlerMap.values()) {
			all.addAll(prefixes);
		}
		return all.toArray(new String[all.size()]);
	}

}
