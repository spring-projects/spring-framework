/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.theme.ThemeChangeInterceptor;

/**
 * Test fixture with a {@link InterceptorConfigurer}, two {@link HandlerInterceptor}s and two
 * {@link WebRequestInterceptor}s.
 *
 * @author Rossen Stoyanchev
 */
public class InterceptorConfigurerTests {

	private InterceptorConfigurer configurer;

	private final HandlerInterceptor interceptor1 = new LocaleChangeInterceptor();

	private final HandlerInterceptor interceptor2 = new ThemeChangeInterceptor();

	private TestWebRequestInterceptor webRequestInterceptor1;

	private TestWebRequestInterceptor webRequestInterceptor2;

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	@Before
	public void setUp() {
		configurer = new InterceptorConfigurer();
		webRequestInterceptor1 = new TestWebRequestInterceptor();
		webRequestInterceptor2 = new TestWebRequestInterceptor();
	}

	@Test
	public void addInterceptor() {
		configurer.addInterceptor(interceptor1);
		HandlerInterceptor[] interceptors = getInterceptorsForPath(null);
		assertArrayEquals(new HandlerInterceptor[] {interceptor1}, interceptors);
	}

	@Test
	public void addInterceptors() {
		configurer.addInterceptors(interceptor1, interceptor2);
		HandlerInterceptor[] interceptors = getInterceptorsForPath(null);
		assertArrayEquals(new HandlerInterceptor[] {interceptor1, interceptor2}, interceptors);
	}

	@Test
	public void mapInterceptor() {
		configurer.mapInterceptor(new String[] {"/path1"}, interceptor1);
		configurer.mapInterceptor(new String[] {"/path2"}, interceptor2);

		assertArrayEquals(new HandlerInterceptor[] {interceptor1}, getInterceptorsForPath("/path1"));
		assertArrayEquals(new HandlerInterceptor[] {interceptor2}, getInterceptorsForPath("/path2"));
	}

	@Test
	public void mapInterceptors() {
		configurer.mapInterceptors(new String[] {"/path1"}, interceptor1, interceptor2);

		assertArrayEquals(new HandlerInterceptor[] {interceptor1, interceptor2}, getInterceptorsForPath("/path1"));
		assertArrayEquals(new HandlerInterceptor[] {}, getInterceptorsForPath("/path2"));
	}

	@Test
	public void addWebRequestInterceptor() throws Exception {
		configurer.addInterceptor(webRequestInterceptor1);
		HandlerInterceptor[] interceptors = getInterceptorsForPath(null);

		assertEquals(1, interceptors.length);
		verifyAdaptedInterceptor(interceptors[0], webRequestInterceptor1);
	}

	@Test
	public void addWebRequestInterceptors() throws Exception {
		configurer.addInterceptors(webRequestInterceptor1, webRequestInterceptor2);
		HandlerInterceptor[] interceptors = getInterceptorsForPath(null);

		assertEquals(2, interceptors.length);
		verifyAdaptedInterceptor(interceptors[0], webRequestInterceptor1);
		verifyAdaptedInterceptor(interceptors[1], webRequestInterceptor2);
	}

	@Test
	public void mapWebRequestInterceptor() throws Exception {
		configurer.mapInterceptor(new String[] {"/path1"}, webRequestInterceptor1);
		configurer.mapInterceptor(new String[] {"/path2"}, webRequestInterceptor2);

		HandlerInterceptor[] interceptors = getInterceptorsForPath("/path1");
		assertEquals(1, interceptors.length);
		verifyAdaptedInterceptor(interceptors[0], webRequestInterceptor1);

		interceptors = getInterceptorsForPath("/path2");
		assertEquals(1, interceptors.length);
		verifyAdaptedInterceptor(interceptors[0], webRequestInterceptor2);
	}

	@Test
	public void mapWebRequestInterceptor2() throws Exception {
		configurer.mapInterceptors(new String[] {"/path1"}, webRequestInterceptor1, webRequestInterceptor2);

		HandlerInterceptor[] interceptors = getInterceptorsForPath("/path1");
		assertEquals(2, interceptors.length);
		verifyAdaptedInterceptor(interceptors[0], webRequestInterceptor1);
		verifyAdaptedInterceptor(interceptors[1], webRequestInterceptor2);

		assertEquals(0, getInterceptorsForPath("/path2").length);
	}

	private HandlerInterceptor[] getInterceptorsForPath(String lookupPath) {
		return configurer.getMappedInterceptors().getInterceptors(lookupPath, new AntPathMatcher());
	}

	private void verifyAdaptedInterceptor(HandlerInterceptor interceptor, TestWebRequestInterceptor webInterceptor)
			throws Exception {
		assertTrue(interceptor instanceof WebRequestHandlerInterceptorAdapter);
		interceptor.preHandle(request, response, null);
		assertTrue(webInterceptor.preHandleInvoked);
	}

	private static class TestWebRequestInterceptor implements WebRequestInterceptor {

		private boolean preHandleInvoked = false;

		public void preHandle(WebRequest request) throws Exception {
			preHandleInvoked = true;
		}

		public void postHandle(WebRequest request, ModelMap model) throws Exception {
		}

		public void afterCompletion(WebRequest request, Exception ex) throws Exception {
		}

	}

}
