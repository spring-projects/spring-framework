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

package org.springframework.aop.aspectj;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.weaver.tools.ShadowMatch;
import org.jspecify.annotations.Nullable;

/**
 * Internal {@link ShadowMatch} utilities.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 6.2
 */
public abstract class ShadowMatchUtils {

	private static final Map<Object, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(256);


	/**
	 * Find a {@link ShadowMatch} for the specified key.
	 * @param key the key to use
	 * @return the {@code ShadowMatch} to use for the specified key,
	 * or {@code null} if none found
	 */
	static @Nullable ShadowMatch getShadowMatch(Object key) {
		return shadowMatchCache.get(key);
	}

	/**
	 * Associate the {@link ShadowMatch} with the specified key.
	 * If an entry already exists, the given {@code shadowMatch} is ignored.
	 * @param key the key to use
	 * @param shadowMatch the shadow match to use for this key
	 * if none already exists
	 * @return the shadow match to use for the specified key
	 */
	static ShadowMatch setShadowMatch(Object key, ShadowMatch shadowMatch) {
		ShadowMatch existing = shadowMatchCache.putIfAbsent(key, shadowMatch);
		return (existing != null ? existing : shadowMatch);
	}

	/**
	 * Clear the cache of computed {@link ShadowMatch} instances.
	 */
	public static void clearCache() {
		shadowMatchCache.clear();
	}

}
