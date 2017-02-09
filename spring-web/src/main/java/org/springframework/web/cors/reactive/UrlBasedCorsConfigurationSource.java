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

package org.springframework.web.cors.reactive;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.support.HttpRequestPathHelper;
import org.springframework.web.util.patterns.PathPattern;
import org.springframework.web.util.patterns.PathPatternRegistry;

/**
 * Provide a per reactive request {@link CorsConfiguration} instance based on a
 * collection of {@link CorsConfiguration} mapped on path patterns.
 *
 * <p>Exact path mapping URIs (such as {@code "/admin"}) are supported
 * as well as Ant-style path patterns (such as {@code "/admin/**"}).
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource {

	private final PathPatternRegistry patternRegistry = new PathPatternRegistry();

	private final Map<PathPattern, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();

	private HttpRequestPathHelper pathHelper = new HttpRequestPathHelper();


	/**
	 * Set if context path and request URI should be URL-decoded. Both are returned
	 * <i>undecoded</i> by the Servlet API, in contrast to the servlet path.
	 * <p>Uses either the request encoding or the default encoding according
	 * to the Servlet spec (ISO-8859-1).
	 * @see HttpRequestPathHelper#setUrlDecode
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.pathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths.
	 * <p>Use this to override the default UrlPathHelper with a custom subclass.
	 */
	public void setHttpRequestPathHelper(HttpRequestPathHelper pathHelper) {
		Assert.notNull(pathHelper, "HttpRequestPathHelper must not be null");
		this.pathHelper = pathHelper;
	}

	/**
	 * Set CORS configuration based on URL patterns.
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		this.patternRegistry.clear();
		this.corsConfigurations.clear();
		if (corsConfigurations != null) {
			corsConfigurations.forEach((pattern, config) -> {
				List<PathPattern> registered = this.patternRegistry.register(pattern);
				registered.forEach(p -> this.corsConfigurations.put(p, config));
			});
		}
	}

	/**
	 * Get the CORS configuration.
	 */
	public Map<PathPattern, CorsConfiguration> getCorsConfigurations() {
		return Collections.unmodifiableMap(this.corsConfigurations);
	}

	/**
	 * Register a {@link CorsConfiguration} for the specified path pattern.
	 */
	public void registerCorsConfiguration(String path, CorsConfiguration config) {
		this.patternRegistry
				.register(path)
				.forEach(pattern -> this.corsConfigurations.put(pattern, config));
	}


	@Override
	public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
		String lookupPath = this.pathHelper.getLookupPathForRequest(exchange);
		SortedSet<PathPattern> matches = this.patternRegistry.findMatches(lookupPath);
		if(!matches.isEmpty()) {
			return this.corsConfigurations.get(matches.first());
		}
		return null;
	}

}
