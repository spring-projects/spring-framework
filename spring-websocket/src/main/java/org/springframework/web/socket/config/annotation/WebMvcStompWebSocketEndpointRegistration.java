/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

/**
 * An abstract base class for configuring STOMP over WebSocket/SockJS endpoints.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebMvcStompWebSocketEndpointRegistration implements StompWebSocketEndpointRegistration {

	private final String[] paths;

	private final WebSocketHandler webSocketHandler;

	private final TaskScheduler sockJsTaskScheduler;

	@Nullable
	private HandshakeHandler handshakeHandler;

	private final List<HandshakeInterceptor> interceptors = new ArrayList<>();

	private final List<String> allowedOrigins = new ArrayList<>();

	@Nullable
	private SockJsServiceRegistration registration;


	public WebMvcStompWebSocketEndpointRegistration(
			String[] paths, WebSocketHandler webSocketHandler, TaskScheduler sockJsTaskScheduler) {

		Assert.notEmpty(paths, "No paths specified");
		Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");

		this.paths = paths;
		this.webSocketHandler = webSocketHandler;
		this.sockJsTaskScheduler = sockJsTaskScheduler;
	}


	@Override
	public StompWebSocketEndpointRegistration setHandshakeHandler(HandshakeHandler handshakeHandler) {
		this.handshakeHandler = handshakeHandler;
		return this;
	}

	@Override
	public StompWebSocketEndpointRegistration addInterceptors(HandshakeInterceptor... interceptors) {
		if (!ObjectUtils.isEmpty(interceptors)) {
			this.interceptors.addAll(Arrays.asList(interceptors));
		}
		return this;
	}

	@Override
	public StompWebSocketEndpointRegistration setAllowedOrigins(String... allowedOrigins) {
		this.allowedOrigins.clear();
		if (!ObjectUtils.isEmpty(allowedOrigins)) {
			this.allowedOrigins.addAll(Arrays.asList(allowedOrigins));
		}
		return this;
	}

	@Override
	public SockJsServiceRegistration withSockJS() {
		this.registration = new SockJsServiceRegistration();
		this.registration.setTaskScheduler(this.sockJsTaskScheduler);
		HandshakeInterceptor[] interceptors = getInterceptors();
		if (interceptors.length > 0) {
			this.registration.setInterceptors(interceptors);
		}
		if (this.handshakeHandler != null) {
			WebSocketTransportHandler handler = new WebSocketTransportHandler(this.handshakeHandler);
			this.registration.setTransportHandlerOverrides(handler);
		}
		if (!this.allowedOrigins.isEmpty()) {
			this.registration.setAllowedOrigins(StringUtils.toStringArray(this.allowedOrigins));
		}
		return this.registration;
	}

	protected HandshakeInterceptor[] getInterceptors() {
		List<HandshakeInterceptor> interceptors = new ArrayList<>(this.interceptors.size() + 1);
		interceptors.addAll(this.interceptors);
		interceptors.add(new OriginHandshakeInterceptor(this.allowedOrigins));
		return interceptors.toArray(new HandshakeInterceptor[0]);
	}

	public final MultiValueMap<HttpRequestHandler, String> getMappings() {
		MultiValueMap<HttpRequestHandler, String> mappings = new LinkedMultiValueMap<>();
		if (this.registration != null) {
			SockJsService sockJsService = this.registration.getSockJsService();
			for (String path : this.paths) {
				String pattern = (path.endsWith("/") ? path + "**" : path + "/**");
				SockJsHttpRequestHandler handler = new SockJsHttpRequestHandler(sockJsService, this.webSocketHandler);
				mappings.add(handler, pattern);
			}
		}
		else {
			for (String path : this.paths) {
				WebSocketHttpRequestHandler handler;
				if (this.handshakeHandler != null) {
					handler = new WebSocketHttpRequestHandler(this.webSocketHandler, this.handshakeHandler);
				}
				else {
					handler = new WebSocketHttpRequestHandler(this.webSocketHandler);
				}
				HandshakeInterceptor[] interceptors = getInterceptors();
				if (interceptors.length > 0) {
					handler.setHandshakeInterceptors(Arrays.asList(interceptors));
				}
				mappings.add(handler, path);
			}
		}
		return mappings;
	}

}
