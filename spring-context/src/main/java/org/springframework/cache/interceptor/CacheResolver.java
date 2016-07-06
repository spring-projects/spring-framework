/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.util.Collection;

import org.springframework.cache.Cache;

/**
 * Determine the {@link Cache} instance(s) to use for an intercepted method invocation.
 *
 * <p>Implementations must be thread-safe.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@FunctionalInterface
public interface CacheResolver {

	/**
	 * Return the cache(s) to use for the specified invocation.
	 * @param context the context of the particular invocation
	 * @return the cache(s) to use (never {@code null})
	 */
	Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context);

}
