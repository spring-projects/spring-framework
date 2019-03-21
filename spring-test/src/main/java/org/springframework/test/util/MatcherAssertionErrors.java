/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/**
 * A replacement of {@link org.hamcrest.MatcherAssert} that removes the need to
 * depend on "hamcrest-all" when using Hamcrest 1.1 and also maintains backward
 * compatibility with Hamcrest 1.1 (also embedded in JUnit 4.4 through 4.8).
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 * @deprecated as of Spring 4.2, in favor of the original
 * {@link org.hamcrest.MatcherAssert} class with JUnit 4.9 / Hamcrest 1.3
 */
@Deprecated
public abstract class MatcherAssertionErrors {

	/**
	 * Assert that the given matcher matches the actual value.
	 * @param <T> the static type accepted by the matcher
	 * @param actual the value to match against
	 * @param matcher the matcher
	 */
	public static <T> void assertThat(T actual, Matcher<T> matcher) {
		assertThat("", actual, matcher);
	}

	/**
	 * Assert that the given matcher matches the actual value.
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
			description.appendText("\n     but: ");
			matcher.describeMismatch(actual, description);
			throw new AssertionError(description.toString());
		}
	}

}
