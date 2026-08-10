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
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ParameterResolutionDelegate.isAutowirable(null, 0))
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
		int startIndex = (executable instanceof Constructor) &&
				ClassUtils.isInnerClass(executable.getDeclaringClass()) ? 1 : 0;
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
			assertThat(ParameterResolutionDelegate.isAutowirable(parameter, parameterIndex))
					.as("Parameter " + parameter + " must not be autowirable").isFalse();
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
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ParameterResolutionDelegate.resolveDependency(getParameter(), 0, null, null))
				.withMessageContaining("Containing class must not be null");
	}

	@Test
	void resolveDependencyPreconditionsForBeanFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ParameterResolutionDelegate.resolveDependency(getParameter(), 0, getClass(), null))
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
			assertThat(intermediateDependencyDescriptor.usesStandardBeanLookup()).isTrue();
		}
	}

	@Test
	void resolveDependencyWithCustomParameterNamePreconditionsForParameter() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ParameterResolutionDelegate.resolveDependency(null, 0, "customName", getClass(), mock()))
				.withMessageContaining("Parameter must not be null");
	}

	@Test
	void resolveDependencyWithCustomParameterNamePreconditionsForContainingClass() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ParameterResolutionDelegate.resolveDependency(getParameter(), 0, "customName", null, mock()))
				.withMessageContaining("Containing class must not be null");
	}

	@Test
	void resolveDependencyWithCustomParameterNamePreconditionsForBeanFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ParameterResolutionDelegate.resolveDependency(getParameter(), 0, "customName", getClass(), null))
				.withMessageContaining("AutowireCapableBeanFactory must not be null");
	}

	@Test
	void resolveDependencyWithNullCustomParameterNameFallsBackToDefaultParameterNameDiscovery() throws Exception {
		Constructor<?> constructor = AutowirableClass.class.getConstructor(String.class, String.class, String.class, String.class);
		AutowireCapableBeanFactory beanFactory = mock();
		given(beanFactory.resolveDependency(any(), isNull())).willAnswer(invocation -> invocation.getArgument(0));

		Parameter[] parameters = constructor.getParameters();
		for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
			Parameter parameter = parameters[parameterIndex];
			DependencyDescriptor via4ArgMethod = (DependencyDescriptor) ParameterResolutionDelegate.resolveDependency(
					parameter, parameterIndex, AutowirableClass.class, beanFactory);
			DependencyDescriptor via5ArgMethod = (DependencyDescriptor) ParameterResolutionDelegate.resolveDependency(
					parameter, parameterIndex, null, AutowirableClass.class, beanFactory);
			assertThat(via5ArgMethod.getDependencyName()).isEqualTo(via4ArgMethod.getDependencyName());
		}
	}

	@Test
	void resolveDependencyWithCustomParameterName() throws Exception {
		Constructor<?> constructor = AutowirableClass.class.getConstructor(String.class, String.class, String.class, String.class);
		AutowireCapableBeanFactory beanFactory = mock();
		given(beanFactory.resolveDependency(any(), isNull())).willAnswer(invocation -> invocation.getArgument(0));

		Parameter parameter = constructor.getParameters()[0];
		DependencyDescriptor descriptor = (DependencyDescriptor) ParameterResolutionDelegate.resolveDependency(
				parameter, 0, "customBeanName", AutowirableClass.class, beanFactory);

		assertThat(descriptor.getAnnotatedElement()).isEqualTo(constructor);
		assertThat(descriptor.getMethodParameter().getParameter()).isEqualTo(parameter);
		assertThat(descriptor.getDependencyName()).isEqualTo("customBeanName");
		assertThat(descriptor.usesStandardBeanLookup()).isTrue();
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
