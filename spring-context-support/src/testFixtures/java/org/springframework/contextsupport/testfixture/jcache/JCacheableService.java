/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.contextsupport.testfixture.jcache;

import java.io.IOException;

/**
 * @author Stephane Nicoll
 */
public interface JCacheableService<T> {

	T cache(String id);

	T cacheNull(String id);

	T cacheWithException(String id, boolean matchFilter);

	T cacheWithCheckedException(String id, boolean matchFilter) throws IOException;

	T cacheAlwaysInvoke(String id);

	T cacheWithPartialKey(String id, boolean notUsed);

	T cacheWithCustomCacheResolver(String id);

	T cacheWithCustomKeyGenerator(String id, String anotherId);

	void put(String id, Object value);

	void putWithException(String id, Object value, boolean matchFilter);

	void earlyPut(String id, Object value);

	void earlyPutWithException(String id, Object value, boolean matchFilter);

	void remove(String id);

	void removeWithException(String id, boolean matchFilter);

	void earlyRemove(String id);

	void earlyRemoveWithException(String id, boolean matchFilter);

	void removeAll();

	void removeAllWithException(boolean matchFilter);

	void earlyRemoveAll();

	void earlyRemoveAllWithException(boolean matchFilter);

	long exceptionInvocations();

}
