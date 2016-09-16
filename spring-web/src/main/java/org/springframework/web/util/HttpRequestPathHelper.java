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
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * A helper class to obtain the lookup path for path matching purposes.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
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
		String path = getPathWithinApplication(exchange.getRequest());
		return (this.shouldUrlDecode() ? decode(exchange, path) : path);
	}

	private String getPathWithinApplication(ServerHttpRequest request) {
		String contextPath = request.getContextPath();
		String path = request.getURI().getRawPath();
		if (!StringUtils.hasText(contextPath)) {
			return path;
		}
		return (path.length() > contextPath.length() ? path.substring(contextPath.length()) : "");
	}

	private String decode(ServerWebExchange exchange, String path) {
		// TODO: look up request encoding?
		try {
			return UriUtils.decode(path, "UTF-8");
		}
		catch (UnsupportedEncodingException ex) {
			// Should not happen
			throw new IllegalStateException("Could not decode request string [" + path + "]");
		}
	}

	/**
	 * Decode the given URI path variables unless {@link #setUrlDecode(boolean)}
	 * is set to {@code true} in which case it is assumed the URL path from
	 * which the variables were extracted is already decoded through a call to
	 * {@link #getLookupPathForRequest(ServerWebExchange)}.
	 * @param exchange current exchange
	 * @param vars URI variables extracted from the URL path
	 * @return the same Map or a new Map instance
	 */
	public Map<String, String> decodePathVariables(ServerWebExchange exchange, Map<String, String> vars) {
		if (this.urlDecode) {
			return vars;
		}
		Map<String, String> decodedVars = new LinkedHashMap<>(vars.size());
		for (Map.Entry<String, String> entry : vars.entrySet()) {
			decodedVars.put(entry.getKey(), decode(exchange, entry.getValue()));
		}
		return decodedVars;
	}

	/**
	 * Decode the given matrix variables unless {@link #setUrlDecode(boolean)}
	 * is set to {@code true} in which case it is assumed the URL path from
	 * which the variables were extracted is already decoded through a call to
	 * {@link #getLookupPathForRequest(ServerWebExchange)}.
	 * @param exchange current exchange
	 * @param vars URI variables extracted from the URL path
	 * @return the same Map or a new Map instance
	 */
	public MultiValueMap<String, String> decodeMatrixVariables(ServerWebExchange exchange,
			MultiValueMap<String, String> vars) {

		if (this.urlDecode) {
			return vars;
		}
		MultiValueMap<String, String> decodedVars = new LinkedMultiValueMap<>(vars.size());
		for (String key : vars.keySet()) {
			for (String value : vars.get(key)) {
				decodedVars.add(key, decode(exchange, value));
			}
		}
		return decodedVars;
	}

}