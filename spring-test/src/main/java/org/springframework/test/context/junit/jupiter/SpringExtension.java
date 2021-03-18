/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.annotation.Testable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.ParameterResolutionDelegate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.support.PropertyProvider;
import org.springframework.test.context.support.TestConstructorUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * {@code SpringExtension} integrates the <em>Spring TestContext Framework</em>
 * into JUnit 5's <em>Jupiter</em> programming model.
 *
 * <p>To use this extension, simply annotate a JUnit Jupiter based test class with
 * {@code @ExtendWith(SpringExtension.class)}, {@code @SpringJUnitConfig}, or
 * {@code @SpringJUnitWebConfig}.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see org.springframework.test.context.junit.jupiter.EnabledIf
 * @see org.springframework.test.context.junit.jupiter.DisabledIf
 * @see org.springframework.test.context.junit.jupiter.SpringJUnitConfig
 * @see org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
 * @see org.springframework.test.context.TestContextManager
 */
public class SpringExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor,
		BeforeEachCallback, AfterEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback,
		ParameterResolver {

	/**
	 * {@link Namespace} in which {@code TestContextManagers} are stored, keyed
	 * by test class.
	 */
	private static final Namespace TEST_CONTEXT_MANAGER_NAMESPACE = Namespace.create(SpringExtension.class);

	/**
	 * {@link Namespace} in which {@code @Autowired} validation error messages
	 * are stored, keyed by test class.
	 */
	private static final Namespace AUTOWIRED_VALIDATION_NAMESPACE = Namespace.create(SpringExtension.class.getName() +
			"#autowired.validation");

	private static final String NO_AUTOWIRED_VIOLATIONS_DETECTED = "NO AUTOWIRED VIOLATIONS DETECTED";

	// Note that @Test, @TestFactory, @TestTemplate, @RepeatedTest, and @ParameterizedTest
	// are all meta-annotated with @Testable.
	private static final List<Class<? extends Annotation>> JUPITER_ANNOTATION_TYPES =
			Arrays.asList(BeforeAll.class, AfterAll.class, BeforeEach.class, AfterEach.class, Testable.class);

	private static final MethodFilter autowiredTestOrLifecycleMethodFilter =
			ReflectionUtils.USER_DECLARED_METHODS
				.and(method -> !Modifier.isPrivate(method.getModifiers()))
				.and(SpringExtension::isAutowiredTestOrLifecycleMethod);


	/**
	 * Delegates to {@link TestContextManager#beforeTestClass}.
	 */
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		getTestContextManager(context).beforeTestClass();
	}

	/**
	 * Delegates to {@link TestContextManager#afterTestClass}.
	 */
	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		try {
			getTestContextManager(context).afterTestClass();
		}
		finally {
			getStore(context).remove(context.getRequiredTestClass());
		}
	}

	/**
	 * Delegates to {@link TestContextManager#prepareTestInstance}.
	 * <p>As of Spring Framework 5.3.2, this method also validates that test
	 * methods and test lifecycle methods are not annotated with
	 * {@link Autowired @Autowired}.
	 */
	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		validateAutowiredConfig(context);
		getTestContextManager(context).prepareTestInstance(testInstance);
	}

	/**
	 * Validate that test methods and test lifecycle methods in the supplied
	 * test class are not annotated with {@link Autowired @Autowired}.
	 * @since 5.3.2
	 */
	private void validateAutowiredConfig(ExtensionContext context) {
		// We save the result in the ExtensionContext.Store so that we don't
		// re-validate all methods for the same test class multiple times.
		Store store = context.getStore(AUTOWIRED_VALIDATION_NAMESPACE);
		String errorMessage = store.getOrComputeIfAbsent(context.getRequiredTestClass(),
			testClass -> {
				Method[] methodsWithErrors =
						ReflectionUtils.getUniqueDeclaredMethods(testClass, autowiredTestOrLifecycleMethodFilter);
				return (methodsWithErrors.length == 0 ? NO_AUTOWIRED_VIOLATIONS_DETECTED :
						String.format(
							"Test methods and test lifecycle methods must not be annotated with @Autowired. " +
							"You should instead annotate individual method parameters with @Autowired, " +
							"@Qualifier, or @Value. Offending methods in test class %s: %s",
							testClass.getName(), Arrays.toString(methodsWithErrors)));
			}, String.class);

		if (errorMessage != NO_AUTOWIRED_VIOLATIONS_DETECTED) {
			throw new IllegalStateException(errorMessage);
		}
	}

	/**
	 * Delegates to {@link TestContextManager#beforeTestMethod}.
	 */
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		getTestContextManager(context).beforeTestMethod(testInstance, testMethod);
	}

	/**
	 * Delegates to {@link TestContextManager#beforeTestExecution}.
	 */
	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		getTestContextManager(context).beforeTestExecution(testInstance, testMethod);
	}

	/**
	 * Delegates to {@link TestContextManager#afterTestExecution}.
	 */
	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
		getTestContextManager(context).afterTestExecution(testInstance, testMethod, testException);
	}

	/**
	 * Delegates to {@link TestContextManager#afterTestMethod}.
	 */
	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
		getTestContextManager(context).afterTestMethod(testInstance, testMethod, testException);
	}

	/**
	 * Determine if the value for the {@link Parameter} in the supplied {@link ParameterContext}
	 * should be autowired from the test's {@link ApplicationContext}.
	 * <p>A parameter is considered to be autowirable if one of the following
	 * conditions is {@code true}.
	 * <ol>
	 * <li>The {@linkplain ParameterContext#getDeclaringExecutable() declaring
	 * executable} is a {@link Constructor} and
	 * {@link TestConstructorUtils#isAutowirableConstructor(Constructor, Class, PropertyProvider)}
	 * returns {@code true}. Note that {@code isAutowirableConstructor()} will be
	 * invoked with a fallback {@link PropertyProvider} that delegates its lookup
	 * to {@link ExtensionContext#getConfigurationParameter(String)}.</li>
	 * <li>The parameter is of type {@link ApplicationContext} or a sub-type thereof.</li>
	 * <li>The parameter is of type {@link ApplicationEvents} or a sub-type thereof.</li>
	 * <li>{@link ParameterResolutionDelegate#isAutowirable} returns {@code true}.</li>
	 * </ol>
	 * <p><strong>WARNING</strong>: If a test class {@code Constructor} is annotated
	 * with {@code @Autowired} or automatically autowirable (see {@link TestConstructor}),
	 * Spring will assume the responsibility for resolving all parameters in the
	 * constructor. Consequently, no other registered {@link ParameterResolver}
	 * will be able to resolve parameters.
	 * @see #resolveParameter
	 * @see TestConstructorUtils#isAutowirableConstructor(Constructor, Class)
	 * @see ParameterResolutionDelegate#isAutowirable
	 */
	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Parameter parameter = parameterContext.getParameter();
		Executable executable = parameter.getDeclaringExecutable();
		Class<?> testClass = extensionContext.getRequiredTestClass();
		PropertyProvider junitPropertyProvider = propertyName ->
				extensionContext.getConfigurationParameter(propertyName).orElse(null);
		return (TestConstructorUtils.isAutowirableConstructor(executable, testClass, junitPropertyProvider) ||
				ApplicationContext.class.isAssignableFrom(parameter.getType()) ||
				supportsApplicationEvents(parameterContext) ||
				ParameterResolutionDelegate.isAutowirable(parameter, parameterContext.getIndex()));
	}

	private boolean supportsApplicationEvents(ParameterContext parameterContext) {
		if (ApplicationEvents.class.isAssignableFrom(parameterContext.getParameter().getType())) {
			Assert.isTrue(parameterContext.getDeclaringExecutable() instanceof Method,
				"ApplicationEvents can only be injected into test and lifecycle methods");
			return true;
		}
		return false;
	}

	/**
	 * Resolve a value for the {@link Parameter} in the supplied {@link ParameterContext} by
	 * retrieving the corresponding dependency from the test's {@link ApplicationContext}.
	 * <p>Delegates to {@link ParameterResolutionDelegate#resolveDependency}.
	 * @see #supportsParameter
	 * @see ParameterResolutionDelegate#resolveDependency
	 */
	@Override
	@Nullable
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Parameter parameter = parameterContext.getParameter();
		int index = parameterContext.getIndex();
		Class<?> testClass = extensionContext.getRequiredTestClass();
		ApplicationContext applicationContext = getApplicationContext(extensionContext);
		return ParameterResolutionDelegate.resolveDependency(parameter, index, testClass,
				applicationContext.getAutowireCapableBeanFactory());
	}


	/**
	 * Get the {@link ApplicationContext} associated with the supplied {@code ExtensionContext}.
	 * @param context the current {@code ExtensionContext} (never {@code null})
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving the application context
	 * @see org.springframework.test.context.TestContext#getApplicationContext()
	 */
	public static ApplicationContext getApplicationContext(ExtensionContext context) {
		return getTestContextManager(context).getTestContext().getApplicationContext();
	}

	/**
	 * Get the {@link TestContextManager} associated with the supplied {@code ExtensionContext}.
	 * @return the {@code TestContextManager} (never {@code null})
	 */
	static TestContextManager getTestContextManager(ExtensionContext context) {
		Assert.notNull(context, "ExtensionContext must not be null");
		Class<?> testClass = context.getRequiredTestClass();
		Store store = getStore(context);
		return store.getOrComputeIfAbsent(testClass, TestContextManager::new, TestContextManager.class);
	}

	private static Store getStore(ExtensionContext context) {
		return context.getRoot().getStore(TEST_CONTEXT_MANAGER_NAMESPACE);
	}

	private static boolean isAutowiredTestOrLifecycleMethod(Method method) {
		MergedAnnotations mergedAnnotations =
				MergedAnnotations.from(method, SearchStrategy.DIRECT, RepeatableContainers.none());
		if (!mergedAnnotations.isPresent(Autowired.class)) {
			return false;
		}
		for (Class<? extends Annotation> annotationType : JUPITER_ANNOTATION_TYPES) {
			if (mergedAnnotations.isPresent(annotationType)) {
				return true;
			}
		}
		return false;
	}

}
