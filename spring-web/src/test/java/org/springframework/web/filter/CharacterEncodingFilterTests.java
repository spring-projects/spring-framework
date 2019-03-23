/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.filter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.mock.web.test.MockFilterConfig;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.util.WebUtils;

import static org.mockito.BDDMockito.*;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Vedran Pavic
 */
public class CharacterEncodingFilterTests {

	private static final String FILTER_NAME = "boot";

	private static final String ENCODING = "UTF-8";


	@Test
	public void forceEncodingAlwaysSetsEncoding() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		request.setCharacterEncoding(ENCODING);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);

		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);

		CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING, true);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(response).setCharacterEncoding(ENCODING);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	public void encodingIfEmptyAndNotForced() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		given(request.getCharacterEncoding()).willReturn(null);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = mock(FilterChain.class);

		CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request).setCharacterEncoding(ENCODING);
		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(filterChain).doFilter(request, response);
	}

	@Test
	public void doesNotIfEncodingIsNotEmptyAndNotForced() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		given(request.getCharacterEncoding()).willReturn(ENCODING);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = mock(FilterChain.class);

		CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(filterChain).doFilter(request, response);
	}

	@Test
	public void withBeanInitialization() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		given(request.getCharacterEncoding()).willReturn(null);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = mock(FilterChain.class);

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(ENCODING);
		filter.setBeanName(FILTER_NAME);
		filter.setServletContext(new MockServletContext());
		filter.doFilter(request, response, filterChain);

		verify(request).setCharacterEncoding(ENCODING);
		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(filterChain).doFilter(request, response);
	}

	@Test
	public void withIncompleteInitialization() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		given(request.getCharacterEncoding()).willReturn(null);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(CharacterEncodingFilter.class.getName()))).willReturn(null);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = mock(FilterChain.class);

		CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING);
		filter.doFilter(request, response, filterChain);

		verify(request).setCharacterEncoding(ENCODING);
		verify(request).setAttribute(filteredName(CharacterEncodingFilter.class.getName()), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(CharacterEncodingFilter.class.getName()));
		verify(filterChain).doFilter(request, response);
	}

	// SPR-14240
	@Test
	public void setForceEncodingOnRequestOnly() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		request.setCharacterEncoding(ENCODING);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);

		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);

		CharacterEncodingFilter filter = new CharacterEncodingFilter(ENCODING, true, false);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(request, times(2)).setCharacterEncoding(ENCODING);
		verify(response, never()).setCharacterEncoding(ENCODING);
		verify(filterChain).doFilter(request, response);
	}

	private String filteredName(String prefix) {
		return prefix + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX;
	}

}
