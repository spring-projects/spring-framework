/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;
import org.springframework.http.server.RequestPath;
import org.springframework.ui.ModelMap;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.ServletRequestPathUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * Test fixture with a {@link InterceptorRegistry}, two {@link HandlerInterceptor}s and two
 * {@link WebRequestInterceptor}s.
 *
 * @author Rossen Stoyanchev
 * @author Eko Kurniawan Khannedy
 */
@SuppressWarnings("deprecation")
public class InterceptorRegistryTests {

	private InterceptorRegistry registry;

	private final HandlerInterceptor interceptor1 = new LocaleChangeInterceptor();

	private final HandlerInterceptor interceptor2 = new LocaleChangeInterceptor();

	private TestWebRequestInterceptor webInterceptor1;

	private TestWebRequestInterceptor webInterceptor2;

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@BeforeEach
	void setUp() {
		this.registry = new InterceptorRegistry();
		this.webInterceptor1 = new TestWebRequestInterceptor();
		this.webInterceptor2 = new TestWebRequestInterceptor();
	}

	@Test
	void addInterceptor() {
		this.registry.addInterceptor(this.interceptor1);
		List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);
		assertThat(interceptors).isEqualTo(List.of(this.interceptor1));
	}

	@Test
	void addTwoInterceptors() {
		this.registry.addInterceptor(this.interceptor1);
		this.registry.addInterceptor(this.interceptor2);
		List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);
		assertThat(interceptors).isEqualTo(Arrays.asList(this.interceptor1, this.interceptor2));
	}

	@Test
	void addInterceptorsWithUrlPatterns() {
		this.registry.addInterceptor(this.interceptor1).addPathPatterns("/path1/**").excludePathPatterns("/path1/secret");
		this.registry.addInterceptor(this.interceptor2).addPathPatterns("/path2");

		assertThat(getInterceptorsForPath("/path1/test")).containsExactly(this.interceptor1);
		assertThat(getInterceptorsForPath("/path2")).containsExactly(this.interceptor2);
		assertThat(getInterceptorsForPath("/path1/secret")).isEmpty();
	}

	@Test
	void addWebRequestInterceptor() throws Exception {
		this.registry.addWebRequestInterceptor(this.webInterceptor1);
		List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);

		assertThat(interceptors).hasSize(1);
		verifyWebInterceptor(interceptors.get(0), this.webInterceptor1);
	}

	@Test
	void addWebRequestInterceptors() throws Exception {
		this.registry.addWebRequestInterceptor(this.webInterceptor1);
		this.registry.addWebRequestInterceptor(this.webInterceptor2);
		List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);

		assertThat(interceptors).hasSize(2);
		verifyWebInterceptor(interceptors.get(0), this.webInterceptor1);
		verifyWebInterceptor(interceptors.get(1), this.webInterceptor2);
	}

	@SuppressWarnings("removal")
	@Test
	void addInterceptorsWithCustomPathMatcher() {
		PathMatcher pathMatcher = mock();
		this.registry.addInterceptor(interceptor1).addPathPatterns("/path1/**").pathMatcher(pathMatcher);

		MappedInterceptor mappedInterceptor = (MappedInterceptor) this.registry.getInterceptors().get(0);
		assertThat(mappedInterceptor.getPathMatcher()).isSameAs(pathMatcher);
	}

	@Test
	void addWebRequestInterceptorsWithUrlPatterns() throws Exception {
		this.registry.addWebRequestInterceptor(this.webInterceptor1).addPathPatterns("/path1");
		this.registry.addWebRequestInterceptor(this.webInterceptor2).addPathPatterns("/path2");

		List<HandlerInterceptor> interceptors = getInterceptorsForPath("/path1");
		assertThat(interceptors).hasSize(1);
		verifyWebInterceptor(interceptors.get(0), this.webInterceptor1);

		interceptors = getInterceptorsForPath("/path2");
		assertThat(interceptors).hasSize(1);
		verifyWebInterceptor(interceptors.get(0), this.webInterceptor2);
	}

	@Test  // SPR-11130
	public void addInterceptorWithExcludePathPatternOnly() {
		this.registry.addInterceptor(this.interceptor1).excludePathPatterns("/path1/secret");
		this.registry.addInterceptor(this.interceptor2).addPathPatterns("/path2");

		assertThat(getInterceptorsForPath("/path1")).isEqualTo(Collections.singletonList(this.interceptor1));
		assertThat(getInterceptorsForPath("/path2")).isEqualTo(Arrays.asList(this.interceptor1, this.interceptor2));
		assertThat(getInterceptorsForPath("/path1/secret")).isEqualTo(Collections.emptyList());
	}

	@Test
	void orderedInterceptors() {
		this.registry.addInterceptor(this.interceptor1).order(Ordered.LOWEST_PRECEDENCE);
		this.registry.addInterceptor(this.interceptor2).order(Ordered.HIGHEST_PRECEDENCE);

		List<Object> interceptors = this.registry.getInterceptors();
		assertThat(interceptors).hasSize(2);

		assertThat(interceptors).element(0).isSameAs(this.interceptor2);
		assertThat(interceptors).element(1).isSameAs(this.interceptor1);
	}

	@Test
	void nonOrderedInterceptors() {
		this.registry.addInterceptor(this.interceptor1).order(0);
		this.registry.addInterceptor(this.interceptor2).order(0);

		List<Object> interceptors = this.registry.getInterceptors();
		assertThat(interceptors).hasSize(2);

		assertThat(interceptors).element(0).isSameAs(this.interceptor1);
		assertThat(interceptors).element(1).isSameAs(this.interceptor2);
	}


	private List<HandlerInterceptor> getInterceptorsForPath(@Nullable String lookupPath) {
		lookupPath = (lookupPath != null ? lookupPath : "");
		this.request.setAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE, RequestPath.parse(lookupPath, null));
		List<HandlerInterceptor> result = new ArrayList<>();
		for (Object interceptor : this.registry.getInterceptors()) {
			if (interceptor instanceof MappedInterceptor mappedInterceptor) {
				if (mappedInterceptor.matches(this.request)) {
					result.add(mappedInterceptor.getInterceptor());
				}
			}
			else if (interceptor instanceof HandlerInterceptor) {
				result.add((HandlerInterceptor) interceptor);
			}
			else {
				fail("Unexpected interceptor type: " + interceptor.getClass().getName());
			}
		}
		return result;
	}

	private void verifyWebInterceptor(HandlerInterceptor interceptor,
			TestWebRequestInterceptor webInterceptor) throws Exception {

		boolean condition = interceptor instanceof WebRequestHandlerInterceptorAdapter;
		assertThat(condition).isTrue();
		interceptor.preHandle(this.request, this.response, null);
		assertThat(webInterceptor.preHandleInvoked).isTrue();
	}


	private static class TestWebRequestInterceptor implements WebRequestInterceptor {

		private boolean preHandleInvoked = false;

		@Override
		public void preHandle(WebRequest request) {
			preHandleInvoked = true;
		}

		@Override
		public void postHandle(WebRequest request, @Nullable ModelMap model) {
		}

		@Override
		public void afterCompletion(WebRequest request, @Nullable Exception ex) {
		}
	}

}
