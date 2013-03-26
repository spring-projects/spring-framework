/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matcher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;

/**
 * Factory for assertions on the request. An instance of this class is
 * typically accessed via {@link MockMvcResultMatchers#request()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class RequestResultMatchers {


	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#request()}.
	 */
	protected RequestResultMatchers() {
	}

	/**
	 * Assert a request attribute value with the given Hamcrest {@link Matcher}.
	 * Whether asynchronous processing started, usually as a result of a
	 * controller method returning {@link Callable} or {@link DeferredResult}.
	 * The test will await the completion of a {@code Callable} so that
	 * {@link #asyncResult(Matcher)} can be used to assert the resulting value.
	 * Neither a {@code Callable} nor a {@code DeferredResult} will complete
	 * processing all the way since a {@link MockHttpServletRequest} does not
	 * perform asynchronous dispatches.
	 */
	public ResultMatcher asyncStarted() {
		return new ResultMatcher() {
			public void match(MvcResult result) {
				HttpServletRequest request = result.getRequest();
				assertEquals("Async started", true, request.isAsyncStarted());
			}
		};
	}

	/**
	 * Assert that asynchronous processing was not start.
	 * @see #asyncStarted()
	 */
	public ResultMatcher asyncNotStarted() {
		return new ResultMatcher() {
			public void match(MvcResult result) {
				HttpServletRequest request = result.getRequest();
				assertEquals("Async started", false, request.isAsyncStarted());
			}
		};
	}

	/**
	 * Assert the result from asynchronous processing with the given matcher.
	 */
	public <T> ResultMatcher asyncResult(final Matcher<T> matcher) {
		return new ResultMatcher() {
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) {
				HttpServletRequest request = result.getRequest();
				assertEquals("Async started", true, request.isAsyncStarted());
				assertThat("Async result", (T) result.getAsyncResult(), matcher);
			}
		};
	}

	/**
	 * Assert the result from asynchronous processing.
	 * This method can be used when a controller method returns {@link Callable}
	 * or {@link WebAsyncTask}. The value matched is the value returned from the
	 * {@code Callable} or the exception raised.
	 */
	public <T> ResultMatcher asyncResult(final Object expectedResult) {
		return new ResultMatcher() {
			public void match(MvcResult result) {
				HttpServletRequest request = result.getRequest();
				assertEquals("Async started", true, request.isAsyncStarted());
				assertEquals("Async result", expectedResult, result.getAsyncResult());
			}
		};
	}

	/**
	 * Assert a request attribute value with the given Hamcrest {@link Matcher}.
	 */
	public <T> ResultMatcher attribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) {
				T value = (T) result.getRequest().getAttribute(name);
				assertThat("Request attribute", value, matcher);
			}
		};
	}

	/**
	 * Assert a request attribute value.
	 */
	public <T> ResultMatcher attribute(final String name, final Object expectedValue) {
		return new ResultMatcher() {
			public void match(MvcResult result) {
				assertEquals("Request attribute", expectedValue, result.getRequest().getAttribute(name));
			}
		};
	}

	/**
	 * Assert a session attribute value with the given Hamcrest {@link Matcher}.
	 */
	public <T> ResultMatcher sessionAttribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) {
				T value = (T) result.getRequest().getSession().getAttribute(name);
				assertThat("Request attribute", value, matcher);
			}
		};
	}

	/**
	 * Assert a session attribute value..
	 */
	public <T> ResultMatcher sessionAttribute(final String name, final Object value) {
		return new ResultMatcher() {
			public void match(MvcResult result) {
				assertEquals("Request attribute", value, result.getRequest().getSession().getAttribute(name));
			}
		};
	}

}
