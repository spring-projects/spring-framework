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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.cache.Cache;
import org.springframework.util.Assert;

/**
 * Default implementation of expression root object. 
 * 
 * @author Costin Leau
 */
public class DefaultCacheExpressionRootObject implements CacheExpressionRootObject {

	private final Object target;
	private final Class<?> targetClass;
	private final String methodName;
	private final Method method;
	private final Collection<Cache> caches;
	private final Object[] args;

	public DefaultCacheExpressionRootObject(Collection<Cache> caches, Method method, Object[] args,
			Object target, Class<?> targetClass) {
		Assert.notNull(method, "method is required");
		Assert.notNull(targetClass, "targetClass is required");
		this.method = method;
		this.methodName = method.getName();
		this.target = target;
		this.targetClass = targetClass;
		this.args = args;
		this.caches = caches;
	}

	public String getMethodName() {
		return methodName;
	}

	public Collection<Cache> getCaches() {
		return caches;
	}

	public Method getMethod() {
		return method;
	}

	public Object[] getParams() {
		return args;
	}

	public Object getTarget() {
		return target;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}
}
