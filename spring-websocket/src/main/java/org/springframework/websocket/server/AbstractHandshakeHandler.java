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

package org.springframework.websocket.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHandshakeHandler implements HandshakeHandler, BeanFactoryAware {

	private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	protected Log logger = LogFactory.getLog(getClass());

	private final Object webSocketHandler;

	private final Class<?> webSocketHandlerClass;

	private List<String> supportedProtocols;

	private AutowireCapableBeanFactory beanFactory;


	public AbstractHandshakeHandler(Object handler) {
		Assert.notNull(handler, "webSocketHandler is required");
		this.webSocketHandler = handler;
		this.webSocketHandlerClass = null;
	}

	public AbstractHandshakeHandler(Class<?> handlerClass) {
		Assert.notNull((handlerClass), "handlerClass is required");
		this.webSocketHandler = null;
		this.webSocketHandlerClass = handlerClass;
	}

	public void setSupportedProtocols(String... protocols) {
		this.supportedProtocols = Arrays.asList(protocols);
	}

	public String[] getSupportedProtocols() {
		return this.supportedProtocols.toArray(new String[this.supportedProtocols.size()]);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof AutowireCapableBeanFactory) {
			this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
		}
	}

	protected Object getWebSocketHandler() {
		if (this.webSocketHandlerClass != null) {
			Assert.notNull(this.beanFactory, "BeanFactory is required for WebSocket handler instances per request.");
			return this.beanFactory.createBean(this.webSocketHandlerClass);
		}
		else {
			return this.webSocketHandler;
		}
	}

	@Override
	public final boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response) throws Exception {

		logger.debug("Starting handshake for " + request.getURI());

		if (!HttpMethod.GET.equals(request.getMethod())) {
			response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
			response.getHeaders().setAllow(Collections.singleton(HttpMethod.GET));
			logger.debug("Only HTTP GET is allowed, current method is " + request.getMethod());
			return false;
		}
		if (!"WebSocket".equalsIgnoreCase(request.getHeaders().getUpgrade())) {
			handleInvalidUpgradeHeader(request, response);
			return false;
		}
		if (!request.getHeaders().getConnection().contains("Upgrade")) {
			handleInvalidConnectHeader(request, response);
			return false;
		}
		if (!isWebSocketVersionSupported(request)) {
			handleWebSocketVersionNotSupported(request, response);
			return false;
		}
		if (!isValidOrigin(request)) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			return false;
		}
		String wsKey = request.getHeaders().getSecWebSocketKey();
		if (wsKey == null) {
			logger.debug("Missing \"Sec-WebSocket-Key\" header");
			response.setStatusCode(HttpStatus.BAD_REQUEST);
			return false;
		}

		String protocol = selectProtocol(request.getHeaders().getSecWebSocketProtocol());
		// TODO: select extensions

		response.setStatusCode(HttpStatus.SWITCHING_PROTOCOLS);
		response.getHeaders().setUpgrade("WebSocket");
		response.getHeaders().setConnection("Upgrade");
		response.getHeaders().setSecWebSocketProtocol(protocol);
		response.getHeaders().setSecWebSocketAccept(getWebSocketKeyHash(wsKey));
		// TODO: response.getHeaders().setSecWebSocketExtensions(extensions);

		logger.debug("Successfully negotiated WebSocket handshake");

		// TODO: surely there is a better way to flush headers
		response.getBody();

		doHandshakeInternal(request, response, protocol);

		return true;
	}

	protected abstract void doHandshakeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String protocol) throws Exception;


	protected void handleInvalidUpgradeHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		logger.debug("Invalid Upgrade header " + request.getHeaders().getUpgrade());
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("Can \"Upgrade\" only to \"websocket\".".getBytes("UTF-8"));
	}

	protected void handleInvalidConnectHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		logger.debug("Invalid Connection header " + request.getHeaders().getConnection());
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("\"Connection\" must be \"upgrade\".".getBytes("UTF-8"));
	}

	protected boolean isWebSocketVersionSupported(ServerHttpRequest request) {
		String requestedVersion = request.getHeaders().getSecWebSocketVersion();
		for (String supportedVersion : getSupportedVerions()) {
			if (supportedVersion.equals(requestedVersion)) {
				return true;
			}
		}
		return false;
	}

	protected String[] getSupportedVerions() {
		return new String[] { "13" };
	}

	protected void handleWebSocketVersionNotSupported(ServerHttpRequest request, ServerHttpResponse response) {
		logger.debug("WebSocket version not supported " + request.getHeaders().get("Sec-WebSocket-Version"));
		response.setStatusCode(HttpStatus.UPGRADE_REQUIRED);
		response.getHeaders().setSecWebSocketVersion(StringUtils.arrayToCommaDelimitedString(getSupportedVerions()));
	}

	protected boolean isValidOrigin(ServerHttpRequest request) {
		String origin = request.getHeaders().getOrigin();
		if (origin != null) {
			// UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(origin);
			// TODO
			// A simple strategy checks against the current request's scheme/port/host
			// Or match scheme, port, and host against configured allowed origins (wild cards for hosts?)
			// return false;
		}
		return true;
	}

	protected String selectProtocol(List<String> requestedProtocols) {
		if (requestedProtocols != null) {
			for (String protocol : requestedProtocols) {
				if (this.supportedProtocols.contains(protocol)) {
					return protocol;
				}
			}
		}
		return null;
	}

    private String getWebSocketKeyHash(String key) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] bytes = digest.digest((key + GUID).getBytes(Charset.forName("ISO-8859-1")));
		return DatatypeConverter.printBase64Binary(bytes);
    }

}
