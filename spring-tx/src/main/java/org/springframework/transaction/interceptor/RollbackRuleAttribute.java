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

package org.springframework.transaction.interceptor;

import java.io.Serializable;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Rule determining whether a given exception should cause a rollback.
 *
 * <p>Multiple such rules can be applied to determine whether a transaction
 * should commit or rollback after an exception has been thrown.
 *
 * <p>Each rule is based on an exception type or exception pattern, supplied via
 * {@link #RollbackRuleAttribute(Class)} or {@link #RollbackRuleAttribute(String)},
 * respectively.
 *
 * <p>When a rollback rule is defined with an exception type, that type will be
 * used to match against the type of a thrown exception and its super types,
 * providing type safety and avoiding any unintentional matches that may occur
 * when using a pattern. For example, a value of
 * {@code jakarta.servlet.ServletException.class} will only match thrown exceptions
 * of type {@code jakarta.servlet.ServletException} and its subclasses.
 *
 * <p>When a rollback rule is defined with an exception pattern, the pattern can
 * be a fully qualified class name or a substring of a fully qualified class name
 * for an exception type (which must be a subclass of {@code Throwable}), with no
 * wildcard support at present. For example, a value of
 * {@code "jakarta.servlet.ServletException"} or {@code "ServletException"} will
 * match {@code jakarta.servlet.ServletException} and its subclasses.
 *
 * <p>See the javadocs for
 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * for further details on rollback rule semantics, patterns, and warnings regarding
 * possible unintentional matches with pattern-based rules.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 09.04.2003
 * @see NoRollbackRuleAttribute
 */
@SuppressWarnings("serial")
public class RollbackRuleAttribute implements Serializable{

	/**
	 * The {@linkplain RollbackRuleAttribute rollback rule} for
	 * {@link RuntimeException RuntimeExceptions}.
	 */
	public static final RollbackRuleAttribute ROLLBACK_ON_RUNTIME_EXCEPTIONS =
			new RollbackRuleAttribute(RuntimeException.class);


	/**
	 * Exception pattern: used when searching for matches in a thrown exception's
	 * class hierarchy based on names of exceptions, with zero type safety and
	 * potentially resulting in unintentional matches for similarly named exception
	 * types and nested exception types.
	 */
	private final String exceptionPattern;

	/**
	 * Exception type: used to ensure type safety when searching for matches in
	 * a thrown exception's class hierarchy.
	 * @since 6.0
	 */
	@Nullable
	private final Class<? extends Throwable> exceptionType;


	/**
	 * Create a new instance of the {@code RollbackRuleAttribute} class
	 * for the given {@code exceptionType}.
	 * <p>This is the preferred way to construct a rollback rule that matches
	 * the supplied exception type and its subclasses with type safety.
	 * <p>See the javadocs for
	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
	 * for further details on rollback rule semantics.
	 * @param exceptionType exception type; must be {@link Throwable} or a subclass
	 * of {@code Throwable}
	 * @throws IllegalArgumentException if the supplied {@code exceptionType} is
	 * not a {@code Throwable} type or is {@code null}
	 */
	@SuppressWarnings("unchecked")
	public RollbackRuleAttribute(Class<?> exceptionType) {
		Assert.notNull(exceptionType, "'exceptionType' cannot be null");
		if (!Throwable.class.isAssignableFrom(exceptionType)) {
			throw new IllegalArgumentException(
					"Cannot construct rollback rule from [" + exceptionType.getName() + "]: it's not a Throwable");
		}
		this.exceptionPattern = exceptionType.getName();
		this.exceptionType = (Class<? extends Throwable>) exceptionType;
	}

	/**
	 * Create a new instance of the {@code RollbackRuleAttribute} class
	 * for the given {@code exceptionPattern}.
	 * <p>See the javadocs for
	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
	 * for further details on rollback rule semantics, patterns, and warnings regarding
	 * possible unintentional matches.
	 * <p>For improved type safety and to avoid unintentional matches, use
	 * {@link #RollbackRuleAttribute(Class)} instead.
	 * @param exceptionPattern the exception name pattern; can also be a fully
	 * package-qualified class name
	 * @throws IllegalArgumentException if the supplied {@code exceptionPattern}
	 * is {@code null} or empty
	 */
	public RollbackRuleAttribute(String exceptionPattern) {
		Assert.hasText(exceptionPattern, "'exceptionPattern' cannot be null or empty");
		this.exceptionPattern = exceptionPattern;
		this.exceptionType = null;
	}


	/**
	 * Get the configured exception name pattern that this rule uses for matching.
	 * @see #getDepth(Throwable)
	 */
	public String getExceptionName() {
		return this.exceptionPattern;
	}

	/**
	 * Return the depth of the superclass matching, with the following semantics.
	 * <ul>
	 * <li>{@code -1} means this rule does not match the supplied {@code exception}.</li>
	 * <li>{@code 0} means this rule matches the supplied {@code exception} directly.</li>
	 * <li>Any other positive value means this rule matches the supplied {@code exception}
	 * within the superclass hierarchy, where the value is the number of levels in the
	 * class hierarchy between the supplied {@code exception} and the exception against
	 * which this rule matches directly.</li>
	 * </ul>
	 * <p>When comparing roll back rules that match against a given exception, a rule
	 * with a lower matching depth wins. For example, a direct match ({@code depth == 0})
	 * wins over a match in the superclass hierarchy ({@code depth > 0}).
	 * <p>When constructed with an exception pattern via {@link #RollbackRuleAttribute(String)},
	 * a match against a nested exception type or similarly named exception type
	 * will return a depth signifying a match at the corresponding level in the
	 * class hierarchy as if there had been a direct match.
	 */
	public int getDepth(Throwable exception) {
		return getDepth(exception.getClass(), 0);
	}


	private int getDepth(Class<?> exceptionType, int depth) {
		if (this.exceptionType != null) {
			if (this.exceptionType.equals(exceptionType)) {
				// Found it!
				return depth;
			}
		}
		else if (exceptionType.getName().contains(this.exceptionPattern)) {
			// Found it!
			return depth;
		}
		// If we've gone as far as we can go and haven't found it...
		if (exceptionType == Throwable.class) {
			return -1;
		}
		return getDepth(exceptionType.getSuperclass(), depth + 1);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof RollbackRuleAttribute that &&
				this.exceptionPattern.equals(that.exceptionPattern)));
	}

	@Override
	public int hashCode() {
		return this.exceptionPattern.hashCode();
	}

	@Override
	public String toString() {
		return "RollbackRuleAttribute with pattern [" + this.exceptionPattern + "]";
	}

}
