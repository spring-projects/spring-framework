/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.cache.jcache.config;

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
