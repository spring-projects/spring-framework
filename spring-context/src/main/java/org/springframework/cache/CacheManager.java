/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

/**
 * Spring's central cache manager SPI.
 *
 * <p>Allows for retrieving named {@link Cache} regions.
 *
 * @author Costin Leau
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.1
 */
public interface CacheManager {

	/**
	 * Get the cache associated with the given name.
	 * <p>Note that the cache may be lazily created at runtime if the
	 * native provider supports it.
	 * @param name the cache identifier (must not be {@code null})
	 * @return the associated cache, or {@code null} if such a cache
	 * does not exist or could be not created
	 */
	@Nullable Cache getCache(String name);

	/**
	 * Get a collection of the cache names known by this manager.
	 * @return the names of all caches known by the cache manager
	 */
	Collection<String> getCacheNames();

	/**
	 * Remove all registered caches from this cache manager if possible,
	 * re-creating them on demand. After this call, {@link #getCacheNames()}
	 * will possibly be empty and the cache provider will have dropped all
	 * cache management state.
	 * <p>Alternatively, an implementation may perform an equivalent reset
	 * on fixed existing cache regions without actually dropping the cache.
	 * This behavior will be indicated by {@link #getCacheNames()} still
	 * exposing a non-empty set of names, whereas the corresponding cache
	 * regions will not contain cache entries anymore.
	 * <p>The default implementation calls {@link Cache#clear} on all
	 * registered caches, retaining all caches as registered, satisfying
	 * the alternative implementation path above. Custom implementations
	 * may either drop the actual caches (re-creating them on demand) or
	 * perform a more exhaustive reset at the actual cache provider level.
	 * @since 7.0.2
	 * @see Cache#clear()
	 */
	default void resetCaches() {
		for (String cacheName : getCacheNames()) {
			Cache cache = getCache(cacheName);
			if (cache != null) {
				cache.clear();
			}
		}
	}

}
