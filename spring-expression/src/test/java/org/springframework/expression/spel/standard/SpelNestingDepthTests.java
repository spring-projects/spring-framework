/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.expression.spel.standard;

import org.junit.jupiter.api.Test;

import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.SpelParserConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for the configurable structural nesting depth limit in SpEL expressions.
 *
 * @author Naman Agrawal
 * @since 7.0
 * @see SpelParserConfiguration#DEFAULT_MAX_NESTING_DEPTH
 * @see SpelParserConfiguration#SPRING_EXPRESSION_MAX_NESTING_DEPTH_PROPERTY_NAME
 * @see SpelMessage#MAX_NESTING_DEPTH_EXCEEDED
 */
class SpelNestingDepthTests {

	// ------------------------------------------------------------------
	// SpelParserConfiguration defaults and accessors
	// ------------------------------------------------------------------

	@Test
	void defaultMaxNestingDepthConstantIsPositive() {
		assertThat(SpelParserConfiguration.DEFAULT_MAX_NESTING_DEPTH).isPositive();
	}

	@Test
	void defaultConfigurationReportsDefaultNestingDepth() {
		SpelParserConfiguration config = new SpelParserConfiguration();
		assertThat(config.getMaximumNestingDepth())
				.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_NESTING_DEPTH);
	}

	@Test
	void customNestingDepthIsStoredCorrectly() {
		SpelParserConfiguration config = new SpelParserConfiguration(
				null, null, false, false, Integer.MAX_VALUE,
				SpelParserConfiguration.DEFAULT_MAX_EXPRESSION_LENGTH,
				SpelParserConfiguration.DEFAULT_MAX_OPERATIONS, 42);
		assertThat(config.getMaximumNestingDepth()).isEqualTo(42);
	}

	@Test
	void nestingDepthMustBePositive() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SpelParserConfiguration(
						null, null, false, false, Integer.MAX_VALUE,
						SpelParserConfiguration.DEFAULT_MAX_EXPRESSION_LENGTH,
						SpelParserConfiguration.DEFAULT_MAX_OPERATIONS, 0))
				.withMessageContaining("maximumNestingDepth");
	}

	// ------------------------------------------------------------------
	// Inline lists
	// ------------------------------------------------------------------

	@Test
	void flatInlineListWithinDepthLimit() {
		SpelExpressionParser parser = parserWithMaxDepth(3);
		// {1, 2, 3} – depth 1, should parse fine
		assertThat(parser.parseExpression("{1, 2, 3}")).isNotNull();
	}

	@Test
	void nestedInlineListAtExactLimit() {
		SpelExpressionParser parser = parserWithMaxDepth(3);
		// {{{}}} – depth 3, exactly at the limit
		assertThat(parser.parseExpression("{{{}}}")).isNotNull();
	}

	@Test
	void nestedInlineListExceedingDepthThrowsException() {
		SpelExpressionParser parser = parserWithMaxDepth(3);
		// {{{{}}}} – depth 4, one level over
		assertThatExceptionOfType(SpelParseException.class)
				.isThrownBy(() -> parser.parseExpression("{{{{}}}}"))
				.satisfies(ex -> assertThat(ex.getMessageCode())
						.isEqualTo(SpelMessage.MAX_NESTING_DEPTH_EXCEEDED));
	}

	@Test
	void deeplyNestedInlineListExceedsDefaultLimitEventually() {
		SpelExpressionParser parser = new SpelExpressionParser();
		String expr = buildNestedList(SpelParserConfiguration.DEFAULT_MAX_NESTING_DEPTH + 1);
		assertThatExceptionOfType(SpelParseException.class)
				.isThrownBy(() -> parser.parseExpression(expr))
				.satisfies(ex -> assertThat(ex.getMessageCode())
						.isEqualTo(SpelMessage.MAX_NESTING_DEPTH_EXCEEDED));
	}

	// ------------------------------------------------------------------
	// Inline maps
	// ------------------------------------------------------------------

	@Test
	void flatInlineMapWithinDepthLimit() {
		SpelExpressionParser parser = parserWithMaxDepth(3);
		// {'k': 'v'} – depth 1
		assertThat(parser.parseExpression("{'k': 'v'}")).isNotNull();
	}

	@Test
	void nestedInlineMapAtExactLimit() {
		SpelExpressionParser parser = parserWithMaxDepth(3);
		// {'a': {'b': {'c': 1}}} – depth 3
		assertThat(parser.parseExpression("{'a': {'b': {'c': 1}}}")).isNotNull();
	}

	@Test
	void nestedInlineMapExceedingDepthThrowsException() {
		SpelExpressionParser parser = parserWithMaxDepth(2);
		// {'a': {'b': {'c': 1}}} – depth 3, over the limit of 2
		assertThatExceptionOfType(SpelParseException.class)
				.isThrownBy(() -> parser.parseExpression("{'a': {'b': {'c': 1}}}"))
				.satisfies(ex -> assertThat(ex.getMessageCode())
						.isEqualTo(SpelMessage.MAX_NESTING_DEPTH_EXCEEDED));
	}

	// ------------------------------------------------------------------
	// Mixed list + map nesting
	// ------------------------------------------------------------------

	@Test
	void mixedNestingAtExactLimit() {
		SpelExpressionParser parser = parserWithMaxDepth(2);
		// {{'k': 'v'}} – depth 2
		assertThat(parser.parseExpression("{{'k': 'v'}}")).isNotNull();
	}

	@Test
	void mixedNestingExceedingDepthThrowsException() {
		SpelExpressionParser parser = parserWithMaxDepth(2);
		// {{{'k': 'v'}}} – depth 3
		assertThatExceptionOfType(SpelParseException.class)
				.isThrownBy(() -> parser.parseExpression("{{{'k': 'v'}}}"))
				.satisfies(ex -> assertThat(ex.getMessageCode())
						.isEqualTo(SpelMessage.MAX_NESTING_DEPTH_EXCEEDED));
	}

	// ------------------------------------------------------------------
	// Re-use of parser across multiple expressions (counter must reset)
	// ------------------------------------------------------------------

	@Test
	void parserIsReusableAfterExceedingDepth() {
		SpelExpressionParser parser = parserWithMaxDepth(1);

		// First parse: exceeds depth
		assertThatExceptionOfType(SpelParseException.class)
				.isThrownBy(() -> parser.parseExpression("{{1}}"));

		// Second parse: within depth – must succeed
		assertThat(parser.parseExpression("{1}")).isNotNull();
	}

	@Test
	void errorMessageContainsConfiguredLimit() {
		int limit = 2;
		SpelExpressionParser parser = parserWithMaxDepth(limit);
		assertThatExceptionOfType(SpelParseException.class)
				.isThrownBy(() -> parser.parseExpression("{{{1}}}"))
				.withMessageContaining(String.valueOf(limit));
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private static SpelExpressionParser parserWithMaxDepth(int maxDepth) {
		SpelParserConfiguration config = new SpelParserConfiguration(
				null, null, false, false, Integer.MAX_VALUE,
				SpelParserConfiguration.DEFAULT_MAX_EXPRESSION_LENGTH,
				SpelParserConfiguration.DEFAULT_MAX_OPERATIONS, maxDepth);
		return new SpelExpressionParser(config);
	}

	/** Build a string like {{...{}}...}} with {@code depth} levels of nesting. */
	private static String buildNestedList(int depth) {
		return "{".repeat(depth) + "}".repeat(depth);
	}

}
