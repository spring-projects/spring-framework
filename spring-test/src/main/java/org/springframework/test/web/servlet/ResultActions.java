/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.web.servlet;

import org.springframework.test.util.ExceptionCollector;

/**
 * Allows applying actions, such as expectations, on the result of an executed
 * request.
 *
 * <p>See static factory methods in
 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers} and
 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Michał Rowicki
 * @since 3.2
 */
public interface ResultActions {

	/**
	 * Perform an expectation.
	 *
	 * <h4>Example</h4>
	 * <p>You can invoke {@code andExpect()} multiple times as in the following
	 * example.
	 * <pre class="code">
	 * // static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
	 *
	 * mockMvc.perform(get("/person/1"))
	 *   .andExpect(status().isOk())
	 *   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
	 *   .andExpect(jsonPath("$.person.name").value("Jason"));
	 * </pre>
	 * @see #andExpectAll(ResultMatcher...)
	 */
	ResultActions andExpect(ResultMatcher matcher) throws Exception;

	/**
	 * Perform multiple expectations, with the guarantee that all expectations
	 * will be asserted even if one or more expectations fail with an exception.
	 * <p>If a single {@link Error} or {@link Exception} is thrown, it will
	 * be rethrown.
	 * <p>If multiple exceptions are thrown, this method will throw an
	 * {@link AssertionError} whose error message is a summary of all the
	 * exceptions. In addition, each exception will be added as a
	 * {@linkplain Throwable#addSuppressed(Throwable) suppressed exception} to
	 * the {@code AssertionError}.
	 * <p>This feature is similar to the {@code SoftAssertions} support in AssertJ
	 * and the {@code assertAll()} support in JUnit Jupiter.
	 *
	 * <h4>Example</h4>
	 * <p>Instead of invoking {@code andExpect()} multiple times, you can invoke
	 * {@code andExpectAll()} as in the following example.
	 * <pre class="code">
	 * // static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
	 *
	 * mockMvc.perform(get("/person/1"))
	 *   .andExpectAll(
	 *       status().isOk(),
	 *       content().contentType(MediaType.APPLICATION_JSON),
	 *       jsonPath("$.person.name").value("Jason")
	 *   );
	 * </pre>
	 * @since 5.3.10
	 * @see #andExpect(ResultMatcher)
	 */
	default ResultActions andExpectAll(ResultMatcher... matchers) throws Exception {
		ExceptionCollector exceptionCollector = new ExceptionCollector();
		for (ResultMatcher matcher : matchers) {
			exceptionCollector.execute(() -> this.andExpect(matcher));
		}
		exceptionCollector.assertEmpty();
		return this;
	}

	/**
	 * Perform a general action.
	 *
	 * <h4>Example</h4>
	 * <pre class="code">
	 * static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
	 *
	 * mockMvc.perform(get("/form")).andDo(print());
	 * </pre>
	 */
	ResultActions andDo(ResultHandler handler) throws Exception;

	/**
	 * Return the result of the executed request for direct access to the results.
	 * @return the result of the request
	 */
	MvcResult andReturn();

}
