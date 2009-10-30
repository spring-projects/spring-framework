/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.instrument.classloading.jboss;

import java.lang.reflect.Method;

/**
 * Reflection helper.
 *
 * @author Ales Justin
 */
public abstract class ReflectionHelper {

	/**
	 * Get method from class.
	 *
	 * @param clazz the owner class
	 * @param name the method name
	 * @return declared method
	 * @throws Exception for any error
	 */
	protected static Method getMethod(Class<?> clazz, String name) throws Exception {
		Method method = clazz.getDeclaredMethod(name);
		method.setAccessible(true);
		return method;
	}

	/**
	 * Invoke method and check the result.
	 *
	 * @param method the method
	 * @param target the target
	 * @param expectedType the expected type
	 * @param <T> the exact type
	 * @return invocation's result
	 * @throws Exception for any error
	 */
	protected static <T> T invokeMethod(Method method, Object target, Class<T> expectedType) throws Exception {
		Object result = method.invoke(target);
		if (expectedType.isInstance(result) == false) {
			throw new IllegalArgumentException("Returned result must be instance of [" + expectedType.getName() + "]");
		}

		return expectedType.cast(result);
	}
}
