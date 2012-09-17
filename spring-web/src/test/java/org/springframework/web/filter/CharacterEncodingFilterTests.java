/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.filter;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.request.async.WebAsyncUtils;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class CharacterEncodingFilterTests extends TestCase {

	private static final String FILTER_NAME = "boot";

	private static final String ENCODING = "UTF-8";


	public void testForceAlwaysSetsEncoding() throws Exception {
		HttpServletRequest request = createMock(HttpServletRequest.class);
		addAsyncManagerExpectations(request);
		request.setCharacterEncoding(ENCODING);
		expect(request.getAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX)).andReturn(null);
		request.setAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		request.removeAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		replay(request);

		HttpServletResponse response = createMock(HttpServletResponse.class);
		response.setCharacterEncoding(ENCODING);
		replay(response);

		FilterChain filterChain = createMock(FilterChain.class);
		filterChain.doFilter(request, response);
		replay(filterChain);

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setForceEncoding(true);
		filter.setEncoding(ENCODING);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request);
		verify(response);
		verify(filterChain);
	}

	public void testEncodingIfEmptyAndNotForced() throws Exception {
		HttpServletRequest request = createMock(HttpServletRequest.class);
		addAsyncManagerExpectations(request);
		expect(request.getCharacterEncoding()).andReturn(null);
		request.setCharacterEncoding(ENCODING);
		expect(request.getAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX)).andReturn(null);
		request.setAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		request.removeAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		replay(request);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = createMock(FilterChain.class);
		filterChain.doFilter(request, response);
		replay(filterChain);

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setForceEncoding(false);
		filter.setEncoding(ENCODING);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request);
		verify(filterChain);
	}

	public void testDoesNowtIfEncodingIsNotEmptyAndNotForced() throws Exception {
		HttpServletRequest request = createMock(HttpServletRequest.class);
		addAsyncManagerExpectations(request);
		expect(request.getCharacterEncoding()).andReturn(ENCODING);
		expect(request.getAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX)).andReturn(null);
		request.setAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		request.removeAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		replay(request);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = createMock(FilterChain.class);
		filterChain.doFilter(request, response);
		replay(filterChain);

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(ENCODING);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request);
		verify(filterChain);
	}

	public void testWithBeanInitialization() throws Exception {
		HttpServletRequest request = createMock(HttpServletRequest.class);
		addAsyncManagerExpectations(request);
		expect(request.getCharacterEncoding()).andReturn(null);
		request.setCharacterEncoding(ENCODING);
		expect(request.getAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX)).andReturn(null);
		request.setAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		request.removeAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		replay(request);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = createMock(FilterChain.class);
		filterChain.doFilter(request, response);
		replay(filterChain);

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(ENCODING);
		filter.setBeanName(FILTER_NAME);
		filter.setServletContext(new MockServletContext());
		filter.doFilter(request, response, filterChain);

		verify(request);
		verify(filterChain);
	}

	public void testWithIncompleteInitialization() throws Exception {
		HttpServletRequest request = createMock(HttpServletRequest.class);
		addAsyncManagerExpectations(request);
		expect(request.getCharacterEncoding()).andReturn(null);
		request.setCharacterEncoding(ENCODING);
		expect(request.getAttribute(CharacterEncodingFilter.class.getName() + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX)).andReturn(null);
		request.setAttribute(CharacterEncodingFilter.class.getName() + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		request.removeAttribute(CharacterEncodingFilter.class.getName() + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		replay(request);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = createMock(FilterChain.class);
		filterChain.doFilter(request, response);
		replay(filterChain);

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(ENCODING);
		filter.doFilter(request, response, filterChain);

		verify(request);
		verify(filterChain);
	}


	private void addAsyncManagerExpectations(HttpServletRequest request) {
		expect(request.getAttribute(WebAsyncUtils.WEB_ASYNC_MANAGER_ATTRIBUTE)).andReturn(null);
		expectLastCall().anyTimes();
		request.setAttribute(same(WebAsyncUtils.WEB_ASYNC_MANAGER_ATTRIBUTE), notNull());
		expectLastCall().anyTimes();
	}
}
