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
