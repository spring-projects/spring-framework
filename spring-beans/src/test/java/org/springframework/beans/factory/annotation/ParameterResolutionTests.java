/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ParameterResolutionDelegate}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Loïc Ledoyen
 */
public class ParameterResolutionTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void isAutowirablePreconditions() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Parameter must not be null");
		ParameterResolutionDelegate.isAutowirable(null, 0);
	}

	@Test
	public void annotatedParametersInMethodAreCandidatesForAutowiring() throws Exception {
		Method method = getClass().getDeclaredMethod("autowirableMethod", String.class, String.class, String.class, String.class);
		assertAutowirableParameters(method);
	}

	@Test
	public void annotatedParametersInTopLevelClassConstructorAreCandidatesForAutowiring() throws Exception {
		Constructor<?> constructor = AutowirableClass.class.getConstructor(String.class, String.class, String.class, String.class);
		assertAutowirableParameters(constructor);
	}

	@Test
	public void annotatedParametersInInnerClassConstructorAreCandidatesForAutowiring() throws Exception {
		Class<?> innerClass = AutowirableClass.InnerAutowirableClass.class;
		assertTrue(ClassUtils.isInnerClass(innerClass));
		Constructor<?> constructor = innerClass.getConstructor(AutowirableClass.class, String.class, String.class);
		assertAutowirableParameters(constructor);
	}

	private void assertAutowirableParameters(Executable executable) {
		int startIndex = (executable instanceof Constructor)
				&& ClassUtils.isInnerClass(executable.getDeclaringClass()) ? 1 : 0;
		Parameter[] parameters = executable.getParameters();
		for (int parameterIndex = startIndex; parameterIndex < parameters.length; parameterIndex++) {
			Parameter parameter = parameters[parameterIndex];
			assertTrue("Parameter " + parameter + " must be autowirable",
					ParameterResolutionDelegate.isAutowirable(parameter, parameterIndex));
		}
	}

	@Test
	public void nonAnnotatedParametersInTopLevelClassConstructorAreNotCandidatesForAutowiring() throws Exception {
		Constructor<?> notAutowirableConstructor = AutowirableClass.class.getConstructor(String.class);

		Parameter[] parameters = notAutowirableConstructor.getParameters();
		for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
			Parameter parameter = parameters[parameterIndex];
			assertFalse("Parameter " + parameter + " must not be autowirable",
					ParameterResolutionDelegate.isAutowirable(parameter, parameterIndex));
		}
	}

	@Test
	public void resolveDependencyPreconditionsForParameter() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Parameter must not be null");
		ParameterResolutionDelegate.resolveDependency(null, 0, null, mock(AutowireCapableBeanFactory.class));
	}

	@Test
	public void resolveDependencyPreconditionsForContainingClass() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Containing class must not be null");
		ParameterResolutionDelegate.resolveDependency(getParameter(), 0, null, null);
	}

	@Test
	public void resolveDependencyPreconditionsForBeanFactory() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("AutowireCapableBeanFactory must not be null");
		ParameterResolutionDelegate.resolveDependency(getParameter(), 0, getClass(), null);
	}

	private Parameter getParameter() throws NoSuchMethodException {
		Method method = getClass().getDeclaredMethod("autowirableMethod", String.class, String.class, String.class, String.class);
		return method.getParameters()[0];
	}

	@Test
	public void resolveDependencyForAnnotatedParametersInTopLevelClassConstructor() throws Exception {
		Constructor<?> constructor = AutowirableClass.class.getConstructor(String.class, String.class, String.class, String.class);

		AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
		// Configure the mocked BeanFactory to return the DependencyDescriptor for convenience and
		// to avoid using an ArgumentCaptor.
		when(beanFactory.resolveDependency(any(), isNull())).thenAnswer(invocation -> invocation.getArgument(0));

		Parameter[] parameters = constructor.getParameters();
		for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
			Parameter parameter = parameters[parameterIndex];
			DependencyDescriptor intermediateDependencyDescriptor = (DependencyDescriptor) ParameterResolutionDelegate.resolveDependency(
					parameter, parameterIndex, AutowirableClass.class, beanFactory);
			assertEquals(constructor, intermediateDependencyDescriptor.getAnnotatedElement());
			assertEquals(parameter, intermediateDependencyDescriptor.getMethodParameter().getParameter());
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
