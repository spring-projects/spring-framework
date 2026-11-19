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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CompositeFilter}.
 *
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

	@Test  // gh-37445
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

	@Test  // gh-37445
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

	@Test  // gh-37445
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

	@Test  // gh-37445
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

	@Test  // gh-37445
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

	@Test  // gh-37445
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


	@Test
	void initInvokesFiltersInOrder() throws ServletException {
		Filter firstFilter = mock();
		Filter secondFilter = mock();
		Filter thirdFilter = mock();
		FilterConfig filterConfig = mock();

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, secondFilter, thirdFilter));

		compositeFilter.init(filterConfig);

		InOrder inOrder = inOrder(firstFilter, secondFilter, thirdFilter);
		inOrder.verify(firstFilter).init(filterConfig);
		inOrder.verify(secondFilter).init(filterConfig);
		inOrder.verify(thirdFilter).init(filterConfig);
	}

	@Test
	void initDestroysEarlierFiltersAfterServletException() throws ServletException {
		Filter firstFilter = mock();
		Filter failingFilter = mock();
		Filter laterFilter = mock();
		FilterConfig filterConfig = mock();

		ServletException failingFilterException = new ServletException();
		doThrow(failingFilterException).when(failingFilter).init(filterConfig);

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, failingFilter, laterFilter));

		assertThatThrownBy(() -> compositeFilter.init(filterConfig)).isSameAs(failingFilterException);
		verify(firstFilter).init(filterConfig);
		verify(firstFilter).destroy();
		verify(failingFilter, never()).destroy();
		verify(laterFilter, never()).init(filterConfig);
		verify(laterFilter, never()).destroy();
	}

	@Test
	void initDestroysEarlierFiltersInReverseOrder() throws ServletException {
		Filter firstFilter = mock();
		Filter secondFilter = mock();
		Filter failingFilter = mock();
		FilterConfig filterConfig = mock();

		ServletException failingFilterException = new ServletException();
		doThrow(failingFilterException).when(failingFilter).init(filterConfig);

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, secondFilter, failingFilter));

		assertThatThrownBy(() -> compositeFilter.init(filterConfig)).isSameAs(failingFilterException);

		InOrder inOrder = inOrder(firstFilter, secondFilter, failingFilter);
		inOrder.verify(firstFilter).init(filterConfig);
		inOrder.verify(secondFilter).init(filterConfig);
		inOrder.verify(failingFilter).init(filterConfig);
		inOrder.verify(secondFilter).destroy();
		inOrder.verify(firstFilter).destroy();
	}

	@Test
	void initAddsDestroyFailuresAsSuppressed() throws ServletException {
		Filter firstFilter = mock();
		Filter secondFilter = mock();
		Filter failingFilter = mock();
		FilterConfig filterConfig = mock();

		ServletException failingFilterException = new ServletException();
		RuntimeException secondDestroyException = new RuntimeException();
		Error firstDestroyError = new Error();
		doThrow(failingFilterException).when(failingFilter).init(filterConfig);
		doThrow(secondDestroyException).when(secondFilter).destroy();
		doThrow(firstDestroyError).when(firstFilter).destroy();

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, secondFilter, failingFilter));

		assertThatThrownBy(() -> compositeFilter.init(filterConfig)).isSameAs(failingFilterException);
		assertThat(failingFilterException.getSuppressed())
				.containsExactly(secondDestroyException, firstDestroyError);
	}

	@Test
	void initDestroysEarlierFiltersAfterRuntimeException() throws ServletException {
		Filter firstFilter = mock();
		Filter failingFilter = mock();
		FilterConfig filterConfig = mock();

		RuntimeException failingFilterException = new RuntimeException();
		doThrow(failingFilterException).when(failingFilter).init(filterConfig);

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, failingFilter));

		assertThatThrownBy(() -> compositeFilter.init(filterConfig)).isSameAs(failingFilterException);
		verify(firstFilter).destroy();
		verify(failingFilter, never()).destroy();
	}

	@Test
	void initDestroysEarlierFiltersAfterError() throws ServletException {
		Filter firstFilter = mock();
		Filter failingFilter = mock();
		FilterConfig filterConfig = mock();

		Error failingFilterError = new Error();
		doThrow(failingFilterError).when(failingFilter).init(filterConfig);

		CompositeFilter compositeFilter = new CompositeFilter();
		compositeFilter.setFilters(List.of(firstFilter, failingFilter));

		assertThatThrownBy(() -> compositeFilter.init(filterConfig)).isSameAs(failingFilterError);
		verify(firstFilter).destroy();
		verify(failingFilter, never()).destroy();
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
