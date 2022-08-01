/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.handler;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;

import org.springframework.lang.Nullable;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

import static org.junit.jupiter.api.Named.named;

/**
 * Utility methods to help with parameterized tests for URL pattern matching
 * via pre-parsed {@code PathPattern}s or String pattern matching with
 * {@code PathMatcher}.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public abstract class PathPatternsTestUtils {

	public static Stream<Named<Function<String, MockHttpServletRequest>>> requestArguments() {
		return requestArguments(null);
	}

	public static Stream<Named<Function<String, MockHttpServletRequest>>> requestArguments(@Nullable String contextPath) {
		return Stream.of(
				named("ServletRequestPathUtils", path -> {
					MockHttpServletRequest request = createRequest("GET", contextPath, path);
					ServletRequestPathUtils.parseAndCache(request);
					return request;
				}),
				named("UrlPathHelper", path -> {
					MockHttpServletRequest request = createRequest("GET", contextPath, path);
					UrlPathHelper.defaultInstance.resolveAndCacheLookupPath(request);
					return request;
				})
		);
	}

	/**
	 * Create a MockHttpServletRequest and initialize the request attributes for
	 * the lookupPath via {@link ServletRequestPathUtils} or {@link UrlPathHelper}
	 * depending on the setting of the parsedPatterns argument.
	 * <p>At runtime this would be done by the DispatcherServlet (for the RequestPath)
	 * and by the AbstractHandlerMapping (for UrlPathHelper).
	 */
	public static MockHttpServletRequest initRequest(String method, String requestUri, boolean parsedPatterns) {
		return initRequest(method, null, requestUri, parsedPatterns);
	}

	/**
	 * See {@link #initRequest(String, String, boolean)}. This variant adds a contextPath.
	 */
	public static MockHttpServletRequest initRequest(
			String method, @Nullable String contextPath, String path, boolean parsedPatterns) {

		return initRequest(method, contextPath, path, parsedPatterns, null);
	}

	/**
	 * See {@link #initRequest(String, String, boolean)}. This variant adds a contextPath
	 * and a post-construct initializer to apply further changes before the
	 * lookupPath is resolved.
	 */
	public static MockHttpServletRequest initRequest(
			String method, @Nullable String contextPath, String path,
			boolean parsedPatterns, @Nullable Consumer<MockHttpServletRequest> postConstructInitializer) {

		MockHttpServletRequest request = createRequest(method, contextPath, path);
		if (postConstructInitializer != null) {
			postConstructInitializer.accept(request);
		}
		// At runtime this is done by the DispatcherServlet and AbstractHandlerMapping
		if (parsedPatterns) {
			ServletRequestPathUtils.parseAndCache(request);
		}
		else {
			UrlPathHelper.defaultInstance.resolveAndCacheLookupPath(request);
		}
		return request;
	}

	private static MockHttpServletRequest createRequest(String method, @Nullable String contextPath, String path) {
		if (contextPath != null) {
			String requestUri = contextPath + (path.startsWith("/") ? "" : "/") + path;
			MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
			request.setContextPath(contextPath);
			return request;
		}
		else {
			return new MockHttpServletRequest(method, path);
		}
	}

}
