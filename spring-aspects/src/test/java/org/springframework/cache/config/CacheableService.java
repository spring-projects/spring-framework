/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.config;

/**
 * Copy of the shared {@code CacheableService}: necessary
 * due to issues with Gradle test fixtures and AspectJ configuration
 * in the Gradle build.
 *
 * <p>Basic service interface for caching tests.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public interface CacheableService<T> {

	T cache(Object arg1);

	T cacheNull(Object arg1);

	T cacheSync(Object arg1);

	T cacheSyncNull(Object arg1);

	void evict(Object arg1, Object arg2);

	void evictWithException(Object arg1);

	void evictEarly(Object arg1);

	void evictAll(Object arg1);

	void evictAllEarly(Object arg1);

	T conditional(int field);

	T conditionalSync(int field);

	T unless(int arg);

	T key(Object arg1, Object arg2);

	T varArgsKey(Object... args);

	T name(Object arg1);

	T nullValue(Object arg1);

	T update(Object arg1);

	T conditionalUpdate(Object arg2);

	Number nullInvocations();

	T rootVars(Object arg1);

	T customKeyGenerator(Object arg1);

	T unknownCustomKeyGenerator(Object arg1);

	T customCacheManager(Object arg1);

	T unknownCustomCacheManager(Object arg1);

	T throwChecked(Object arg1) throws Exception;

	T throwUnchecked(Object arg1);

	T throwCheckedSync(Object arg1) throws Exception;

	T throwUncheckedSync(Object arg1);

	T multiCache(Object arg1);

	T multiEvict(Object arg1);

	T multiCacheAndEvict(Object arg1);

	T multiConditionalCacheAndEvict(Object arg1);

	T multiUpdate(Object arg1);

	TestEntity putRefersToResult(TestEntity arg1);

}
