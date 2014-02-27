/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.jcache.model;

import static java.util.Arrays.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheValue;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.Assert;
import org.springframework.util.filter.ExceptionTypeFilter;

/**
 * A base {@link JCacheOperation} implementation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public abstract class BaseCacheOperation<A extends Annotation> implements JCacheOperation<A> {

	private final CacheMethodDetails<A> methodDetails;

	private final CacheResolver cacheResolver;

	protected final List<CacheParameterDetail> allParameterDetails;

	/**
	 * Create a new instance.
	 * @param methodDetails the {@link CacheMethodDetails} related to the cached method
	 * @param cacheResolver the cache resolver to resolve regular caches
	 */
	protected BaseCacheOperation(CacheMethodDetails<A> methodDetails, CacheResolver cacheResolver) {
		Assert.notNull(methodDetails, "method details must not be null.");
		Assert.notNull(cacheResolver, "cache resolver must not be null.");
		this.methodDetails = methodDetails;
		this.cacheResolver = cacheResolver;
		this.allParameterDetails = initializeAllParameterDetails(methodDetails.getMethod());
	}

	/**
	 * Return the {@link ExceptionTypeFilter} to use to filter exceptions thrown while
	 * invoking the method.
	 */
	public abstract ExceptionTypeFilter getExceptionTypeFilter();

	@Override
	public Method getMethod() {
		return methodDetails.getMethod();
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return methodDetails.getAnnotations();
	}

	@Override
	public A getCacheAnnotation() {
		return methodDetails.getCacheAnnotation();
	}

	@Override
	public String getCacheName() {
		return methodDetails.getCacheName();
	}

	@Override
	public Set<String> getCacheNames() {
		return Collections.singleton(getCacheName());
	}

	@Override
	public CacheResolver getCacheResolver() {
		return cacheResolver;
	}

	@Override
	public CacheInvocationParameter[] getAllParameters(Object... values) {
		if (allParameterDetails.size() != values.length) {
			throw new IllegalStateException("Values mismatch, operation has "
					+ allParameterDetails.size() + " parameter(s) but got " + values.length + " value(s)");
		}
		List<CacheInvocationParameter> result = new ArrayList<CacheInvocationParameter>();
		for (int i = 0; i < allParameterDetails.size(); i++) {
			result.add(allParameterDetails.get(i).toCacheInvocationParameter(values[i]));
		}
		return result.toArray(new CacheInvocationParameter[result.size()]);
	}

	protected ExceptionTypeFilter createExceptionTypeFiler(Class<? extends Throwable>[] includes,
			Class<? extends Throwable>[] excludes) {
		return new ExceptionTypeFilter(asList(includes), asList(excludes), true);
	}


	private static List<CacheParameterDetail> initializeAllParameterDetails(Method method) {
		List<CacheParameterDetail> result = new ArrayList<CacheParameterDetail>();
		for (int i = 0; i < method.getParameterTypes().length; i++) {
			CacheParameterDetail detail = new CacheParameterDetail(method, i);
			result.add(detail);
		}
		return result;
	}

	@Override
	public String toString() {
		return getOperationDescription().append("]").toString();
	}

	/**
	 * Return an identifying description for this caching operation.
	 * <p>Available to subclasses, for inclusion in their {@code toString()} result.
	 */
	protected StringBuilder getOperationDescription() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append("[");
		result.append(this.methodDetails);
		return result;
	}


	protected static class CacheParameterDetail {

		private final Class<?> rawType;

		private final Set<Annotation> annotations;

		private final int parameterPosition;

		private final boolean isKey;

		private final boolean isValue;

		private CacheParameterDetail(Method m, int parameterPosition) {
			this.rawType = m.getParameterTypes()[parameterPosition];
			this.annotations = new LinkedHashSet<Annotation>();
			boolean foundKeyAnnotation = false;
			boolean foundValueAnnotation = false;
			for (Annotation annotation : m.getParameterAnnotations()[parameterPosition]) {
				annotations.add(annotation);
				if (CacheKey.class.isAssignableFrom(annotation.annotationType())) {
					foundKeyAnnotation = true;
				}
				if (CacheValue.class.isAssignableFrom(annotation.annotationType())) {
					foundValueAnnotation = true;
				}
			}
			this.parameterPosition = parameterPosition;
			this.isKey = foundKeyAnnotation;
			this.isValue = foundValueAnnotation;
		}

		public int getParameterPosition() {
			return parameterPosition;
		}

		protected boolean isKey() {
			return isKey;
		}

		protected boolean isValue() {
			return isValue;
		}

		public CacheInvocationParameter toCacheInvocationParameter(Object value) {
			return new CacheInvocationParameterImpl(this, value);
		}
	}

	protected static class CacheInvocationParameterImpl implements CacheInvocationParameter {

		private final CacheParameterDetail detail;

		private final Object value;

		private CacheInvocationParameterImpl(CacheParameterDetail detail, Object value) {
			this.detail = detail;
			this.value = value;
		}

		@Override
		public Class<?> getRawType() {
			return detail.rawType;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public Set<Annotation> getAnnotations() {
			return detail.annotations;
		}

		@Override
		public int getParameterPosition() {
			return detail.parameterPosition;
		}
	}

}
