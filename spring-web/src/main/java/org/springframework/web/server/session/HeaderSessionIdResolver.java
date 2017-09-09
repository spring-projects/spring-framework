/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.web.server.session;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * Header-based {@link WebSessionIdResolver}.
 * 
 * @author Greg Turnquist
 * @since 5.0
 */
public class HeaderSessionIdResolver implements WebSessionIdResolver {

	private String headerName = "SESSION";

	/**
	 * Set the name of the session header to use for the session id.
	 * <p>By default set to "SESSION".
	 * @param headerName the header name
	 */
	public void setHeaderName(String headerName) {
		Assert.hasText(headerName, "'headerName' must not be empty.");
		this.headerName = headerName;
	}

	/**
	 * Return the configured header name.
	 */
	public String getHeaderName() {
		return this.headerName;
	}

	@Override
	public List<String> resolveSessionIds(ServerWebExchange exchange) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		List<String> sessionHeaders = headers.get(this.getHeaderName());
		if (sessionHeaders == null) {
			return Collections.emptyList();
		}
		return sessionHeaders;
	}

	@Override
	public void setSessionId(ServerWebExchange exchange, String id) {
		Assert.notNull(id, "'id' is required.");
		exchange.getResponse().getHeaders().set(this.headerName, id);
	}

	@Override
	public void expireSession(ServerWebExchange exchange) {
		this.setSessionId(exchange, "");
	}
}
