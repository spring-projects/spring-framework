/*
 * Copyright 2010 the original author or authors.
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
import org.springframework.util.Assert;

/**
 * Default implementation of expression root object. 
 * 
 * @author Costin Leau
 */
public class DefaultCacheExpressionRootObject implements CacheExpressionRootObject {

	private final String methodName;
	private final Collection<Cache<?, ?>> caches;

	public DefaultCacheExpressionRootObject(Collection<Cache<?,?>> caches, String methodName) {
		Assert.hasText(methodName, "method name is required");
		this.methodName = methodName;
		this.caches = caches;
	}

	public String getMethodName() {
		return methodName;
	}

	public Collection<Cache<?, ?>> getCaches() {
		return caches;
	}
}
