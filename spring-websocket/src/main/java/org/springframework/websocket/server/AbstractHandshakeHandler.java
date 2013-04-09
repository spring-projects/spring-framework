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
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.websocket.WebSocketHandler;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHandshakeHandler implements HandshakeHandler, BeanFactoryAware {

	private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	protected Log logger = LogFactory.getLog(getClass());

	private final WebSocketHandler webSocketHandler;

	private final Class<? extends WebSocketHandler> handlerClass;

	private List<String> protocols;

	private AutowireCapableBeanFactory beanFactory;


	public AbstractHandshakeHandler(WebSocketHandler webSocketHandler) {
		Assert.notNull(webSocketHandler, "webSocketHandler is required");
		this.webSocketHandler = webSocketHandler;
		this.handlerClass = null;
	}

	public AbstractHandshakeHandler(Class<? extends WebSocketHandler> handlerClass) {
		Assert.notNull((handlerClass), "handlerClass is required");
		this.webSocketHandler = null;
		this.handlerClass = handlerClass;
	}

	public void setProtocols(String... protocols) {
		this.protocols = Arrays.asList(protocols);
	}

	public String[] getProtocols() {
		return this.protocols.toArray(new String[this.protocols.size()]);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof AutowireCapableBeanFactory) {
			this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
		}
	}

	protected WebSocketHandler getWebSocketHandler() {
		if (this.handlerClass != null) {
			Assert.notNull(this.beanFactory, "BeanFactory is required for WebSocketHandler instance per request.");
			return this.beanFactory.createBean(this.handlerClass);
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
		if (!validateUpgradeHeader(request, response)) {
			return false;
		}
		if (!validateConnectHeader(request, response)) {
			return false;
		}
		if (!validateWebSocketVersion(request, response)) {
			return false;
		}
		if (!validateOrigin(request, response)) {
			return false;
		}
		String wsKey = request.getHeaders().getSecWebSocketKey();
		if (wsKey == null) {
			logger.debug("Missing \"Sec-WebSocket-Key\" header");
			response.setStatusCode(HttpStatus.BAD_REQUEST);
			return false;
		}
		String protocol = selectProtocol(request.getHeaders().getSecWebSocketProtocol());
		// TODO: request.getHeaders().getSecWebSocketExtensions())

		response.setStatusCode(HttpStatus.SWITCHING_PROTOCOLS);
		response.getHeaders().setUpgrade("WebSocket");
		response.getHeaders().setConnection("Upgrade");
		response.getHeaders().setSecWebSocketProtocol(protocol);
		response.getHeaders().setSecWebSocketAccept(getWebSocketKeyHash(wsKey));
		// TODO: response.getHeaders().setSecWebSocketExtensions(extensions);

		logger.debug("Successfully negotiated WebSocket handshake");

		// TODO: surely there is a better way to flush the headers
		response.getBody();

		doHandshakeInternal(request, response, protocol);

		return true;
	}

	protected abstract void doHandshakeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String protocol) throws Exception;

	protected boolean validateUpgradeHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		if (!"WebSocket".equalsIgnoreCase(request.getHeaders().getUpgrade())) {
			response.setStatusCode(HttpStatus.BAD_REQUEST);
			response.getBody().write("Can \"Upgrade\" only to \"websocket\".".getBytes("UTF-8"));
			logger.debug("Invalid Upgrade header " + request.getHeaders().getUpgrade());
			return false;
		}
		return true;
	}

	protected boolean validateConnectHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		if (!request.getHeaders().getConnection().contains("Upgrade")) {
			response.setStatusCode(HttpStatus.BAD_REQUEST);
			response.getBody().write("\"Connection\" must be \"upgrade\".".getBytes("UTF-8"));
			logger.debug("Invalid Connection header " + request.getHeaders().getConnection());
			return false;
		}
		return true;
	}

	protected boolean validateWebSocketVersion(ServerHttpRequest request, ServerHttpResponse response) {
		if (!"13".equals(request.getHeaders().getSecWebSocketVersion())) {
			response.setStatusCode(HttpStatus.UPGRADE_REQUIRED);
			response.getHeaders().set("Sec-WebSocket-Version", "13");
			logger.debug("WebSocket version not supported " + request.getHeaders().get("Sec-WebSocket-Version"));
			return false;
		}
		return true;
	}

	protected boolean validateOrigin(ServerHttpRequest request, ServerHttpResponse response) {
		String origin = request.getHeaders().getOrigin();
		if (origin != null) {
			UriComponentsBuilder originUriBuilder = UriComponentsBuilder.fromHttpUrl(origin);

			// TODO
			// Check scheme, port, and host against list of configured origins (allow wild cards in the host?)
			// Another strategy might be to match current request's scheme/port/host

			// response.setStatusCode(HttpStatus.FORBIDDEN);
			// return false;
		}
		return true;
	}

	protected String selectProtocol(List<String> requestedProtocols) {
		if (requestedProtocols != null) {
			for (String p : requestedProtocols) {
				if (this.protocols.contains(p)) {
					return p;
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
