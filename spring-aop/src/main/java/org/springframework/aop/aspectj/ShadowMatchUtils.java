/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
