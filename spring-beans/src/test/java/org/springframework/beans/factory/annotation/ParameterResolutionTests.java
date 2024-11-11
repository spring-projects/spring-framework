/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ParameterResolutionDelegate}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Loïc Ledoyen
 */
class ParameterResolutionTests {

	@Test
	void isAutowirablePreconditions() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ParameterResolutionDelegate.isAutowirable(null, 0))
			.withMessageContaining("Parameter must not be null");
	}

	@Test
	void annotatedParametersInMethodAreCandidatesForAutowiring() throws Exception {
		Method method = getClass().getDeclaredMethod("autowirableMethod", String.class, String.class, String.class, String.class);
		assertAutowirableParameters(method);
	}

	@Test
	void annotatedParametersInTopLevelClassConstructorAreCandidatesForAutowiring() throws Exception {
		Constructor<?> constructor = AutowirableClass.class.getConstructor(String.class, String.class, String.class, String.class);
		assertAutowirableParameters(constructor);
	}

	@Test
	void annotatedParametersInInnerClassConstructorAreCandidatesForAutowiring() throws Exception {
		Class<?> innerClass = AutowirableClass.InnerAutowirableClass.class;
		assertThat(ClassUtils.isInnerClass(innerClass)).isTrue();
		Constructor<?> constructor = innerClass.getConstructor(AutowirableClass.class, String.class, String.class);
		assertAutowirableParameters(constructor);
	}

	private void assertAutowirableParameters(Executable executable) {
		int startIndex = (executable instanceof Constructor)
				&& ClassUtils.isInnerClass(executable.getDeclaringClass()) ? 1 : 0;
		Parameter[] parameters = executable.getParameters();
		for (int parameterIndex = startIndex; parameterIndex < parameters.length; parameterIndex++) {
			Parameter parameter = parameters[parameterIndex];
			assertThat(ParameterResolutionDelegate.isAutowirable(parameter, parameterIndex)).as("Parameter " + parameter + " must be autowirable").isTrue();
		}
	}

	@Test
	void nonAnnotatedParametersInTopLevelClassConstructorAreNotCandidatesForAutowiring() throws Exception {
		Constructor<?> notAutowirableConstructor = AutowirableClass.class.getConstructor(String.class);

		Parameter[] parameters = notAutowirableConstructor.getParameters();
		for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
			Parameter parameter = parameters[parameterIndex];
			assertThat(ParameterResolutionDelegate.isAutowirable(parameter, parameterIndex)).as("Parameter " + parameter + " must not be autowirable").isFalse();
		}
	}

	@Test
	void resolveDependencyPreconditionsForParameter() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ParameterResolutionDelegate.resolveDependency(null, 0, null, mock()))
			.withMessageContaining("Parameter must not be null");
	}

	@Test
	void resolveDependencyPreconditionsForContainingClass() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ParameterResolutionDelegate.resolveDependency(getParameter(), 0, null, null))
			.withMessageContaining("Containing class must not be null");
	}

	@Test
	void resolveDependencyPreconditionsForBeanFactory() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ParameterResolutionDelegate.resolveDependency(getParameter(), 0, getClass(), null))
			.withMessageContaining("AutowireCapableBeanFactory must not be null");
	}

	private Parameter getParameter() throws NoSuchMethodException {
		Method method = getClass().getDeclaredMethod("autowirableMethod", String.class, String.class, String.class, String.class);
		return method.getParameters()[0];
	}

	@Test
	void resolveDependencyForAnnotatedParametersInTopLevelClassConstructor() throws Exception {
		Constructor<?> constructor = AutowirableClass.class.getConstructor(String.class, String.class, String.class, String.class);

		AutowireCapableBeanFactory beanFactory = mock();
		// Configure the mocked BeanFactory to return the DependencyDescriptor for convenience and
		// to avoid using an ArgumentCaptor.
		given(beanFactory.resolveDependency(any(), isNull())).willAnswer(invocation -> invocation.getArgument(0));

		Parameter[] parameters = constructor.getParameters();
		for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
			Parameter parameter = parameters[parameterIndex];
			DependencyDescriptor intermediateDependencyDescriptor = (DependencyDescriptor) ParameterResolutionDelegate.resolveDependency(
					parameter, parameterIndex, AutowirableClass.class, beanFactory);
			assertThat(intermediateDependencyDescriptor.getAnnotatedElement()).isEqualTo(constructor);
			assertThat(intermediateDependencyDescriptor.getMethodParameter().getParameter()).isEqualTo(parameter);
		}
	}


	void autowirableMethod(
			@Autowired String firstParameter,
			@Qualifier("someQualifier") String secondParameter,
			@Value("${someValue}") String thirdParameter,
			@Autowired(required = false) String fourthParameter) {
	}


	public static class AutowirableClass {

		public AutowirableClass(@Autowired String firstParameter,
				@Qualifier("someQualifier") String secondParameter,
				@Value("${someValue}") String thirdParameter,
				@Autowired(required = false) String fourthParameter) {
		}

		public AutowirableClass(String notAutowirableParameter) {
		}

		public class InnerAutowirableClass {

			public InnerAutowirableClass(@Autowired String firstParameter,
					@Qualifier("someQualifier") String secondParameter) {
			}
		}
	}

}
