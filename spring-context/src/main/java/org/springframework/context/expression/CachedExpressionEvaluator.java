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

package org.springframework.context.expression;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * Shared utility class used to evaluate and cache SpEL expressions that
 * are defined on an {@link java.lang.reflect.AnnotatedElement AnnotatedElement}.
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @see AnnotatedElementKey
 */
public abstract class CachedExpressionEvaluator {

	private final SpelExpressionParser parser;

	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	/**
	 * Create a new instance with the default {@link SpelExpressionParser}.
	 */
	protected CachedExpressionEvaluator() {
		this(new SpelExpressionParser());
	}

	/**
	 * Create a new instance with the specified {@link SpelExpressionParser}.
	 */
	protected CachedExpressionEvaluator(SpelExpressionParser parser) {
		Assert.notNull(parser, "SpelExpressionParser must not be null");
		this.parser = parser;
	}


	/**
	 * Return the {@link SpelExpressionParser} to use.
	 */
	protected SpelExpressionParser getParser() {
		return this.parser;
	}

	/**
	 * Return a shared parameter name discoverer which caches data internally.
	 * @since 4.3
	 */
	protected ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	/**
	 * Return the parsed {@link Expression} for the specified SpEL expression.
	 * <p>{@linkplain #parseExpression(String) Parses} the expression if it hasn't
	 * already been parsed and cached.
	 * @param cache the cache to use
	 * @param elementKey the {@code AnnotatedElementKey} containing the element
	 * on which the expression is defined
	 * @param expression the expression to parse
	 */
	protected Expression getExpression(Map<ExpressionKey, Expression> cache,
			AnnotatedElementKey elementKey, String expression) {

		ExpressionKey expressionKey = createKey(elementKey, expression);
		return cache.computeIfAbsent(expressionKey, key -> parseExpression(expression));
	}

	/**
	 * Parse the specified {@code expression}.
	 * @param expression the expression to parse
	 * @since 5.3.13
	 */
	protected Expression parseExpression(String expression) {
		return getParser().parseExpression(expression);
	}

	private ExpressionKey createKey(AnnotatedElementKey elementKey, String expression) {
		return new ExpressionKey(elementKey, expression);
	}


	/**
	 * An expression key.
	 */
	protected static class ExpressionKey implements Comparable<ExpressionKey> {

		private final AnnotatedElementKey element;

		private final String expression;

		protected ExpressionKey(AnnotatedElementKey element, String expression) {
			Assert.notNull(element, "AnnotatedElementKey must not be null");
			Assert.notNull(expression, "Expression must not be null");
			this.element = element;
			this.expression = expression;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof ExpressionKey that &&
					this.element.equals(that.element) && this.expression.equals(that.expression)));
		}

		@Override
		public int hashCode() {
			return this.element.hashCode() * 29 + this.expression.hashCode();
		}

		@Override
		public String toString() {
			return this.element + " with expression \"" + this.expression + "\"";
		}

		@Override
		public int compareTo(ExpressionKey other) {
			int result = this.element.toString().compareTo(other.element.toString());
			if (result == 0) {
				result = this.expression.compareTo(other.expression);
			}
			return result;
		}
	}

}
