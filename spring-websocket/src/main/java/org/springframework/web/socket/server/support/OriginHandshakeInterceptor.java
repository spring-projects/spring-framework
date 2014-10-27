/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.server.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * An interceptor to check request {@code Origin} header value against a collection of
 * allowed origins.
 *
 * @author Sebastien Deleuze
 * @since 4.1.2
 */
public class OriginHandshakeInterceptor implements HandshakeInterceptor {

	protected Log logger = LogFactory.getLog(getClass());

	private final List<String> allowedOrigins;


	/**
	 * Default constructor with no origin allowed.
	 */
	public OriginHandshakeInterceptor() {
		this.allowedOrigins = new ArrayList<String>();
	}

	/**
	 * Use this property to define a collection of allowed origins.
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		this.allowedOrigins.clear();
		if (allowedOrigins != null) {
			this.allowedOrigins.addAll(allowedOrigins);
		}
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
		if (!isValidOrigin(request)) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake request rejected, Origin header value "
						+ request.getHeaders().getOrigin() + " not allowed");
			}
			return false;
		}
		return true;
	}

	protected boolean isValidOrigin(ServerHttpRequest request) {
		return this.allowedOrigins.contains(request.getHeaders().getOrigin());
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
	}

}
