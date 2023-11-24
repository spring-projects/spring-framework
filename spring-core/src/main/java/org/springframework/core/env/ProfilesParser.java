/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Internal parser used by {@link Profiles#of}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.1
 */
final class ProfilesParser {

	private ProfilesParser() {
	}


	static Profiles parse(String... expressions) {
		Assert.notEmpty(expressions, "Must specify at least one profile expression");
		Profiles[] parsed = new Profiles[expressions.length];
		for (int i = 0; i < expressions.length; i++) {
			parsed[i] = parseExpression(expressions[i]);
		}
		return new ParsedProfiles(expressions, parsed);
	}

	private static Profiles parseExpression(String expression) {
		Assert.hasText(expression, () -> "Invalid profile expression [" + expression + "]: must contain text");
		StringTokenizer tokens = new StringTokenizer(expression, "()&|!", true);
		return parseTokens(expression, tokens);
	}

	private static Profiles parseTokens(String expression, StringTokenizer tokens) {
		return parseTokens(expression, tokens, Context.NONE);
	}

	private static Profiles parseTokens(String expression, StringTokenizer tokens, Context context) {
		List<Profiles> elements = new ArrayList<>();
		Operator operator = null;
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (token.isEmpty()) {
				continue;
			}
			switch (token) {
				case "(" -> {
					Profiles contents = parseTokens(expression, tokens, Context.PARENTHESIS);
					if (context == Context.NEGATE) {
						return contents;
					}
					elements.add(contents);
				}
				case "&" -> {
					assertWellFormed(expression, operator == null || operator == Operator.AND);
					operator = Operator.AND;
				}
				case "|" -> {
					assertWellFormed(expression, operator == null || operator == Operator.OR);
					operator = Operator.OR;
				}
				case "!" -> elements.add(not(parseTokens(expression, tokens, Context.NEGATE)));
				case ")" -> {
					Profiles merged = merge(expression, elements, operator);
					if (context == Context.PARENTHESIS) {
						return merged;
					}
					elements.clear();
					elements.add(merged);
					operator = null;
				}
				default -> {
					Profiles value = equals(token);
					if (context == Context.NEGATE) {
						return value;
					}
					elements.add(value);
				}
			}
		}
		return merge(expression, elements, operator);
	}

	private static Profiles merge(String expression, List<Profiles> elements, @Nullable Operator operator) {
		assertWellFormed(expression, !elements.isEmpty());
		if (elements.size() == 1) {
			return elements.get(0);
		}
		Profiles[] profiles = elements.toArray(new Profiles[0]);
		return (operator == Operator.AND ? and(profiles) : or(profiles));
	}

	private static void assertWellFormed(String expression, boolean wellFormed) {
		Assert.isTrue(wellFormed, () -> "Malformed profile expression [" + expression + "]");
	}

	private static Profiles or(Profiles... profiles) {
		return activeProfile -> Arrays.stream(profiles).anyMatch(isMatch(activeProfile));
	}

	private static Profiles and(Profiles... profiles) {
		return activeProfile -> Arrays.stream(profiles).allMatch(isMatch(activeProfile));
	}

	private static Profiles not(Profiles profiles) {
		return activeProfile -> !profiles.matches(activeProfile);
	}

	private static Profiles equals(String profile) {
		return activeProfile -> activeProfile.test(profile);
	}

	private static Predicate<Profiles> isMatch(Predicate<String> activeProfiles) {
		return profiles -> profiles.matches(activeProfiles);
	}


	private enum Operator { AND, OR }

	private enum Context { NONE, NEGATE, PARENTHESIS }


	private static class ParsedProfiles implements Profiles {

		private final Set<String> expressions = new LinkedHashSet<>();

		private final Profiles[] parsed;

		ParsedProfiles(String[] expressions, Profiles[] parsed) {
			Collections.addAll(this.expressions, expressions);
			this.parsed = parsed;
		}

		@Override
		public boolean matches(Predicate<String> activeProfiles) {
			for (Profiles candidate : this.parsed) {
				if (candidate.matches(activeProfiles)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof ParsedProfiles that &&
					this.expressions.equals(that.expressions)));
		}

		@Override
		public int hashCode() {
			return this.expressions.hashCode();
		}

		@Override
		public String toString() {
			if (this.expressions.size() == 1) {
				return this.expressions.iterator().next();
			}
			return this.expressions.stream().map(this::wrap).collect(Collectors.joining(" | "));
		}

		private String wrap(String str) {
			return "(" + str + ")";
		}
	}

}
