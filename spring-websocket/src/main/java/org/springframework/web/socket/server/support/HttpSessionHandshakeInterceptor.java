/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * An interceptor to copy HTTP session attributes into the map of "handshake attributes"
 * made available through {@link WebSocketSession#getAttributes()}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HttpSessionHandshakeInterceptor implements HandshakeInterceptor {

	/**
	 * The name of the attribute under which the HTTP session id is exposed when
	 * {@link #setCopyHttpSessionId(boolean) copyHttpSessionId} is "true".
	 */
	public static final String HTTP_SESSION_ID_ATTR_NAME = "HTTP.SESSION.ID";

	private final Collection<String> attributeNames;

	private boolean copyHttpSessionId;


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

	/**
	 * When set to "true", the HTTP session id is copied to the WebSocket
	 * handshake attributes, and is subsequently available via
	 * {@link org.springframework.web.socket.WebSocketSession#getAttributes()}
	 * under the key {@link #HTTP_SESSION_ID_ATTR_NAME}.
	 * <p>By default this is "false".
	 * @param copyHttpSessionId whether to copy the HTTP session id.
	 */
	public void setCopyHttpSessionId(boolean copyHttpSessionId) {
		this.copyHttpSessionId = copyHttpSessionId;
	}

	/**
	 * Whether to copy the HTTP session id to the handshake attributes.
	 */
	public boolean isCopyHttpSessionId() {
		return this.copyHttpSessionId;
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
						attributes.put(name, session.getAttribute(name));
					}
				}
				if (isCopyHttpSessionId()) {
					attributes.put(HTTP_SESSION_ID_ATTR_NAME, session.getId());
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
