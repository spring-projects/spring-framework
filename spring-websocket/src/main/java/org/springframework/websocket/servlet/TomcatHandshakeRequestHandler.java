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

package org.springframework.websocket.servlet;

import java.lang.reflect.Method;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.tomcat.websocket.server.WsHandshakeRequest;
import org.apache.tomcat.websocket.server.WsHttpUpgradeHandler;
import org.apache.tomcat.websocket.server.WsServerContainer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.websocket.AbstractHandshakeRequestHandler;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.support.ServerEndpointRegistration;
import org.springframework.websocket.support.StandardWebSocketHandlerAdapter;


/**
*
* @author Rossen Stoyanchev
 * @since 4.0
*/
public class TomcatHandshakeRequestHandler extends AbstractHandshakeRequestHandler {

	private final Endpoint endpoint;

	private final ServerEndpointConfig endpointConfig;


	public TomcatHandshakeRequestHandler(WebSocketHandler webSocketHandler) {
		this.endpoint = new StandardWebSocketHandlerAdapter(webSocketHandler);
		this.endpointConfig = new ServerEndpointRegistration("/shouldnotmatter", this.endpoint);
	}

	public TomcatHandshakeRequestHandler(Endpoint endpoint) {
		this.endpoint = endpoint;
		this.endpointConfig = new ServerEndpointRegistration("/shouldnotmatter", this.endpoint);
	}

	@Override
	public void doHandshakeInternal(ServerHttpRequest request, ServerHttpResponse response, String protocol) throws Exception {

		logger.debug("Upgrading HTTP request");

		Assert.isTrue(request instanceof ServletServerHttpRequest);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

       WsHandshakeRequest wsRequest = new WsHandshakeRequest(servletRequest);
       Method method = ReflectionUtils.findMethod(WsHandshakeRequest.class, "finished");
       ReflectionUtils.makeAccessible(method);
       method.invoke(wsRequest);

       WsHttpUpgradeHandler wsHandler = servletRequest.upgrade(WsHttpUpgradeHandler.class);

       wsHandler.preInit(this.endpoint, this.endpointConfig, WsServerContainer.getServerContainer(),
       		wsRequest, protocol, Collections.<String, String> emptyMap(), servletRequest.isSecure());
	}

}