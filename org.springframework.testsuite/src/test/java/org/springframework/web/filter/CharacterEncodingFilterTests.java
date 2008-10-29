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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class CharacterEncodingFilterTests extends TestCase {

	private static final String FILTER_NAME = "boot";

	private static final String ENCODING = "UTF-8";


	public void testForceAlwaysSetsEncoding() throws Exception {
		MockControl mockRequest = MockControl.createControl(HttpServletRequest.class);
		HttpServletRequest request = (HttpServletRequest) mockRequest.getMock();
		request.setCharacterEncoding(ENCODING);
		mockRequest.setVoidCallable();
		request.getAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setReturnValue(null);
		request.setAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		mockRequest.setVoidCallable();
		request.removeAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setVoidCallable();
		mockRequest.replay();

		MockControl mockResponse = MockControl.createControl(HttpServletResponse.class);
		HttpServletResponse response = (HttpServletResponse) mockResponse.getMock();
		response.setCharacterEncoding(ENCODING);
		mockResponse.setVoidCallable();
		mockResponse.replay();

		MockControl mockFilter = MockControl.createControl(FilterChain.class);
		FilterChain filterChain = (FilterChain) mockFilter.getMock();
		filterChain.doFilter(request, response);
		mockFilter.replay();

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setForceEncoding(true);
		filter.setEncoding(ENCODING);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		mockRequest.verify();
		mockResponse.verify();
		mockFilter.verify();
	}

	public void testEncodingIfEmptyAndNotForced() throws Exception {
		MockControl mockRequest = MockControl.createControl(HttpServletRequest.class);
		HttpServletRequest request = (HttpServletRequest) mockRequest.getMock();
		request.getCharacterEncoding();
		mockRequest.setReturnValue(null);
		request.setCharacterEncoding(ENCODING);
		mockRequest.setVoidCallable();
		request.getAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setReturnValue(null);
		request.setAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		mockRequest.setVoidCallable();
		request.removeAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setVoidCallable();
		mockRequest.replay();

		MockHttpServletResponse response = new MockHttpServletResponse();

		MockControl mockFilter = MockControl.createControl(FilterChain.class);
		FilterChain filterChain = (FilterChain) mockFilter.getMock();
		filterChain.doFilter(request, response);
		mockFilter.replay();

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setForceEncoding(false);
		filter.setEncoding(ENCODING);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		mockRequest.verify();
		mockFilter.verify();
	}

	public void testDoesNowtIfEncodingIsNotEmptyAndNotForced() throws Exception {
		MockControl mockRequest = MockControl.createControl(HttpServletRequest.class);
		HttpServletRequest request = (HttpServletRequest) mockRequest.getMock();
		request.getCharacterEncoding();
		mockRequest.setReturnValue(ENCODING);
		request.getAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setReturnValue(null);
		request.setAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		mockRequest.setVoidCallable();
		request.removeAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setVoidCallable();
		mockRequest.replay();

		MockHttpServletResponse response = new MockHttpServletResponse();

		MockControl mockFilter = MockControl.createControl(FilterChain.class);
		FilterChain filterChain = (FilterChain) mockFilter.getMock();
		filterChain.doFilter(request, response);
		mockFilter.replay();

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(ENCODING);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		mockRequest.verify();
		mockFilter.verify();
	}

	public void testWithBeanInitialization() throws Exception {
		MockControl mockRequest = MockControl.createControl(HttpServletRequest.class);
		HttpServletRequest request = (HttpServletRequest) mockRequest.getMock();
		request.getCharacterEncoding();
		mockRequest.setReturnValue(null);
		request.setCharacterEncoding(ENCODING);
		mockRequest.setVoidCallable();
		request.getAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setReturnValue(null);
		request.setAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		mockRequest.setVoidCallable();
		request.removeAttribute(FILTER_NAME + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setVoidCallable();
		mockRequest.replay();

		MockHttpServletResponse response = new MockHttpServletResponse();

		MockControl mockFilter = MockControl.createControl(FilterChain.class);
		FilterChain filterChain = (FilterChain) mockFilter.getMock();
		filterChain.doFilter(request, response);
		mockFilter.replay();

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(ENCODING);
		filter.setBeanName(FILTER_NAME);
		filter.setServletContext(new MockServletContext());
		filter.doFilter(request, response, filterChain);

		mockRequest.verify();
		mockFilter.verify();
	}

	public void testWithIncompleteInitialization() throws Exception {
		MockControl mockRequest = MockControl.createControl(HttpServletRequest.class);
		HttpServletRequest request = (HttpServletRequest) mockRequest.getMock();
		request.getCharacterEncoding();
		mockRequest.setReturnValue(null);
		request.setCharacterEncoding(ENCODING);
		mockRequest.setVoidCallable();
		request.getAttribute(CharacterEncodingFilter.class.getName() + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setReturnValue(null);
		request.setAttribute(CharacterEncodingFilter.class.getName() + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		mockRequest.setVoidCallable();
		request.removeAttribute(CharacterEncodingFilter.class.getName() + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
		mockRequest.setVoidCallable();
		mockRequest.replay();

		MockHttpServletResponse response = new MockHttpServletResponse();

		MockControl mockFilter = MockControl.createControl(FilterChain.class);
		FilterChain filterChain = (FilterChain) mockFilter.getMock();
		filterChain.doFilter(request, response);
		mockFilter.replay();

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(ENCODING);
		filter.doFilter(request, response, filterChain);

		mockRequest.verify();
		mockFilter.verify();
	}

}
