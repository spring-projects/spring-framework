/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

import org.hamcrest.Matcher;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Factory for response header assertions. An instance of this
 * class is usually accessed via {@link MockMvcResultMatchers#header()}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class HeaderResultMatchers {


	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#header()}.
	 */
	protected HeaderResultMatchers() {
	}

	/**
	 * Assert the primary value of the named response header with the given
	 * Hamcrest {@link Matcher}.
	 */
	public ResultMatcher string(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {

			@Override
			public void match(MvcResult result) {
				assertThat("Response header " + name, result.getResponse().getHeader(name), matcher);
			}
		};
	}

	/**
	 * Assert the primary value of the named response header as a {@link String}.
	 */
	public ResultMatcher string(final String name, final String value) {
		return new ResultMatcher() {

			@Override
			public void match(MvcResult result) {
				assertEquals("Response header " + name, value, result.getResponse().getHeader(name));
			}
		};
	}

	/**
	 * Assert the primary value of the named response header as a {@code long}.
	 *
	 * <p>The {@link ResultMatcher} returned by this method throws an {@link AssertionError}
	 * if the response does not contain the specified header, or if the supplied
	 * {@code value} does not match the primary value.
	 */
	public ResultMatcher longValue(final String name, final long value) {
		return new ResultMatcher() {

			@Override
			public void match(MvcResult result) {
				assertTrue("Response does not contain header " + name, result.getResponse().containsHeader(name));
				assertEquals("Response header " + name, value, Long.parseLong(result.getResponse().getHeader(name)));
			}
		};
	}

}
