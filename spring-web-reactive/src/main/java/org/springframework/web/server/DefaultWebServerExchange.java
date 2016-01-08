/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.web.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link WebServerExchange}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultWebServerExchange implements WebServerExchange {

	private final ServerHttpRequest request;

	private final ServerHttpResponse response;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();


	public DefaultWebServerExchange(ServerHttpRequest request, ServerHttpResponse response) {
		Assert.notNull(request, "'request' is required.");
		Assert.notNull(response, "'response' is required.");
		this.request = request;
		this.response = response;
	}


	@Override
	public ServerHttpRequest getRequest() {
		return this.request;
	}

	@Override
	public ServerHttpResponse getResponse() {
		return this.response;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

}
