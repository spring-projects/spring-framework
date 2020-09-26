/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hamcrest.Matcher;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.Assert;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Factory for assertions on the request.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#request}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class RequestResultMatchers {

	/**
	 * Protected constructor.
	 * <p>Use {@link MockMvcResultMatchers#request()}.
	 */
	protected RequestResultMatchers() {
	}


	/**
	 * Assert whether asynchronous processing started, usually as a result of a
	 * controller method returning {@link Callable} or {@link DeferredResult}.
	 * <p>The test will await the completion of a {@code Callable} so that
	 * {@link #asyncResult(Matcher)} or {@link #asyncResult(Object)} can be used
	 * to assert the resulting value.
	 * <p>Neither a {@code Callable} nor a {@code DeferredResult} will complete
	 * processing all the way since a {@link MockHttpServletRequest} does not
	 * perform asynchronous dispatches.
	 * @see #asyncNotStarted()
	 */
	public ResultMatcher asyncStarted() {
		return result -> assertAsyncStarted(result.getRequest());
	}

	/**
	 * Assert that asynchronous processing was not started.
	 * @see #asyncStarted()
	 */
	public ResultMatcher asyncNotStarted() {
		return result -> assertFalse("Async started", result.getRequest().isAsyncStarted());
	}

	/**
	 * Assert the result from asynchronous processing with the given matcher.
	 * <p>This method can be used when a controller method returns {@link Callable}
	 * or {@link WebAsyncTask}.
	 */
	@SuppressWarnings("unchecked")
	public <T> ResultMatcher asyncResult(Matcher<? super T> matcher) {
		return result -> {
			HttpServletRequest request = result.getRequest();
			assertAsyncStarted(request);
			assertThat("Async result", (T) result.getAsyncResult(), matcher);
		};
	}

	/**
	 * Assert the result from asynchronous processing.
	 * <p>This method can be used when a controller method returns {@link Callable}
	 * or {@link WebAsyncTask}. The value matched is the value returned from the
	 * {@code Callable} or the exception raised.
	 */
	public ResultMatcher asyncResult(Object expectedResult) {
		return result -> {
			HttpServletRequest request = result.getRequest();
			assertAsyncStarted(request);
			assertEquals("Async result", expectedResult, result.getAsyncResult());
		};
	}

	/**
	 * Assert a request attribute value with the given Hamcrest {@link Matcher}.
	 */
	@SuppressWarnings("unchecked")
	public <T> ResultMatcher attribute(String name, Matcher<? super T> matcher) {
		return result -> {
			T value = (T) result.getRequest().getAttribute(name);
			assertThat("Request attribute '" + name + "'", value, matcher);
		};
	}

	/**
	 * Assert a request attribute value.
	 */
	public ResultMatcher attribute(String name, Object expectedValue) {
		return result ->
				assertEquals("Request attribute '" + name + "'", expectedValue, result.getRequest().getAttribute(name));
	}

	/**
	 * Assert a session attribute value with the given Hamcrest {@link Matcher}.
	 */
	@SuppressWarnings("unchecked")
	public <T> ResultMatcher sessionAttribute(String name, Matcher<? super T> matcher) {
		return result -> {
			HttpSession session = result.getRequest().getSession();
			Assert.state(session != null, "No HttpSession");
			T value = (T) session.getAttribute(name);
			assertThat("Session attribute '" + name + "'", value, matcher);
		};
	}

	/**
	 * Assert a session attribute value.
	 */
	public ResultMatcher sessionAttribute(String name, Object value) {
		return result -> {
			HttpSession session = result.getRequest().getSession();
			Assert.state(session != null, "No HttpSession");
			assertEquals("Session attribute '" + name + "'", value, session.getAttribute(name));
		};
	}

	/**
	 * Assert the given session attributes do not exist.
	 * @since 5.2.1
	 */
	public ResultMatcher sessionAttributeDoesNotExist(String... names) {
		return result -> {
			HttpSession session = result.getRequest().getSession();
			Assert.state(session != null, "No HttpSession");
			for (String name : names) {
				assertNull("Session attribute '" + name + "' exists", session.getAttribute(name));
			}
		};
	}

	private static void assertAsyncStarted(HttpServletRequest request) {
		assertTrue("Async not started", request.isAsyncStarted());
	}

}
