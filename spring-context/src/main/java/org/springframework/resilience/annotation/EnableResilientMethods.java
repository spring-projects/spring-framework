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

import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * Enables Spring's core resilience features for method invocations:
 * {@link Retryable @Retryable} as well as {@link ConcurrencyLimit @ConcurrencyLimit}.
 *
 * <p>These annotations can also be individually enabled by
 * defining a {@link RetryAnnotationBeanPostProcessor} or a
 * {@link ConcurrencyLimitBeanPostProcessor}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see RetryAnnotationBeanPostProcessor
 * @see ConcurrencyLimitBeanPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ResilientMethodsConfiguration.class)
public @interface EnableResilientMethods {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies.
	 * <p>The default is {@code false}.
	 * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
	 * Spring-managed beans requiring proxying, not just those marked with {@code @Retryable}
	 * or {@code @ConcurrencyLimit}. For example, other beans marked with Spring's
	 * {@code @Transactional} annotation will be upgraded to subclass proxying at
	 * the same time. This approach has no negative impact in practice unless one is
	 * explicitly expecting one type of proxy vs. another &mdash; for example, in tests.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate the order in which the {@link RetryAnnotationBeanPostProcessor}
	 * and {@link ConcurrencyLimitBeanPostProcessor} should be applied.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE} in order to run
	 * after all other post-processors, so that they can add advisors to
	 * existing proxies rather than double-proxy.
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
