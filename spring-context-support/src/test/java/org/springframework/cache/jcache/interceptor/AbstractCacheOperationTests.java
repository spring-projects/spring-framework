/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import org.junit.jupiter.api.Test;

import org.springframework.cache.jcache.AbstractJCacheTests;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractCacheOperationTests<O extends JCacheOperation<?>> extends AbstractJCacheTests {

	protected final SampleObject sampleInstance = new SampleObject();

	protected abstract O createSimpleOperation();


	@Test
	public void simple() {
		O operation = createSimpleOperation();
		assertThat(operation.getCacheName()).as("Wrong cache name").isEqualTo("simpleCache");
		assertThat(operation.getAnnotations()).as("Unexpected number of annotation on " + operation.getMethod())
				.hasSize(1);
		assertThat(operation.getAnnotations().iterator().next()).as("Wrong method annotation").isEqualTo(operation.getCacheAnnotation());

		assertThat(operation.getCacheResolver()).as("cache resolver should be set").isNotNull();
	}

	protected void assertCacheInvocationParameter(CacheInvocationParameter actual, Class<?> targetType,
			Object value, int position) {
		assertThat(actual.getRawType()).as("wrong parameter type for " + actual).isEqualTo(targetType);
		assertThat(actual.getValue()).as("wrong parameter value for " + actual).isEqualTo(value);
		assertThat(actual.getParameterPosition()).as("wrong parameter position for " + actual).isEqualTo(position);
	}

	protected <A extends Annotation> CacheMethodDetails<A> create(Class<A> annotationType,
			Class<?> targetType, String methodName,
			Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(targetType, methodName, parameterTypes);
		Assert.notNull(method, () -> "requested method '" + methodName + "'does not exist");
		A cacheAnnotation = method.getAnnotation(annotationType);
		return new DefaultCacheMethodDetails<>(method, cacheAnnotation, getCacheName(cacheAnnotation));
	}

	private static String getCacheName(Annotation annotation) {
		Object cacheName = AnnotationUtils.getValue(annotation, "cacheName");
		return cacheName != null ? cacheName.toString() : "test";
	}

}
