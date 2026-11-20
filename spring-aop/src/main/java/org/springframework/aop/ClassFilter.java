/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop;

/**
 * Filter that restricts matching of a pointcut or introduction to a given set
 * of target classes.
 *
 * <p>Can be used as part of a {@link Pointcut} or for the entire targeting of
 * an {@link IntroductionAdvisor}.
 *
 * <p><strong>WARNING</strong>: Concrete implementations of this interface must
 * provide proper implementations of {@link Object#equals(Object)},
 * {@link Object#hashCode()}, and {@link Object#toString()} in order to allow the
 * filter to be used in caching scenarios &mdash; for example, in proxies generated
 * by CGLIB. As of Spring Framework 6.0.13, the {@code toString()} implementation
 * must generate a unique string representation that aligns with the logic used
 * to implement {@code equals()}. See concrete implementations of this interface
 * within the framework for examples.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @see Pointcut
 * @see MethodMatcher
 */
@FunctionalInterface
public interface ClassFilter {

	/**
	 * Should the pointcut apply to the given interface or target class?
	 * @param clazz the candidate target class
	 * @return whether the advice should apply to the given target class
	 */
	boolean matches(Class<?> clazz);


	/**
	 * Canonical instance of a {@code ClassFilter} that matches all classes.
	 */
	ClassFilter TRUE = TrueClassFilter.INSTANCE;

}
