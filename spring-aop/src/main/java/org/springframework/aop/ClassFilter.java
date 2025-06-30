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
