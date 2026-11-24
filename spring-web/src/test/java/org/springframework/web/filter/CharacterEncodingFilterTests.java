/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.filter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockFilterConfig;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.WebUtils;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Vedran Pavic
 */
class CharacterEncodingFilterTests {

	private static final String FILTER_NAME = "boot";

	private static final Charset CHARSET = StandardCharsets.UTF_8;


	@Test
	void forceEncodingAlwaysSetsEncoding() throws Exception {
		HttpServletRequest request = mock();
		request.setCharacterEncoding(CHARSET);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
		given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

		HttpServletResponse response = mock();
		FilterChain filterChain = mock();

		CharacterEncodingFilter filter = new CharacterEncodingFilter(CHARSET);
		filter.setForceEncoding(true);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(response).setCharacterEncoding(CHARSET);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void encodingIfEmptyAndNotForced() throws Exception {
		HttpServletRequest request = mock();
		given(request.getCharacterEncoding()).willReturn(null);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
		given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = mock();

		CharacterEncodingFilter filter = new CharacterEncodingFilter(CHARSET);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request).setCharacterEncoding(CHARSET);
		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doesNotIfEncodingIsNotEmptyAndNotForced() throws Exception {
		HttpServletRequest request = mock();
		given(request.getCharacterEncoding()).willReturn(CHARSET.name());
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
		given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = mock();

		CharacterEncodingFilter filter = new CharacterEncodingFilter(CHARSET);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void withBeanInitialization() throws Exception {
		HttpServletRequest request = mock();
		given(request.getCharacterEncoding()).willReturn(null);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
		given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = mock();

		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setCharset(CHARSET);
		filter.setBeanName(FILTER_NAME);
		filter.setServletContext(new MockServletContext());
		filter.doFilter(request, response, filterChain);

		verify(request).setCharacterEncoding(CHARSET);
		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void withIncompleteInitialization() throws Exception {
		HttpServletRequest request = mock();
		given(request.getCharacterEncoding()).willReturn(null);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(CharacterEncodingFilter.class.getName()))).willReturn(null);
		given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = mock();

		CharacterEncodingFilter filter = new CharacterEncodingFilter(CHARSET);
		filter.doFilter(request, response, filterChain);

		verify(request).setCharacterEncoding(CHARSET);
		verify(request).setAttribute(filteredName(CharacterEncodingFilter.class.getName()), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(CharacterEncodingFilter.class.getName()));
		verify(filterChain).doFilter(request, response);
	}

	@Test  // SPR-14240
	void setForceEncodingOnRequestOnly() throws Exception {
		HttpServletRequest request = mock();
		request.setCharacterEncoding(CHARSET);
		given(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getAttribute(filteredName(FILTER_NAME))).willReturn(null);
		given(request.getDispatcherType()).willReturn(DispatcherType.REQUEST);

		HttpServletResponse response = mock();
		FilterChain filterChain = mock();

		CharacterEncodingFilter filter = new CharacterEncodingFilter(CHARSET.name(), true, false);
		filter.init(new MockFilterConfig(FILTER_NAME));
		filter.doFilter(request, response, filterChain);

		verify(request).setAttribute(filteredName(FILTER_NAME), Boolean.TRUE);
		verify(request).removeAttribute(filteredName(FILTER_NAME));
		verify(request, times(2)).setCharacterEncoding(CHARSET);
		verify(response, never()).setCharacterEncoding(CHARSET);
		verify(filterChain).doFilter(request, response);
	}

	private String filteredName(String prefix) {
		return prefix + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX;
	}

}
