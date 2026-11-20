/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;

/**
 * A custom {@link KeyGenerator} that exposes the algorithm used to compute the key
 * for convenience in test scenarios.
 *
 * @author Stephane Nicoll
 */
public class SomeCustomKeyGenerator implements KeyGenerator {

	@Override
	public Object generate(Object target, Method method, Object... params) {
		return generateKey(method.getName(), params);
	}

	/**
	 * @see #generate(Object, java.lang.reflect.Method, Object...)
	 */
	public static Object generateKey(String methodName, Object... params) {
		final StringBuilder sb = new StringBuilder(methodName);
		for (Object param : params) {
			sb.append(param);
		}
		return sb.toString();
	}

}
