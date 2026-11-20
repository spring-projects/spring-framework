/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
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
		// Prerequisites for gh-35349
		Method abstractMethod = ReflectionUtils.findMethod(GenericAbstractSuperclass.class, "processTwo", Object.class);
		assertThat(abstractMethod).isNotNull();
		assertThat(Modifier.isAbstract(abstractMethod.getModifiers())).as("abstract").isTrue();
		assertThat(Modifier.isPublic(abstractMethod.getModifiers())).as("public").isFalse();

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
		Class<?> clazz = GenericInterfaceImpl.class;
		Method method = ReflectionUtils.findMethod(clazz, name, parameterTypes);
		if (method == null) {
			String parameterNames = stream(parameterTypes).map(Class::getName).collect(joining(", "));
			throw new IllegalStateException("Expected method not found: %s#%s(%s)"
					.formatted(clazz.getSimpleName(), name, parameterNames));
		}
		return method;
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
		// Intentionally NOT public
		abstract void processTwo(@Param C value);
	}

	static class GenericInterfaceImpl extends GenericAbstractSuperclass<String> {

		@Override
		void processTwo(String value) {
		}
	}

}
