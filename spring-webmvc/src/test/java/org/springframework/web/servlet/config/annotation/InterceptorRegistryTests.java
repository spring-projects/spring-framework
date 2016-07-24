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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.theme.ThemeChangeInterceptor;

import static org.junit.Assert.*;

/**
 * Test fixture with a {@link InterceptorRegistry}, two {@link HandlerInterceptor}s and two
 * {@link WebRequestInterceptor}s.
 *
 * @author Rossen Stoyanchev
 */
public class InterceptorRegistryTests {

	private InterceptorRegistry registry;

	private final HandlerInterceptor interceptor1 = new LocaleChangeInterceptor();

	private final HandlerInterceptor interceptor2 = new ThemeChangeInterceptor();

	private TestWebRequestInterceptor webInterceptor1;

	private TestWebRequestInterceptor webInterceptor2;

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Before
	public void setUp() {
		this.registry = new InterceptorRegistry();
		this.webInterceptor1 = new TestWebRequestInterceptor();
		this.webInterceptor2 = new TestWebRequestInterceptor();
	}

	@Test
	public void addInterceptor() {
		this.registry.addInterceptor(this.interceptor1);
		List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);
		assertEquals(Arrays.asList(this.interceptor1), interceptors);
	}

	@Test
	public void addTwoInterceptors() {
		this.registry.addInterceptor(this.interceptor1);
		this.registry.addInterceptor(this.interceptor2);
		List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);
		assertEquals(Arrays.asList(this.interceptor1, this.interceptor2), interceptors);
	}

	@Test
	public void addInterceptorsWithUrlPatterns() {
		this.registry.addInterceptor(this.interceptor1).addPathPatterns("/path1/**").excludePathPatterns("/path1/secret");
		this.registry.addInterceptor(this.interceptor2).addPathPatterns("/path2");

		assertEquals(Arrays.asList(this.interceptor1), getInterceptorsForPath("/path1/test"));
		assertEquals(Arrays.asList(this.interceptor2), getInterceptorsForPath("/path2"));
		assertEquals(Collections.emptyList(), getInterceptorsForPath("/path1/secret"));
	}

	@Test
	public void addWebRequestInterceptor() throws Exception {
		this.registry.addWebRequestInterceptor(this.webInterceptor1);
		List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);

		assertEquals(1, interceptors.size());
		verifyWebInterceptor(interceptors.get(0), this.webInterceptor1);
	}

	@Test
	public void addWebRequestInterceptors() throws Exception {
		this.registry.addWebRequestInterceptor(this.webInterceptor1);
		this.registry.addWebRequestInterceptor(this.webInterceptor2);
		List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);

		assertEquals(2, interceptors.size());
		verifyWebInterceptor(interceptors.get(0), this.webInterceptor1);
		verifyWebInterceptor(interceptors.get(1), this.webInterceptor2);
	}

	@Test
	public void addInterceptorsWithCustomPathMatcher() {
		PathMatcher pathMatcher = Mockito.mock(PathMatcher.class);
		this.registry.addInterceptor(interceptor1).addPathPatterns("/path1/**").pathMatcher(pathMatcher);

		MappedInterceptor mappedInterceptor = (MappedInterceptor) this.registry.getInterceptors().get(0);
		assertSame(pathMatcher, mappedInterceptor.getPathMatcher());
	}

	@Test
	public void addWebRequestInterceptorsWithUrlPatterns() throws Exception {
		this.registry.addWebRequestInterceptor(this.webInterceptor1).addPathPatterns("/path1");
		this.registry.addWebRequestInterceptor(this.webInterceptor2).addPathPatterns("/path2");

		List<HandlerInterceptor> interceptors = getInterceptorsForPath("/path1");
		assertEquals(1, interceptors.size());
		verifyWebInterceptor(interceptors.get(0), this.webInterceptor1);

		interceptors = getInterceptorsForPath("/path2");
		assertEquals(1, interceptors.size());
		verifyWebInterceptor(interceptors.get(0), this.webInterceptor2);
	}

	// SPR-11130

	@Test
	public void addInterceptorWithExcludePathPatternOnly() {
		this.registry.addInterceptor(this.interceptor1).excludePathPatterns("/path1/secret");
		this.registry.addInterceptor(this.interceptor2).addPathPatterns("/path2");

		assertEquals(Arrays.asList(this.interceptor1), getInterceptorsForPath("/path1"));
		assertEquals(Arrays.asList(this.interceptor1, this.interceptor2), getInterceptorsForPath("/path2"));
		assertEquals(Collections.emptyList(), getInterceptorsForPath("/path1/secret"));
	}


	private List<HandlerInterceptor> getInterceptorsForPath(String lookupPath) {
		PathMatcher pathMatcher = new AntPathMatcher();
		List<HandlerInterceptor> result = new ArrayList<>();
		for (Object interceptor : this.registry.getInterceptors()) {
			if (interceptor instanceof MappedInterceptor) {
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(lookupPath, pathMatcher)) {
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

	private void verifyWebInterceptor(HandlerInterceptor interceptor, TestWebRequestInterceptor webInterceptor) throws Exception {
		assertTrue(interceptor instanceof WebRequestHandlerInterceptorAdapter);
		interceptor.preHandle(this.request, this.response, null);
		assertTrue(webInterceptor.preHandleInvoked);
	}

	private static class TestWebRequestInterceptor implements WebRequestInterceptor {

		private boolean preHandleInvoked = false;

		@Override
		public void preHandle(WebRequest request) throws Exception {
			preHandleInvoked = true;
		}

		@Override
		public void postHandle(WebRequest request, ModelMap model) throws Exception {
		}

		@Override
		public void afterCompletion(WebRequest request, Exception ex) throws Exception {
		}
	}

}
