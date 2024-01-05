/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.aspectj;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.weaver.tools.ShadowMatch;

import org.springframework.aop.support.ExpressionPointcut;
import org.springframework.lang.Nullable;

/**
 * Internal {@link ShadowMatch} utilities.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public abstract class ShadowMatchUtils {

	private static final Map<Key, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(256);

	/**
	 * Clear the cache of computed {@link ShadowMatch} instances.
	 */
	public static void clearCache() {
		shadowMatchCache.clear();
	}

	/**
	 * Return the {@link ShadowMatch} for the specified {@link ExpressionPointcut}
	 * and {@link Method} or {@code null} if none is found.
	 * @param expression the expression
	 * @param method the method
	 * @return the {@code ShadowMatch} to use for the specified expression and method
	 */
	@Nullable
	static ShadowMatch getShadowMatch(ExpressionPointcut expression, Method method) {
		return shadowMatchCache.get(new Key(expression, method));
	}

	/**
	 * Associate the {@link ShadowMatch} to the specified {@link ExpressionPointcut}
	 * and method. If an entry already exists, the given {@code shadowMatch} is
	 * ignored.
	 * @param expression the expression
	 * @param method the method
	 * @param shadowMatch the shadow match to use for this expression and method
	 * if none already exists
	 * @return the shadow match to use for the specified expression and method
	 */
	static ShadowMatch setShadowMatch(ExpressionPointcut expression, Method method, ShadowMatch shadowMatch) {
		ShadowMatch existing = shadowMatchCache.putIfAbsent(new Key(expression, method), shadowMatch);
		return (existing != null ? existing : shadowMatch);
	}


	private record Key(ExpressionPointcut expression, Method method) {}

}
