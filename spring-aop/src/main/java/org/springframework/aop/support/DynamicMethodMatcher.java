/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support;

import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;

/**
 * Convenient abstract superclass for dynamic method matchers,
 * which do care about arguments at runtime.
 *
 * @author Rod Johnson
 */
public abstract class DynamicMethodMatcher implements MethodMatcher {

	@Override
	public final boolean isRuntime() {
		return true;
	}

	/**
	 * Can override to add preconditions for dynamic matching. This implementation
	 * always returns true.
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

}
