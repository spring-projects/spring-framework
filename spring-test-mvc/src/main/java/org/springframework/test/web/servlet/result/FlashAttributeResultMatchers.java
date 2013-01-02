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

import static org.springframework.test.util.AssertionErrors.*;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

import org.hamcrest.Matcher;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Factory for "output" flash attribute assertions. An instance of this class is
 * typically accessed via {@link MockMvcResultMatchers#flash()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class FlashAttributeResultMatchers {


	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#flash()}.
	 */
	protected FlashAttributeResultMatchers() {
	}

	/**
	 * Assert a flash attribute's value with the given Hamcrest {@link Matcher}.
	 */
	public <T> ResultMatcher attribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) throws Exception {
				assertThat("Flash attribute", (T) result.getFlashMap().get(name), matcher);
			}
		};
	}

	/**
	 * Assert a flash attribute's value.
	 */
	public <T> ResultMatcher attribute(final String name, final Object value) {
		return new ResultMatcher() {
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) throws Exception {
				assertEquals("Flash attribute", value, result.getFlashMap().get(name));
			}
		};
	}

	/**
	 * Assert the existence of the given flash attributes.
	 */
	public <T> ResultMatcher attributeExists(final String... names) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				for (String name : names) {
					assertTrue("Flash attribute [" + name + "] does not exist", result.getFlashMap().get(name) != null);
				}
			}
		};
	}

	/**
	 * Assert the number of flash attributes.
	 */
	public <T> ResultMatcher attributeCount(final int count) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				assertEquals("FlashMap size", count, result.getFlashMap().size());
			}
		};
	}

}
