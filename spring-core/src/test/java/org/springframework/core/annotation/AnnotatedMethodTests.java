/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotatedMethod}.
 *
 * @author Sam Brannen
 * @since 6.2.11
 */
class AnnotatedMethodTests {

	@Test
	void shouldFindAnnotationOnMethodInGenericAbstractSuperclass() {
		Method processTwo = getMethod("processTwo", String.class);

		AnnotatedMethod annotatedMethod = new AnnotatedMethod(processTwo);

		assertThat(annotatedMethod.hasMethodAnnotation(Handler.class)).isTrue();
	}

	@Test
	void shouldFindAnnotationOnMethodInGenericInterface() {
		Method processOneAndTwo = getMethod("processOneAndTwo", Long.class, Object.class);

		AnnotatedMethod annotatedMethod = new AnnotatedMethod(processOneAndTwo);

		assertThat(annotatedMethod.hasMethodAnnotation(Handler.class)).isTrue();
	}

	@Test
	void shouldFindAnnotationOnMethodParameterInGenericAbstractSuperclass() {
		Method processTwo = getMethod("processTwo", String.class);

		AnnotatedMethod annotatedMethod = new AnnotatedMethod(processTwo);
		MethodParameter[] methodParameters = annotatedMethod.getMethodParameters();

		assertThat(methodParameters).hasSize(1);
		assertThat(methodParameters[0].hasParameterAnnotation(Param.class)).isTrue();
	}

	@Test
	void shouldFindAnnotationOnMethodParameterInGenericInterface() {
		Method processOneAndTwo = getMethod("processOneAndTwo", Long.class, Object.class);

		AnnotatedMethod annotatedMethod = new AnnotatedMethod(processOneAndTwo);
		MethodParameter[] methodParameters = annotatedMethod.getMethodParameters();

		assertThat(methodParameters).hasSize(2);
		assertThat(methodParameters[0].hasParameterAnnotation(Param.class)).isFalse();
		assertThat(methodParameters[1].hasParameterAnnotation(Param.class)).isTrue();
	}


	private static Method getMethod(String name, Class<?>...parameterTypes) {
		return ClassUtils.getMethod(GenericInterfaceImpl.class, name, parameterTypes);
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface Handler {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Param {
	}

	interface GenericInterface<A, B> {

		@Handler
		void processOneAndTwo(A value1, @Param B value2);
	}

	abstract static class GenericAbstractSuperclass<C> implements GenericInterface<Long, C> {

		@Override
		public void processOneAndTwo(Long value1, C value2) {
		}

		@Handler
		public abstract void processTwo(@Param C value);
	}

	static class GenericInterfaceImpl extends GenericAbstractSuperclass<String> {

		@Override
		public void processTwo(String value) {
		}
	}

}
