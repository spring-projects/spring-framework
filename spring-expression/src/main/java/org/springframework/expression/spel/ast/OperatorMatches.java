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

package org.springframework.expression.spel.ast;

import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;
import org.springframework.util.ConcurrentLruCache;

/**
 * Implements the matches operator. Matches takes two operands:
 * The first is a String and the second is a Java regex.
 * It will return {@code true} when {@link #getValue} is called
 * if the first operand matches the regex.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class OperatorMatches extends Operator {

	/**
	 * Maximum number of compiled regular expressions in the pattern cache: {@value}.
	 * @since 6.2.19
	 */
	public static final int MAX_PATTERN_CACHE_SIZE = 256;

	private static final int PATTERN_ACCESS_THRESHOLD = 1000000;

	/**
	 * Maximum number of characters permitted in a regular expression.
	 * @since 5.2.23
	 */
	private static final int MAX_REGEX_LENGTH = 1000;


	private final ConcurrentLruCache<String, Pattern> patternCache;


	/**
	 * Create a new {@link OperatorMatches} instance.
	 * @deprecated as of Spring Framework 5.2.23; for removal in Spring Framework 7.1; invoke
	 * {@link #OperatorMatches(ConcurrentLruCache, int, int, SpelNodeImpl...)} instead
	 */
	@Deprecated(since = "5.2.23", forRemoval = true)
	public OperatorMatches(int startPos, int endPos, SpelNodeImpl... operands) {
		this(new ConcurrentLruCache<>(MAX_PATTERN_CACHE_SIZE, Pattern::compile), startPos, endPos, operands);
	}

	/**
	 * Create a new {@link OperatorMatches} instance with a shared pattern cache.
	 * <p>As of Spring Framework 6.2.19, the supplied {@code patternCacheMap} will
	 * be ignored.
	 * @since 5.2.23
	 * @deprecated as of Spring Framework 6.2.19; for removal in Spring Framework 7.1; invoke
	 * {@link #OperatorMatches(ConcurrentLruCache, int, int, SpelNodeImpl...)} instead
	 */
	@Deprecated(since = "6.2.19", forRemoval = true)
	public OperatorMatches(ConcurrentMap<String, Pattern> patternCacheMap,
			int startPos, int endPos, SpelNodeImpl... operands) {

		this(startPos, endPos, operands);
	}

	/**
	 * Create a new {@link OperatorMatches} instance with a shared pattern cache.
	 * @since 6.2.19
	 */
	public OperatorMatches(ConcurrentLruCache<String, Pattern> patternCache,
			int startPos, int endPos, SpelNodeImpl... operands) {

		super("matches", startPos, endPos, operands);
		this.patternCache = patternCache;
	}


	/**
	 * Check the first operand matches the regex specified as the second operand.
	 * @param state the expression state
	 * @return {@code true} if the first operand matches the regex specified as the
	 * second operand, otherwise {@code false}
	 * @throws EvaluationException if there is a problem evaluating the expression
	 * (for example, the regex is invalid)
	 */
	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();

		String input = leftOp.getValue(state, String.class);
		if (input == null) {
			throw new SpelEvaluationException(leftOp.getStartPosition(),
					SpelMessage.INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR, (Object) null);
		}

		Object right = rightOp.getValue(state);
		if (!(right instanceof String regex)) {
			throw new SpelEvaluationException(rightOp.getStartPosition(),
					SpelMessage.INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR, right);
		}
		if (regex.length() > MAX_REGEX_LENGTH) {
			throw new SpelEvaluationException(rightOp.getStartPosition(),
					SpelMessage.MAX_REGEX_LENGTH_EXCEEDED, MAX_REGEX_LENGTH);
		}

		try {
			Pattern pattern = this.patternCache.get(regex);
			Matcher matcher = pattern.matcher(new MatcherInput(input, new AccessCount()));
			return BooleanTypedValue.forValue(matcher.matches());
		}
		catch (PatternSyntaxException ex) {
			throw new SpelEvaluationException(
					rightOp.getStartPosition(), ex, SpelMessage.INVALID_PATTERN, right);
		}
		catch (IllegalStateException ex) {
			throw new SpelEvaluationException(
					rightOp.getStartPosition(), ex, SpelMessage.FLAWED_PATTERN, right);
		}
	}


	private static class AccessCount {

		private int count;

		public void check() throws IllegalStateException {
			if (this.count++ > PATTERN_ACCESS_THRESHOLD) {
				throw new IllegalStateException("Pattern access threshold exceeded");
			}
		}
	}


	private static class MatcherInput implements CharSequence {

		private final CharSequence value;

		private final AccessCount access;

		public MatcherInput(CharSequence value, AccessCount access) {
			this.value = value;
			this.access = access;
		}

		@Override
		public char charAt(int index) {
			this.access.check();
			return this.value.charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return new MatcherInput(this.value.subSequence(start, end), this.access);
		}

		@Override
		public int length() {
			return this.value.length();
		}

		@Override
		public String toString() {
			return this.value.toString();
		}
	}

}
