/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.cache;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cache.support.AbstractValueAdaptingCache;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractValueAdaptingCacheTests<T extends AbstractValueAdaptingCache>
		extends AbstractCacheTests<T>  {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected final static String CACHE_NAME_NO_NULL = "testCacheNoNull";

	protected abstract T getCache(boolean allowNull);

	@Test
	public void testCachePutNullValueAllowNullFalse() {
		T cache = getCache(false);
		String key = createRandomKey();

		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(CACHE_NAME_NO_NULL);
		this.thrown.expectMessage(
				"is configured to not allow null values but null was provided");
		cache.put(key, null);
	}

}
