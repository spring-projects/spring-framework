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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.ObjectUtils;

/**
 * Inner class that implements a Pointcut that matches if the underlying
 * {@link CacheDefinitionSource} has an attribute for a given method.
 *
 * @author Costin Leau
 */
@SuppressWarnings("serial")
abstract class CacheDefinitionSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

	public boolean matches(Method method, Class<?> targetClass) {
		CacheDefinitionSource cas = getCacheDefinitionSource();
		return (cas == null || cas.getCacheDefinition(method, targetClass) != null);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CacheDefinitionSourcePointcut)) {
			return false;
		}
		CacheDefinitionSourcePointcut otherPc = (CacheDefinitionSourcePointcut) other;
		return ObjectUtils.nullSafeEquals(getCacheDefinitionSource(),
				otherPc.getCacheDefinitionSource());
	}

	@Override
	public int hashCode() {
		return CacheDefinitionSourcePointcut.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + getCacheDefinitionSource();
	}


	/**
	 * Obtain the underlying CacheOperationDefinitionSource (may be <code>null</code>).
	 * To be implemented by subclasses.
	 */
	protected abstract CacheDefinitionSource getCacheDefinitionSource();
}