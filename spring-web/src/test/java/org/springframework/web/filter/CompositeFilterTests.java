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

package org.springframework.web.filter;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.web.testfixture.servlet.MockFilterConfig;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Dave Syer
 */
class CompositeFilterTests {

	@Test
	void compositeFilter() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();
		MockFilter targetFilter = new MockFilter();
		MockFilterConfig proxyConfig = new MockFilterConfig(sc);

		CompositeFilter filterProxy = new CompositeFilter();
		filterProxy.setFilters(List.of(targetFilter));
		filterProxy.init(proxyConfig);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertThat(targetFilter.filterConfig).isNotNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void destroyInvokesFiltersInReverseOrder() {
		Filter firstFilter = mock();
		Filter secondFilter = mock();
		Filter thirdFilter = mock();

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, secondFilter, thirdFilter));

		compositeFilter.destroy();

		InOrder inOrder = inOrder(firstFilter, secondFilter, thirdFilter);
		inOrder.verify(thirdFilter).destroy();
		inOrder.verify(secondFilter).destroy();
		inOrder.verify(firstFilter).destroy();
	}

	@Test
	void destroyInvokesRemainingFilterAfterRuntimeException() {
		Filter remainingFilter = mock();
		Filter failingFilter = mock();

		RuntimeException failingFilterException = new RuntimeException();
		doThrow(failingFilterException).when(failingFilter).destroy();

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(remainingFilter, failingFilter));

		assertThatThrownBy(compositeFilter::destroy).isSameAs(failingFilterException);
		verify(remainingFilter).destroy();
	}

	@Test
	void destroyAddsLaterRuntimeExceptionsAsSuppressed() {
		Filter firstFilter = mock();
		Filter secondFilter = mock();
		Filter thirdFilter = mock();

		RuntimeException firstFilterException = new RuntimeException();
		RuntimeException secondFilterException = new RuntimeException();
		RuntimeException thirdFilterException = new RuntimeException();
		doThrow(firstFilterException).when(firstFilter).destroy();
		doThrow(secondFilterException).when(secondFilter).destroy();
		doThrow(thirdFilterException).when(thirdFilter).destroy();

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, secondFilter, thirdFilter));

		assertThatThrownBy(compositeFilter::destroy).isSameAs(thirdFilterException);
		assertThat(thirdFilterException.getSuppressed()).containsExactly(secondFilterException, firstFilterException);
	}

	@Test
	void destroyContinuesWhenSameExceptionInstanceIsThrown() {
		Filter firstFilter = mock();
		Filter secondFilter = mock();
		Filter thirdFilter = mock();

		RuntimeException sharedException = new RuntimeException();
		doThrow(sharedException).when(secondFilter).destroy();
		doThrow(sharedException).when(thirdFilter).destroy();

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, secondFilter, thirdFilter));

		assertThatThrownBy(compositeFilter::destroy).isSameAs(sharedException);
		assertThat(sharedException.getSuppressed()).isEmpty();
		verify(firstFilter).destroy();
		verify(secondFilter).destroy();
		verify(thirdFilter).destroy();
	}

	@Test
	void destroySuppressesErrorAfterRuntimeException() {
		Filter firstFilter = mock();
		Filter secondFilter = mock();
		Filter thirdFilter = mock();

		RuntimeException thirdFilterException = new RuntimeException();
		Error secondFilterError = new Error();
		doThrow(secondFilterError).when(secondFilter).destroy();
		doThrow(thirdFilterException).when(thirdFilter).destroy();

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, secondFilter, thirdFilter));

		assertThatThrownBy(compositeFilter::destroy).isSameAs(thirdFilterException);
		assertThat(thirdFilterException.getSuppressed()).containsExactly(secondFilterError);
		verify(firstFilter).destroy();
	}

	@Test
	void destroyContinuesAfterErrorAndRethrowsIt() {
		Filter firstFilter = mock();
		Filter secondFilter = mock();

		Error secondFilterError = new Error();
		RuntimeException firstFilterException = new RuntimeException();
		doThrow(firstFilterException).when(firstFilter).destroy();
		doThrow(secondFilterError).when(secondFilter).destroy();

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, secondFilter));

		assertThatThrownBy(compositeFilter::destroy).isSameAs(secondFilterError);
		assertThat(secondFilterError.getSuppressed()).containsExactly(firstFilterException);
	}


	public static class MockFilter implements Filter {

		public FilterConfig filterConfig;

		@Override
		public void init(FilterConfig filterConfig) {
			this.filterConfig = filterConfig;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
			request.setAttribute("called", Boolean.TRUE);
		}

		@Override
		public void destroy() {
			this.filterConfig = null;
		}
	}

}
