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

package org.springframework.web.socket.server.support;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * An interceptor to copy HTTP session attributes into the map of "handshake attributes"
 * made available through {@link WebSocketSession#getHandshakeAttributes()}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HttpSessionHandshakeInterceptor implements HandshakeInterceptor {

	private static Log logger = LogFactory.getLog(HttpSessionHandshakeInterceptor.class);

	private Collection<String> attributeNames;


	/**
	 * A constructor for copying all available HTTP session attributes.
	 */
	public HttpSessionHandshakeInterceptor() {
		this(null);
	}

	/**
	 * A constructor for copying a subset of HTTP session attributes.
	 * @param attributeNames the HTTP session attributes to copy
	 */
	public HttpSessionHandshakeInterceptor(Collection<String> attributeNames) {
		this.attributeNames = attributeNames;
	}


	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

		if (request instanceof ServletServerHttpRequest) {
			ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
			HttpSession session = servletRequest.getServletRequest().getSession(false);
			if (session != null) {
				Enumeration<String> names = session.getAttributeNames();
				while (names.hasMoreElements()) {
					String name = names.nextElement();
					if (CollectionUtils.isEmpty(this.attributeNames) || this.attributeNames.contains(name)) {
						if (logger.isTraceEnabled()) {
							logger.trace("Adding HTTP session attribute to handshake attributes: " + name);
						}
						attributes.put(name, session.getAttribute(name));
					}
					else {
						if (logger.isTraceEnabled()) {
							logger.trace("Skipped HTTP session attribute");
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception ex) {
	}

}
