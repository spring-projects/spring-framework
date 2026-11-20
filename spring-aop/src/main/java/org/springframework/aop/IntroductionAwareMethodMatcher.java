/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop;

import java.lang.reflect.Method;

/**
 * A specialized type of {@link MethodMatcher} that takes into account introductions
 * when matching methods. If there are no introductions on the target class,
 * a method matcher may be able to optimize matching more effectively for example.
 *
 * @author Adrian Colyer
 * @since 2.0
 */
public interface IntroductionAwareMethodMatcher extends MethodMatcher {

	/**
	 * Perform static checking whether the given method matches. This may be invoked
	 * instead of the 2-arg {@link #matches(java.lang.reflect.Method, Class)} method
	 * if the caller supports the extended IntroductionAwareMethodMatcher interface.
	 * @param method the candidate method
	 * @param targetClass the target class
	 * @param hasIntroductions {@code true} if the object on whose behalf we are
	 * asking is the subject on one or more introductions; {@code false} otherwise
	 * @return whether this method matches statically
	 */
	boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions);

}
