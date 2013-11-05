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
package org.springframework.test.util;

import java.lang.reflect.Method;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.springframework.util.ClassUtils;

/**
 * A replacement of {@link org.hamcrest.MatcherAssert} that removes the need to
 * depend on "hamcrest-all" when using Hamcrest 1.1 and also maintains backward
 * compatibility with Hamcrest 1.1 (also embedded in JUnit 4.4 through 4.8).
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class MatcherAssertionErrors {

	private static final Method describeMismatchMethod =
			ClassUtils.getMethodIfAvailable(Matcher.class, "describeMismatch", Object.class, Description.class);


	private MatcherAssertionErrors() {
	}

	/**
	 * Asserts that the given matcher matches the actual value.
	 *
	 * @param <T> the static type accepted by the matcher
	 * @param actual the value to match against
	 * @param matcher the matcher
	 */
	public static <T> void assertThat(T actual, Matcher<T> matcher) {
		assertThat("", actual, matcher);
	}

	/**
	 * Asserts that the given matcher matches the actual value.
	 *
	 * @param <T> the static type accepted by the matcher
	 * @param reason additional information about the error
	 * @param actual the value to match against
	 * @param matcher the matcher
	 */
	public static <T> void assertThat(String reason, T actual, Matcher<T> matcher) {
		if (!matcher.matches(actual)) {
			Description description = new StringDescription();
			description.appendText(reason);
			description.appendText("\nExpected: ");
			description.appendDescriptionOf(matcher);
			if (describeMismatchMethod != null) {
				description.appendText("\n     but: ");
				matcher.describeMismatch(actual, description);
			}
			else {
				description.appendText("\n     got: ");
				description.appendValue(actual);
				description.appendText("\n");
			}
			throw new AssertionError(description.toString());
		}
	}

}
