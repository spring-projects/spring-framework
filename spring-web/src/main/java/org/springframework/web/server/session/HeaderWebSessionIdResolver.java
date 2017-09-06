/*
 * Copyright 2002-2017 the original author or authors.
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
 * Request and response header-based {@link WebSessionIdResolver}.
 * 
 * @author Greg Turnquist
 * @author Rob Winch
 * @since 5.0
 */
public class HeaderWebSessionIdResolver implements WebSessionIdResolver {

	/** Default value for {@link #setHeaderName(String)}. */
	public static final String DEFAULT_HEADER_NAME = "SESSION";


	private String headerName = DEFAULT_HEADER_NAME;


	/**
	 * Set the name of the session header to use for the session id.
	 * The name is used to extract the session id from the request headers as
	 * well to set the session id on the response headers.
	 * <p>By default set to {@code DEFAULT_HEADER_NAME}
	 * @param headerName the header name
	 */
	public void setHeaderName(String headerName) {
		Assert.hasText(headerName, "'headerName' must not be empty.");
		this.headerName = headerName;
	}

	/**
	 * Return the configured header name.
	 * @return the configured header name
	 */
	public String getHeaderName() {
		return this.headerName;
	}


	@Override
	public List<String> resolveSessionIds(ServerWebExchange exchange) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		return headers.getOrDefault(getHeaderName(), Collections.emptyList());
	}

	@Override
	public void setSessionId(ServerWebExchange exchange, String id) {
		Assert.notNull(id, "'id' is required.");
		exchange.getResponse().getHeaders().set(getHeaderName(), id);
	}

	@Override
	public void expireSession(ServerWebExchange exchange) {
		this.setSessionId(exchange, "");
	}

}
