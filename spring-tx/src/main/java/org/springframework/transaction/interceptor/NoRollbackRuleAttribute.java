/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.interceptor;

/**
 * Tag subclass of {@link RollbackRuleAttribute} that has the opposite behavior
 * to the {@code RollbackRuleAttribute} superclass.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 09.04.2003
 */
@SuppressWarnings("serial")
public class NoRollbackRuleAttribute extends RollbackRuleAttribute {

	/**
	 * Create a new instance of the {@code NoRollbackRuleAttribute} class
	 * for the given {@code exceptionType}.
	 * @param exceptionType exception type; must be {@link Throwable} or a subclass
	 * of {@code Throwable}
	 * @throws IllegalArgumentException if the supplied {@code exceptionType} is
	 * not a {@code Throwable} type or is {@code null}
	 * @see RollbackRuleAttribute#RollbackRuleAttribute(Class)
	 */
	public NoRollbackRuleAttribute(Class<?> exceptionType) {
		super(exceptionType);
	}

	/**
	 * Create a new instance of the {@code NoRollbackRuleAttribute} class
	 * for the supplied {@code exceptionPattern}.
	 * @param exceptionPattern the exception name pattern; can also be a fully
	 * package-qualified class name
	 * @throws IllegalArgumentException if the supplied {@code exceptionPattern}
	 * is {@code null} or empty
	 * @see RollbackRuleAttribute#RollbackRuleAttribute(String)
	 */
	public NoRollbackRuleAttribute(String exceptionPattern) {
		super(exceptionPattern);
	}

	@Override
	public String toString() {
		return "No" + super.toString();
	}

}
