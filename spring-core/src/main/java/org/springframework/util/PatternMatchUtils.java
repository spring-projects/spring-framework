/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.util;

import org.jspecify.annotations.Nullable;

/**
 * Utility methods for simple pattern matching, in particular for Spring's typical
 * {@code xxx*}, {@code *xxx}, {@code *xxx*}, and {@code xxx*yyy} pattern styles.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class PatternMatchUtils {

	/**
	 * Match a String against the given pattern, supporting direct equality as
	 * well as the following simple pattern styles: {@code xxx*}, {@code *xxx},
	 * {@code *xxx*}, and {@code xxx*yyy} (with an arbitrary number of pattern parts).
	 * <p>Returns {@code false} if the supplied String or pattern is {@code null}.
	 * @param pattern the pattern to match against
	 * @param str the String to match
	 * @return whether the String matches the given pattern
	 */
	public static boolean simpleMatch(@Nullable String pattern, @Nullable String str) {
		return simpleMatch(pattern, str, false);
	}

	/**
	 * Variant of {@link #simpleMatch(String, String)} that ignores upper/lower case.
	 * @since 6.1.20
	 */
	public static boolean simpleMatchIgnoreCase(@Nullable String pattern, @Nullable String str) {
		return simpleMatch(pattern, str, true);
	}

	private static boolean simpleMatch(@Nullable String pattern, @Nullable String str, boolean ignoreCase) {
		if (pattern == null || str == null) {
			return false;
		}

		int firstIndex = pattern.indexOf('*');
		if (firstIndex == -1) {
			return (ignoreCase ? pattern.equalsIgnoreCase(str) : pattern.equals(str));
		}

		if (firstIndex == 0) {
			if (pattern.length() == 1) {
				return true;
			}
			int nextIndex = pattern.indexOf('*', 1);
			if (nextIndex == -1) {
				String part = pattern.substring(1);
				return (ignoreCase ? StringUtils.endsWithIgnoreCase(str, part) : str.endsWith(part));
			}
			String part = pattern.substring(1, nextIndex);
			if (part.isEmpty()) {
				return simpleMatch(pattern.substring(nextIndex), str, ignoreCase);
			}
			int partIndex = indexOf(str, part, 0, ignoreCase);
			while (partIndex != -1) {
				if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()), ignoreCase)) {
					return true;
				}
				partIndex = indexOf(str, part, partIndex + 1, ignoreCase);
			}
			return false;
		}

		return (str.length() >= firstIndex &&
				checkStartsWith(pattern, str, firstIndex, ignoreCase) &&
				simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex), ignoreCase));
	}

	private static boolean checkStartsWith(String pattern, String str, int index, boolean ignoreCase) {
		String part = str.substring(0, index);
		return (ignoreCase ? StringUtils.startsWithIgnoreCase(pattern, part) : pattern.startsWith(part));
	}

	private static int indexOf(String str, String otherStr, int startIndex, boolean ignoreCase) {
		if (!ignoreCase) {
			return str.indexOf(otherStr, startIndex);
		}
		for (int i = startIndex; i <= (str.length() - otherStr.length()); i++) {
			if (str.regionMatches(true, i, otherStr, 0, otherStr.length())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Match a String against the given patterns, supporting direct equality as
	 * well as the following simple pattern styles: {@code xxx*}, {@code *xxx},
	 * {@code *xxx*}, and {@code xxx*yyy} (with an arbitrary number of pattern parts).
	 * <p>Returns {@code false} if the supplied String is {@code null} or if the
	 * supplied patterns array is {@code null} or empty.
	 * @param patterns the patterns to match against
	 * @param str the String to match
	 * @return whether the String matches any of the given patterns
	 */
	public static boolean simpleMatch(String @Nullable [] patterns, @Nullable String str) {
		if (patterns != null) {
			for (String pattern : patterns) {
				if (simpleMatch(pattern, str)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Variant of {@link #simpleMatch(String[], String)}  that ignores upper/lower case.
	 * @since 6.1.20
	 */
	public static boolean simpleMatchIgnoreCase(String @Nullable [] patterns, @Nullable String str) {
		if (patterns != null) {
			for (String pattern : patterns) {
				if (simpleMatch(pattern, str, true)) {
					return true;
				}
			}
		}
		return false;
	}

}
