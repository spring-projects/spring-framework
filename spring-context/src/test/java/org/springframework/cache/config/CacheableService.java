/*
 * Copyright 2010-2011 the original author or authors.
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

package org.springframework.cache.config;

/**
 * Basic service interface.
 *
 * @author Costin Leau
 */
public interface CacheableService<T> {

	T cache(Object arg1);

	void invalidate(Object arg1);

	void evictEarly(Object arg1);

	void evictAll(Object arg1);

	void evictWithException(Object arg1);

	void evict(Object arg1, Object arg2);

	void invalidateEarly(Object arg1, Object arg2);

	T conditional(int field);

	T key(Object arg1, Object arg2);

	T name(Object arg1);

	T nullValue(Object arg1);

	T update(Object arg1);

	T conditionalUpdate(Object arg2);

	Number nullInvocations();

	T rootVars(Object arg1);

	T throwChecked(Object arg1) throws Exception;

	T throwUnchecked(Object arg1);

	// multi annotations
	T multiCache(Object arg1);

	T multiEvict(Object arg1);

	T multiCacheAndEvict(Object arg1);

	T multiConditionalCacheAndEvict(Object arg1);

	T multiUpdate(Object arg1);
}
