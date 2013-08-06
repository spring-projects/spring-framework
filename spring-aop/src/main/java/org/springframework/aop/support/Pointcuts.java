/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.util.Assert;

/**
 * Pointcut constants for matching getters and setters,
 * and static methods useful for manipulating and evaluating pointcuts.
 * These methods are particularly useful for composing pointcuts
 * using the union and intersection methods.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class Pointcuts {

	/** Pointcut matching all bean property setters, in any class */
	public static final Pointcut SETTERS = SetterPointcut.INSTANCE;

	/** Pointcut matching all bean property getters, in any class */
	public static final Pointcut GETTERS = GetterPointcut.INSTANCE;


	/**
	 * Match all methods that <b>either</b> (or both) of the given pointcuts matches.
	 * @param pc1 the first Pointcut
	 * @param pc2 the second Pointcut
	 * @return a distinct Pointcut that matches all methods that either
	 * of the given Pointcuts matches
	 */
	public static Pointcut union(Pointcut pc1, Pointcut pc2) {
		return new ComposablePointcut(pc1).union(pc2);
	}

	/**
	 * Match all methods that <b>both</b> the given pointcuts match.
	 * @param pc1 the first Pointcut
	 * @param pc2 the second Pointcut
	 * @return a distinct Pointcut that matches all methods that both
	 * of the given Pointcuts match
	 */
	public static Pointcut intersection(Pointcut pc1, Pointcut pc2) {
		return new ComposablePointcut(pc1).intersection(pc2);
	}

	/**
	 * Perform the least expensive check for a pointcut match.
	 * @param pointcut the pointcut to match
	 * @param method the candidate method
	 * @param targetClass the target class
	 * @param args arguments to the method
	 * @return whether there's a runtime match
	 */
	public static boolean matches(Pointcut pointcut, Method method, Class targetClass, Object[] args) {
		Assert.notNull(pointcut, "Pointcut must not be null");
		if (pointcut == Pointcut.TRUE) {
			return true;
		}
		if (pointcut.getClassFilter().matches(targetClass)) {
			// Only check if it gets past first hurdle.
			MethodMatcher mm = pointcut.getMethodMatcher();
			if (mm.matches(method, targetClass)) {
				// We may need additional runtime (argument) check.
				return (!mm.isRuntime() || mm.matches(method, targetClass, args));
			}
		}
		return false;
	}


	/**
	 * Pointcut implementation that matches bean property setters.
	 */
	@SuppressWarnings("serial")
	private static class SetterPointcut extends StaticMethodMatcherPointcut implements Serializable {

		public static SetterPointcut INSTANCE = new SetterPointcut();

		@Override
		public boolean matches(Method method, Class targetClass) {
			return method.getName().startsWith("set") &&
				method.getParameterTypes().length == 1 &&
				method.getReturnType() == Void.TYPE;
		}

		private Object readResolve() {
			return INSTANCE;
		}
	}


	/**
	 * Pointcut implementation that matches bean property getters.
	 */
	@SuppressWarnings("serial")
	private static class GetterPointcut extends StaticMethodMatcherPointcut implements Serializable {

		public static GetterPointcut INSTANCE = new GetterPointcut();

		@Override
		public boolean matches(Method method, Class targetClass) {
			return method.getName().startsWith("get") &&
				method.getParameterTypes().length == 0;
		}

		private Object readResolve() {
			return INSTANCE;
		}
	}

}
