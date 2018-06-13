/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.env;

/**
 * A set of profiles that may be {@link Environment#acceptsProfiles(Profiles) accepted} by
 * an {@link Environment}.
 * <p>
 * May be implemented directly or, more usually, created using the {@link #of(String...)
 * of(...)} factory method.
 *
 * @author Phillip Webb
 * @since 5.0
 * @see #of(String...)
 */
@FunctionalInterface
public interface Profiles {

	/**
	 * Test if this profile set matches against given active profiles.
	 */
	boolean matches(ActiveProfiles activeProfiles);

	/**
	 * Return a new {@link Profiles} instance that checks for matches against the given
	 * profile strings. The returned instance will
	 * {@link Profiles#matches(ActiveProfiles) matches} if any one of the given profile
	 * strings match.
	 * <p>
	 * A profile string may contains a simple profile name (for example
	 * {@code "production"}) or a profile expression. A profile expression allows for more
	 * complicated profile logic to be expressed, for example
	 * {@code "production & cloud"}.
	 * <p>
	 * The following operators are supported in profile expressions:
	 * <ul>
	 * <li>{@code !} - A logical <em>not</em> of the profile</li>
	 * <li>{@code &} - A logical <em>and</em> of the profiles</li>
	 * <li>{@code |} - A logical <em>or</em> of the profiles</li></li>
	 * <p>
	 * Please note that the {@code &} and {@code |} operators may not be mixed without
	 * using parentheses. For example {@code "a & b | c"} is not a valid expression, it
	 * must be expressed as {@code "(a & b) | c"}.
	 *
	 * @param profiles the profiles to include
	 * @return a new {@link Profiles} instance
	 */
	public static Profiles of(String... profiles) {
		return ProfilesParser.parse(profiles);
	}

	/**
	 * The current set of active profiles.
	 */
	interface ActiveProfiles {

		/**
		 * Tests if given profile is currently active.
		 */
		boolean contains(String profile);
	}
}
