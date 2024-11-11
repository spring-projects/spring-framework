/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.assertj;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Base AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be
 * applied to an {@link HttpServletRequest}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 * @param <SELF> the type of assertions
 * @param <ACTUAL> the type of the object to assert
 */
public abstract class AbstractHttpServletRequestAssert<SELF extends AbstractHttpServletRequestAssert<SELF, ACTUAL>, ACTUAL extends HttpServletRequest>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	private final Supplier<MapAssert<String, Object>> attributesAssertProvider;

	private final Supplier<MapAssert<String, Object>> sessionAttributesAssertProvider;

	protected AbstractHttpServletRequestAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
		this.attributesAssertProvider = SingletonSupplier.of(() -> createAttributesAssert(actual));
		this.sessionAttributesAssertProvider = SingletonSupplier.of(() -> createSessionAttributesAssert(actual));
	}

	private static MapAssert<String, Object> createAttributesAssert(HttpServletRequest request) {
		Map<String, Object> map = toMap(request.getAttributeNames(), request::getAttribute);
		return Assertions.assertThat(map).as("Request Attributes");
	}

	private static MapAssert<String, Object> createSessionAttributesAssert(HttpServletRequest request) {
		HttpSession session = request.getSession();
		Assertions.assertThat(session).as("HTTP session").isNotNull();
		Map<String, Object> map = toMap(session.getAttributeNames(), session::getAttribute);
		return Assertions.assertThat(map).as("Session Attributes");
	}

	/**
	 * Return a new {@linkplain MapAssert assertion} object that uses the request
	 * attributes as the object to test, with values mapped by attribute name.
	 * <p>Example: <pre><code class='java'>
	 * // Check for the presence of a request attribute named "attributeName":
	 * assertThat(request).attributes().containsKey("attributeName");
	 * </code></pre>
	 */
	public MapAssert<String, Object> attributes() {
		return this.attributesAssertProvider.get();
	}

	/**
	 * Return a new {@linkplain MapAssert assertion} object that uses the session
	 * attributes as the object to test, with values mapped by attribute name.
	 * <p>Example: <pre><code class='java'>
	 * // Check for the presence of a session attribute named "username":
	 * assertThat(request).sessionAttributes().containsKey("username");
	 * </code></pre>
	 */
	public MapAssert<String, Object> sessionAttributes() {
		return this.sessionAttributesAssertProvider.get();
	}

	/**
	 * Verify whether asynchronous processing has started, usually as a result
	 * of a controller method returning a {@link Callable} or {@link DeferredResult}.
	 * <p>The test will await the completion of a {@code Callable} so that
	 * the asynchronous result is available and can be further asserted.
	 * <p>Neither a {@code Callable} nor a {@code DeferredResult} will complete
	 * processing all the way since a {@link MockHttpServletRequest} does not
	 * perform asynchronous dispatches.
	 * @param started whether asynchronous processing should have started
	 */
	public SELF hasAsyncStarted(boolean started) {
		Assertions.assertThat(this.actual.isAsyncStarted())
				.withFailMessage("Async expected %s have started", (started ? "to" : "not to"))
				.isEqualTo(started);
		return this.myself;
	}


	private static Map<String, Object> toMap(Enumeration<String> keys, Function<String, Object> valueProvider) {
		Map<String, Object> map = new LinkedHashMap<>();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			map.put(key, valueProvider.apply(key));
		}
		return map;
	}

}
