/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.MethodMatcher;

/**
 * Internal framework record, combining a {@link MethodInterceptor} instance
 * with a {@link MethodMatcher} for use as an element in the advisor chain.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @param interceptor the {@code MethodInterceptor}
 * @param matcher the {@code MethodMatcher}
 */
record InterceptorAndDynamicMethodMatcher(MethodInterceptor interceptor, MethodMatcher matcher) {
}
