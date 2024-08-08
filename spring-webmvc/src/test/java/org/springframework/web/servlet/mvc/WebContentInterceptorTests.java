/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.handler.PathPatternsParameterizedTest;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link WebContentInterceptor}.
 *
 * @author Rick Evans
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
class WebContentInterceptorTests {

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final WebContentInterceptor interceptor = new WebContentInterceptor();

	private final Object handler = new Object();


	@SuppressWarnings("unused")
	private static Stream<Named<Function<String, MockHttpServletRequest>>> pathPatternsArguments() {
		return PathPatternsTestUtils.requestArguments();
	}


	@PathPatternsParameterizedTest
	void cacheResourcesConfiguration(Function<String, MockHttpServletRequest> requestFactory) throws Exception {
		interceptor.setCacheSeconds(10);
		interceptor.preHandle(requestFactory.apply("/"), response, handler);

		Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders).contains("max-age=10");
	}

	@PathPatternsParameterizedTest
	void mappedCacheConfigurationOverridesGlobal(Function<String, MockHttpServletRequest> requestFactory) throws Exception {
		Properties mappings = new Properties();
		mappings.setProperty("/*/*handle.vm", "-1");

		interceptor.setCacheSeconds(10);
		interceptor.setCacheMappings(mappings);

		MockHttpServletRequest request = requestFactory.apply("/example/adminhandle.vm");
		interceptor.preHandle(request, response, handler);

		Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders).isEmpty();

		request = requestFactory.apply("/example/bingo.html");
		interceptor.preHandle(request, response, handler);

		cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders).contains("max-age=10");
	}

	@PathPatternsParameterizedTest
	void preventCacheConfiguration(Function<String, MockHttpServletRequest> requestFactory) throws Exception {
		interceptor.setCacheSeconds(0);
		interceptor.preHandle(requestFactory.apply("/"), response, handler);

		Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders).contains("no-store");
	}

	@PathPatternsParameterizedTest
	void emptyCacheConfiguration(Function<String, MockHttpServletRequest> requestFactory) throws Exception {
		interceptor.setCacheSeconds(-1);
		interceptor.preHandle(requestFactory.apply("/"), response, handler);

		Iterable<String> expiresHeaders = response.getHeaders("Expires");
		assertThat(expiresHeaders).isEmpty();
		Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
		assertThat(cacheControlHeaders).isEmpty();
	}

	@Test
	void throwsExceptionWithNullPathMatcher() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new WebContentInterceptor().setPathMatcher(null));
	}

}
