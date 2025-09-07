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

package org.springframework.resilience.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;

/**
 * A common annotation specifying a concurrency limit for an individual method,
 * or for all proxy-invoked methods in a given class hierarchy if annotated at
 * the type level.
 *
 * <p>In the type-level case, all methods inheriting the concurrency limit
 * from the type level share a common concurrency throttle, with any mix
 * of such method invocations contributing to the shared concurrency limit.
 * Whereas for a locally annotated method, a local throttle with the specified
 * limit is going to be applied to invocations of that particular method only.
 *
 * <p>This is particularly useful with Virtual Threads where there is generally
 * no thread pool limit in place. For asynchronous tasks, this can be constrained
 * on {@link org.springframework.core.task.SimpleAsyncTaskExecutor}; for
 * synchronous invocations, this annotation provides equivalent behavior through
 * {@link org.springframework.aop.interceptor.ConcurrencyThrottleInterceptor}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see EnableResilientMethods
 * @see ConcurrencyLimitBeanPostProcessor
 * @see org.springframework.aop.interceptor.ConcurrencyThrottleInterceptor
 * @see org.springframework.core.task.SimpleAsyncTaskExecutor#setConcurrencyLimit
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective
public @interface ConcurrencyLimit {

	/**
	 * The applicable concurrency limit: 1 by default,
	 * effectively locking the target instance for each method invocation.
	 * <p>Specify a limit higher than 1 for pool-like throttling, constraining
	 * the number of concurrent invocations similar to the upper bound of a pool.
	 */
	int value() default 1;

}
