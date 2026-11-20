/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * Canonical MethodMatcher instance that matches all methods.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
final class TrueMethodMatcher implements MethodMatcher, Serializable {

	public static final TrueMethodMatcher INSTANCE = new TrueMethodMatcher();


	/**
	 * Enforce Singleton pattern.
	 */
	private TrueMethodMatcher() {
	}


	@Override
	public boolean isRuntime() {
		return false;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, @Nullable Object... args) {
		// Should never be invoked as isRuntime returns false.
		throw new UnsupportedOperationException();
	}


	@Override
	public String toString() {
		return "MethodMatcher.TRUE";
	}

	/**
	 * Required to support serialization. Replaces with canonical
	 * instance on deserialization, protecting Singleton pattern.
	 * Alternative to overriding {@code equals()}.
	 */
	private Object readResolve() {
		return INSTANCE;
	}

}
