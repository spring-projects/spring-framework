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

import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.context.request.async.MvcAsyncTask;
import org.springframework.web.context.request.async.DeferredResult;

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
				MatcherAssert.assertThat("Async started", request.isAsyncStarted(), equalTo(true));
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
				MatcherAssert.assertThat("Async started", request.isAsyncStarted(), equalTo(false));
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
				MatcherAssert.assertThat("Async started", request.isAsyncStarted(), equalTo(true));
				MatcherAssert.assertThat("Async result", (T) result.getAsyncResult(), matcher);
			}
		};
	}

	/**
	 * Assert the result from asynchronous processing.
	 * This method can be used when a controller method returns {@link Callable}
	 * or {@link MvcAsyncTask}. The value matched is the value returned from the
	 * {@code Callable} or the exception raised.
	 */
	public <T> ResultMatcher asyncResult(Object expectedResult) {
		return asyncResult(equalTo(expectedResult));
	}

	/**
	 * Assert a request attribute value with the given Hamcrest {@link Matcher}.
	 */
	public <T> ResultMatcher attribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) {
				T value = (T) result.getRequest().getAttribute(name);
				MatcherAssert.assertThat("Request attribute: ", value, matcher);
			}
		};
	}

	/**
	 * Assert a request attribute value.
	 */
	public <T> ResultMatcher attribute(String name, Object value) {
		return attribute(name, Matchers.equalTo(value));
	}

	/**
	 * Assert a session attribute value with the given Hamcrest {@link Matcher}.
	 */
	public <T> ResultMatcher sessionAttribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) {
				T value = (T) result.getRequest().getSession().getAttribute(name);
				MatcherAssert.assertThat("Request attribute: ", value, matcher);
			}
		};
	}

	/**
	 * Assert a session attribute value..
	 */
	public <T> ResultMatcher sessionAttribute(String name, Object value) {
		return sessionAttribute(name, Matchers.equalTo(value));
	}

}
