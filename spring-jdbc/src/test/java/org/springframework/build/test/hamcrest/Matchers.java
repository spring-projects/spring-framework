/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.build.test.hamcrest;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Additional hamcrest matchers.
 *
 * @author Phillip Webb
 */
public class Matchers {

	/**
	 * Create a matcher that wrapps the specified matcher and tests against the
	 * {@link Throwable#getCause() cause} of an exception. If the item tested
	 * is {@code null} not a {@link Throwable} the wrapped matcher will be called
	 * with a {@code null} item.
	 *
	 * <p>Often useful when working with JUnit {@link ExpectedException}
	 * {@link Rule @Rule}s, for example:
	 * <pre>
	 * thrown.expect(DataAccessException.class);
	 * thrown.except(exceptionCause(isA(SQLException.class)));
	 * </pre>
	 *
	 * @param matcher the matcher to wrap (must not be null)
	 * @return a matcher that tests using the exception cause
	 */
	@SuppressWarnings("unchecked")
	public static <T> Matcher<T> exceptionCause(final Matcher<T> matcher) {
		return (Matcher<T>) new BaseMatcher<Object>() {
			@Override
			public boolean matches(Object item) {
				Throwable cause = null;
				if(item != null && item instanceof Throwable) {
					cause = ((Throwable)item).getCause();
				}
				return matcher.matches(cause);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("exception cause ").appendDescriptionOf(matcher);
			}
		};
	}

}
