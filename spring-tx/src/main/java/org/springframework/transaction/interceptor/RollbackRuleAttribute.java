/*
 * Copyright 2002-2022 the original author or authors.
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
 * Rule determining whether or not a given exception should cause a rollback.
 *
 * <p>Multiple such rules can be applied to determine whether a transaction
 * should commit or rollback after an exception has been thrown.
 *
 * <p>Each rule is based on an exception pattern which can be a fully qualified
 * class name or a substring of a fully qualified class name for an exception
 * type (which must be a subclass of {@code Throwable}), with no wildcard support
 * at present. For example, a value of {@code "javax.servlet.ServletException"}
 * or {@code "ServletException"} would match {@code javax.servlet.ServletException}
 * and its subclasses.
 *
 * <p>An exception pattern can be specified as a {@link Class} reference or a
 * {@link String} in {@link #RollbackRuleAttribute(Class)} and
 * {@link #RollbackRuleAttribute(String)}, respectively. When an exception type
 * is specified as a class reference its fully qualified name will be used as the
 * pattern. See the javadocs for
 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * for further details on rollback rule semantics, patterns, and warnings regarding
 * possible unintentional matches.
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
	 * Could hold exception, resolving class name but would always require FQN.
	 * This way does multiple string comparisons, but how often do we decide
	 * whether to roll back a transaction following an exception?
	 */
	private final String exceptionPattern;


	/**
	 * Create a new instance of the {@code RollbackRuleAttribute} class
	 * for the given {@code exceptionType}.
	 * <p>This is the preferred way to construct a rollback rule that matches
	 * the supplied exception type, its subclasses, and its nested classes.
	 * <p>See the javadocs for
	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
	 * for further details on rollback rule semantics, patterns, and warnings regarding
	 * possible unintentional matches.
	 * @param exceptionType exception type; must be {@link Throwable} or a subclass
	 * of {@code Throwable}
	 * @throws IllegalArgumentException if the supplied {@code exceptionType} is
	 * not a {@code Throwable} type or is {@code null}
	 */
	public RollbackRuleAttribute(Class<?> exceptionType) {
		Assert.notNull(exceptionType, "'exceptionType' cannot be null");
		if (!Throwable.class.isAssignableFrom(exceptionType)) {
			throw new IllegalArgumentException(
					"Cannot construct rollback rule from [" + exceptionType.getName() + "]: it's not a Throwable");
		}
		this.exceptionPattern = exceptionType.getName();
	}

	/**
	 * Create a new instance of the {@code RollbackRuleAttribute} class
	 * for the given {@code exceptionPattern}.
	 * <p>See the javadocs for
	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
	 * for further details on rollback rule semantics, patterns, and warnings regarding
	 * possible unintentional matches.
	 * @param exceptionPattern the exception name pattern; can also be a fully
	 * package-qualified class name
	 * @throws IllegalArgumentException if the supplied {@code exceptionPattern}
	 * is {@code null} or empty
	 */
	public RollbackRuleAttribute(String exceptionPattern) {
		Assert.hasText(exceptionPattern, "'exceptionPattern' cannot be null or empty");
		this.exceptionPattern = exceptionPattern;
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
	 * <p>A match against a nested exception type or similarly named exception type
	 * will return a depth signifying a match at the corresponding level in the
	 * class hierarchy as if there had been a direct match.
	 */
	public int getDepth(Throwable exception) {
		return getDepth(exception.getClass(), 0);
	}


	private int getDepth(Class<?> exceptionType, int depth) {
		if (exceptionType.getName().contains(this.exceptionPattern)) {
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
		if (this == other) {
			return true;
		}
		if (!(other instanceof RollbackRuleAttribute)) {
			return false;
		}
		RollbackRuleAttribute rhs = (RollbackRuleAttribute) other;
		return this.exceptionPattern.equals(rhs.exceptionPattern);
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
