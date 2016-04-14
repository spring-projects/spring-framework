/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.util;

import java.io.UnsupportedEncodingException;

import org.springframework.web.server.ServerWebExchange;

/**
 * A helper class to obtain the lookup path for path matching purposes.
 *
 * @author Rossen Stoyanchev
 */
public class HttpRequestPathHelper {

	private boolean urlDecode = true;


	// TODO: sanitize path, default/request encoding?, remove path params?

	/**
	 * Set if the request path should be URL-decoded.
	 * <p>Default is "true".
	 * @see UriUtils#decode(String, String)
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlDecode = urlDecode;
	}

	/**
	 * Whether the request path should be URL decoded.
	 */
	public boolean shouldUrlDecode() {
		return this.urlDecode;
	}


	public String getLookupPathForRequest(ServerWebExchange exchange) {
		String path = exchange.getRequest().getURI().getPath();
		return (this.shouldUrlDecode() ? decode(path) : path);
	}

	private String decode(String path) {
		try {
			return UriUtils.decode(path, "UTF-8");
		}
		catch (UnsupportedEncodingException ex) {
			// Should not happen
			throw new IllegalStateException("Could not decode request string [" + path + "]");
		}
	}

}