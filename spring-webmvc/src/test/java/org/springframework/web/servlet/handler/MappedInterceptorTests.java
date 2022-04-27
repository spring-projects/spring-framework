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

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;

import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link MappedInterceptor} tests.
 * @author Rossen Stoyanchev
 */
class MappedInterceptorTests {

	private static final LocaleChangeInterceptor delegate = new LocaleChangeInterceptor();


	@SuppressWarnings("unused")
	private static Stream<Named<Function<String, MockHttpServletRequest>>> pathPatternsArguments() {
		return PathPatternsTestUtils.requestArguments();
	}


	@PathPatternsParameterizedTest
	void noPatterns(Function<String, MockHttpServletRequest> requestFactory) {
		MappedInterceptor interceptor = new MappedInterceptor(null, null, delegate);
		assertThat(interceptor.matches(requestFactory.apply("/foo"))).isTrue();
	}

	@PathPatternsParameterizedTest
	void includePattern(Function<String, MockHttpServletRequest> requestFactory) {
		MappedInterceptor interceptor = new MappedInterceptor(new String[] { "/foo/*" }, null, delegate);

		assertThat(interceptor.matches(requestFactory.apply("/foo/bar"))).isTrue();
		assertThat(interceptor.matches(requestFactory.apply("/bar/foo"))).isFalse();
	}

	@PathPatternsParameterizedTest
	void includePatternWithMatrixVariables(Function<String, MockHttpServletRequest> requestFactory) {
		MappedInterceptor interceptor = new MappedInterceptor(new String[] { "/foo*/*" }, null, delegate);
		assertThat(interceptor.matches(requestFactory.apply("/foo;q=1/bar;s=2"))).isTrue();
	}

	@PathPatternsParameterizedTest
	void excludePattern(Function<String, MockHttpServletRequest> requestFactory) {
		MappedInterceptor interceptor = new MappedInterceptor(null, new String[] { "/admin/**" }, delegate);

		assertThat(interceptor.matches(requestFactory.apply("/foo"))).isTrue();
		assertThat(interceptor.matches(requestFactory.apply("/admin/foo"))).isFalse();
	}

	@PathPatternsParameterizedTest
	void includeAndExcludePatterns(Function<String, MockHttpServletRequest> requestFactory) {
		MappedInterceptor interceptor =
				new MappedInterceptor(new String[] { "/**" }, new String[] { "/admin/**" }, delegate);

		assertThat(interceptor.matches(requestFactory.apply("/foo"))).isTrue();
		assertThat(interceptor.matches(requestFactory.apply("/admin/foo"))).isFalse();
	}

	@PathPatternsParameterizedTest // gh-26690
	void includePatternWithFallbackOnPathMatcher(Function<String, MockHttpServletRequest> requestFactory) {
		MappedInterceptor interceptor = new MappedInterceptor(new String[] { "/path1/**/path2" }, null, delegate);

		assertThat(interceptor.matches(requestFactory.apply("/path1/foo/bar/path2"))).isTrue();
		assertThat(interceptor.matches(requestFactory.apply("/path1/foo/bar/path3"))).isFalse();
		assertThat(interceptor.matches(requestFactory.apply("/path3/foo/bar/path2"))).isFalse();
	}

	@PathPatternsParameterizedTest
	void customPathMatcher(Function<String, MockHttpServletRequest> requestFactory) {
		MappedInterceptor interceptor = new MappedInterceptor(new String[] { "/foo/[0-9]*" }, null, delegate);
		interceptor.setPathMatcher(new TestPathMatcher());

		assertThat(interceptor.matches(requestFactory.apply("/foo/123"))).isTrue();
		assertThat(interceptor.matches(requestFactory.apply("/foo/bar"))).isFalse();
	}

	@Test
	void preHandle() throws Exception {
		HandlerInterceptor delegate = mock(HandlerInterceptor.class);

		new MappedInterceptor(null, delegate).preHandle(
				mock(HttpServletRequest.class), mock(HttpServletResponse.class), null);

		then(delegate).should().preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any());
	}

	@Test
	void postHandle() throws Exception {
		HandlerInterceptor delegate = mock(HandlerInterceptor.class);

		new MappedInterceptor(null, delegate).postHandle(
				mock(HttpServletRequest.class), mock(HttpServletResponse.class), null, mock(ModelAndView.class));

		then(delegate).should().postHandle(any(), any(), any(), any());
	}

	@Test
	void afterCompletion() throws Exception {
		HandlerInterceptor delegate = mock(HandlerInterceptor.class);

		new MappedInterceptor(null, delegate).afterCompletion(
				mock(HttpServletRequest.class), mock(HttpServletResponse.class), null, mock(Exception.class));

		then(delegate).should().afterCompletion(any(), any(), any(), any());
	}


	public static class TestPathMatcher implements PathMatcher {

		@Override
		public boolean isPattern(String path) {
			return false;
		}

		@Override
		public boolean match(String pattern, String path) {
			return path.matches(pattern);
		}

		@Override
		public boolean matchStart(String pattern, String path) {
			return false;
		}

		@Override
		public String extractPathWithinPattern(String pattern, String path) {
			return null;
		}

		@Override
		public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
			return null;
		}

		@Override
		public Comparator<String> getPatternComparator(String path) {
			return null;
		}

		@Override
		public String combine(String pattern1, String pattern2) {
			return null;
		}
	}
}
