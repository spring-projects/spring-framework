/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.resilience.retry;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Predicate for retrying a {@link Throwable} from a specific {@link Method}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see MethodRetrySpec#predicate()
 */
@FunctionalInterface
public interface MethodRetryPredicate {

	/**
	 * Determine whether the given {@code Method} should be retried after
	 * throwing the given {@code Throwable}.
	 * @param method the method to potentially retry
	 * @param throwable the exception encountered
	 */
	boolean shouldRetry(Method method, Throwable throwable);

	/**
	 * Build a {@code Predicate} for testing exceptions from a given method.
	 * @param method the method to build a predicate for
	 */
	default Predicate<Throwable> forMethod(Method method) {
		return (t -> shouldRetry(method, t));
	}

}
