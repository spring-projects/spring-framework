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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.cache.annotation.CacheMethodDetails;

/**
 * The default {@link CacheMethodDetails} implementation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class DefaultCacheMethodDetails<A extends Annotation> implements CacheMethodDetails<A> {

	private final Method method;

	private final Set<Annotation> annotations;

	private final A cacheAnnotation;

	private final String cacheName;

	public DefaultCacheMethodDetails(Method method, A cacheAnnotation,
			String cacheName) {
		this.method = method;
		this.annotations = Collections.unmodifiableSet(
				new LinkedHashSet<Annotation>(asList(method.getAnnotations())));
		this.cacheAnnotation = cacheAnnotation;
		this.cacheName = cacheName;
	}

	@Override
	public Method getMethod() {
		return method;
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return annotations;
	}

	@Override
	public A getCacheAnnotation() {
		return cacheAnnotation;
	}

	@Override
	public String getCacheName() {
		return cacheName;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("details[");
		sb.append("method=").append(method);
		sb.append(", cacheAnnotation=").append(cacheAnnotation);
		sb.append(", cacheName='").append(cacheName).append('\'');
		sb.append(']');
		return sb.toString();
	}

}
