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

package org.springframework.aop;

import java.lang.reflect.Method;

/**
 * Part of a {@link Pointcut}: Checks whether the target method is eligible for advice.
 *
 * <p>A {@code MethodMatcher} may be evaluated <b>statically</b> or at <b>runtime</b>
 * (dynamically). Static matching involves a method and (possibly) method attributes.
 * Dynamic matching also makes arguments for a particular call available, and any
 * effects of running previous advice applying to the joinpoint.
 *
 * <p>If an implementation returns {@code false} from its {@link #isRuntime()}
 * method, evaluation can be performed statically, and the result will be the same
 * for all invocations of this method, whatever their arguments. This means that
 * if the {@link #isRuntime()} method returns {@code false}, the 3-arg
 * {@link #matches(Method, Class, Object[])} method will never be invoked.
 *
 * <p>If an implementation returns {@code true} from its 2-arg
 * {@link #matches(Method, Class)} method and its {@link #isRuntime()} method
 * returns {@code true}, the 3-arg {@link #matches(Method, Class, Object[])}
 * method will be invoked <i>immediately before each potential execution of the
 * related advice</i> to decide whether the advice should run. All previous advice,
 * such as earlier interceptors in an interceptor chain, will have run, so any
 * state changes they have produced in parameters or {@code ThreadLocal} state will
 * be available at the time of evaluation.
 *
 * <p><strong>WARNING</strong>: Concrete implementations of this interface must
 * provide proper implementations of {@link Object#equals(Object)},
 * {@link Object#hashCode()}, and {@link Object#toString()} in order to allow the
 * matcher to be used in caching scenarios &mdash; for example, in proxies generated
 * by CGLIB. As of Spring Framework 6.0.13, the {@code toString()} implementation
 * must generate a unique string representation that aligns with the logic used
 * to implement {@code equals()}. See concrete implementations of this interface
 * within the framework for examples.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 11.11.2003
 * @see Pointcut
 * @see ClassFilter
 */
public interface MethodMatcher {

	/**
	 * Perform static checking to determine whether the given method matches.
	 * <p>If this method returns {@code false} or if {@link #isRuntime()}
	 * returns {@code false}, no runtime check (i.e. no
	 * {@link #matches(Method, Class, Object[])} call) will be made.
	 * @param method the candidate method
	 * @param targetClass the target class
	 * @return whether this method matches statically
	 */
	boolean matches(Method method, Class<?> targetClass);

	/**
	 * Is this {@code MethodMatcher} dynamic, that is, must a final check be made
	 * via the {@link #matches(Method, Class, Object[])} method at runtime even
	 * if {@link #matches(Method, Class)} returns {@code true}?
	 * <p>Can be invoked when an AOP proxy is created, and need not be invoked
	 * again before each method invocation.
	 * @return whether a runtime match via {@link #matches(Method, Class, Object[])}
	 * is required if static matching passed
	 */
	boolean isRuntime();

	/**
	 * Check whether there is a runtime (dynamic) match for this method, which
	 * must have matched statically.
	 * <p>This method is invoked only if {@link #matches(Method, Class)} returns
	 * {@code true} for the given method and target class, and if
	 * {@link #isRuntime()} returns {@code true}.
	 * <p>Invoked immediately before potential running of the advice, after any
	 * advice earlier in the advice chain has run.
	 * @param method the candidate method
	 * @param targetClass the target class
	 * @param args arguments to the method
	 * @return whether there's a runtime match
	 * @see #matches(Method, Class)
	 */
	boolean matches(Method method, Class<?> targetClass, Object... args);


	/**
	 * Canonical instance of a {@code MethodMatcher} that matches all methods.
	 */
	MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

}
