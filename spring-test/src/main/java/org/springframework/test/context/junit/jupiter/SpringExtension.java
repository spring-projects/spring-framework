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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance.Lifecycle;
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
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.commons.annotation.Testable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.ParameterResolutionDelegate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.test.context.MethodInvoker;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.support.PropertyProvider;
import org.springframework.test.context.support.TestConstructorUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * {@code SpringExtension} integrates the <em>Spring TestContext Framework</em>
 * into the JUnit Jupiter testing framework.
 *
 * <p>To use this extension, annotate a JUnit Jupiter based test class with
 * {@code @ExtendWith(SpringExtension.class)}, {@code @SpringJUnitConfig},
 * {@code @SpringJUnitWebConfig}, or any other annotation that is meta-annotated
 * with {@code @ExtendWith(SpringExtension.class)} such as {@code @SpringBootTest},
 * etc.
 *
 * <p>As of Spring Framework 7.0, the {@code SpringExtension} is
 * {@linkplain #getTestInstantiationExtensionContextScope(ExtensionContext)
 * configured} to use a test-method scoped {@link ExtensionContext}, which
 * enables consistent dependency injection into fields and constructors from the
 * {@link ApplicationContext} for the current test method in a
 * {@link org.junit.jupiter.api.Nested @Nested} test class hierarchy. However,
 * if a third-party {@link org.junit.platform.launcher.TestExecutionListener
 * TestExecutionListener} is not compatible with the semantics associated with
 * a test-method scoped extension context &mdash; or if a developer wishes to
 * switch to test-class scoped semantics &mdash; the {@code SpringExtension} can
 * be configured to use a test-class scoped extension context by annotating a
 * top-level test class with
 * {@link SpringExtensionConfig#useTestClassScopedExtensionContext()
 * &#64;SpringExtensionConfig(useTestClassScopedExtensionContext = true)}.
 *
 * @author Sam Brannen
 * @author Simon Baslé
 * @since 5.0
 * @see org.junit.jupiter.api.extension.ExtendWith @ExtendWith
 * @see org.springframework.test.context.junit.jupiter.SpringExtensionConfig @SpringExtensionConfig
 * @see org.springframework.test.context.junit.jupiter.SpringJUnitConfig @SpringJUnitConfig
 * @see org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig @SpringJUnitWebConfig
 * @see org.springframework.test.context.junit.jupiter.EnabledIf @EnabledIf
 * @see org.springframework.test.context.junit.jupiter.DisabledIf @DisabledIf
 * @see org.springframework.test.context.TestContextManager TestContextManager
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
	private static final Namespace AUTOWIRED_VALIDATION_NAMESPACE =
			Namespace.create(SpringExtension.class.getName() + "#autowired.validation");

	/**
	 * <em>Marker</em> string constant to represent that no violations were detected.
	 * <p>The value is an empty string which allows this class to perform quick
	 * {@code isEmpty()} checks instead of performing unnecessary string comparisons.
	 */
	private static final String NO_VIOLATIONS_DETECTED = "";

	/**
	 * {@link Namespace} in which {@code @RecordApplicationEvents} validation error messages
	 * are stored, keyed by test class.
	 */
	private static final Namespace RECORD_APPLICATION_EVENTS_VALIDATION_NAMESPACE =
			Namespace.create(SpringExtension.class.getName() + "#recordApplicationEvents.validation");

	/**
	 * LRU cache for {@link SpringExtensionConfig#useTestClassScopedExtensionContext()}
	 * mappings, keyed by test class.
	 * @since 7.0
	 */
	private static final ConcurrentLruCache<Class<?>, Boolean> useTestClassScopedExtensionContextCache =
			new ConcurrentLruCache<>(32, SpringExtension::useTestClassScopedExtensionContext);

	// Note that @Test, @TestFactory, @TestTemplate, @RepeatedTest, and @ParameterizedTest
	// are all meta-annotated with @Testable.
	private static final List<Class<? extends Annotation>> JUPITER_ANNOTATION_TYPES =
			List.of(BeforeAll.class, AfterAll.class, BeforeEach.class, AfterEach.class, Testable.class);

	private static final MethodFilter autowiredTestOrLifecycleMethodFilter =
			ReflectionUtils.USER_DECLARED_METHODS.and(SpringExtension::isAutowiredTestOrLifecycleMethod);


	/**
	 * Returns {@link ExtensionContextScope#TEST_METHOD ExtensionContextScope.TEST_METHOD}.
	 * <p>This can be effectively overridden by annotating a test class with
	 * {@code @SpringExtensionConfig(useTestClassScopedExtensionContext = true)}.
	 * See the {@linkplain SpringExtension class-level Javadoc} for further details.
	 * @since 7.0
	 * @see SpringExtensionConfig#useTestClassScopedExtensionContext()
	 */
	@Override
	public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
		return ExtensionContextScope.TEST_METHOD;
	}

	/**
	 * Delegates to {@link TestContextManager#beforeTestClass}.
	 */
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		TestContextManager testContextManager = getTestContextManager(context);
		registerMethodInvoker(testContextManager, context);
		testContextManager.beforeTestClass();
	}

	/**
	 * Delegates to {@link TestContextManager#afterTestClass}.
	 */
	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		try {
			TestContextManager testContextManager = getTestContextManager(context);
			registerMethodInvoker(testContextManager, context);
			testContextManager.afterTestClass();
		}
		finally {
			getStore(context).remove(context.getRequiredTestClass());
		}
	}

	/**
	 * Delegates to {@link TestContextManager#prepareTestInstance}.
	 * <p>This method also validates that test methods and test lifecycle methods
	 * are not annotated with {@link Autowired @Autowired}.
	 */
	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		context = findProperlyScopedExtensionContext(testInstance.getClass(), context);

		validateAutowiredConfig(context);
		validateRecordApplicationEventsConfig(context);
		TestContextManager testContextManager = getTestContextManager(context);
		registerMethodInvoker(testContextManager, context);
		testContextManager.prepareTestInstance(testInstance);
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

		String errorMessage = store.computeIfAbsent(context.getRequiredTestClass(), testClass -> {
				Method[] methodsWithErrors =
						ReflectionUtils.getUniqueDeclaredMethods(testClass, autowiredTestOrLifecycleMethodFilter);
				return (methodsWithErrors.length == 0 ? NO_VIOLATIONS_DETECTED :
						String.format(
								"Test methods and test lifecycle methods must not be annotated with @Autowired. " +
								"You should instead annotate individual method parameters with @Autowired, " +
								"@Qualifier, or @Value. Offending methods in test class %s: %s",
								testClass.getName(), Arrays.toString(methodsWithErrors)));
			}, String.class);

		if (!errorMessage.isEmpty()) {
			throw new IllegalStateException(errorMessage);
		}
	}

	/**
	 * Validate that the test class or its enclosing class doesn't attempt to record
	 * application events in a parallel mode that makes it non-deterministic
	 * ({@code @TestInstance(PER_CLASS)} and {@code @Execution(CONCURRENT)}
	 * combination).
	 * @since 6.1
	 */
	private void validateRecordApplicationEventsConfig(ExtensionContext context) {
		// We save the result in the ExtensionContext.Store so that we don't
		// re-validate the configuration for the same test class multiple times.
		Store store = context.getStore(RECORD_APPLICATION_EVENTS_VALIDATION_NAMESPACE);

		String errorMessage = store.computeIfAbsent(context.getRequiredTestClass(), testClass -> {
			boolean recording = TestContextAnnotationUtils.hasAnnotation(testClass, RecordApplicationEvents.class);
			if (!recording) {
				return NO_VIOLATIONS_DETECTED;
			}

			if (context.getTestInstanceLifecycle().orElse(Lifecycle.PER_METHOD) == Lifecycle.PER_METHOD) {
				return NO_VIOLATIONS_DETECTED;
			}

			if (context.getExecutionMode() == ExecutionMode.SAME_THREAD) {
				return NO_VIOLATIONS_DETECTED;
			}

			return """
					Test classes or @Nested test classes that @RecordApplicationEvents must not be run \
					in parallel with the @TestInstance(PER_CLASS) lifecycle mode. Configure either \
					@Execution(SAME_THREAD) or @TestInstance(PER_METHOD) semantics, or disable parallel \
					execution altogether. Note that when recording events in parallel, one might see events \
					published by other tests since the application context may be shared.""";
		}, String.class);

		if (!errorMessage.isEmpty()) {
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
		TestContextManager testContextManager = getTestContextManager(context);
		registerMethodInvoker(testContextManager, context);
		testContextManager.beforeTestMethod(testInstance, testMethod);
	}

	/**
	 * Delegates to {@link TestContextManager#beforeTestExecution}.
	 */
	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		TestContextManager testContextManager = getTestContextManager(context);
		registerMethodInvoker(testContextManager, context);
		testContextManager.beforeTestExecution(testInstance, testMethod);
	}

	/**
	 * Delegates to {@link TestContextManager#afterTestExecution}.
	 */
	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
		TestContextManager testContextManager = getTestContextManager(context);
		registerMethodInvoker(testContextManager, context);
		testContextManager.afterTestExecution(testInstance, testMethod, testException);
	}

	/**
	 * Delegates to {@link TestContextManager#afterTestMethod}.
	 */
	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
		TestContextManager testContextManager = getTestContextManager(context);
		registerMethodInvoker(testContextManager, context);
		testContextManager.afterTestMethod(testInstance, testMethod, testException);
	}

	/**
	 * Determine if the value for the {@link Parameter} in the supplied {@link ParameterContext}
	 * should be autowired from the test's {@link ApplicationContext}.
	 * <p>A parameter is considered to be autowirable if one of the following
	 * conditions is {@code true}.
	 * <ol>
	 * <li>The {@linkplain ParameterContext#getDeclaringExecutable() declaring
	 * executable} is a {@link Constructor} and
	 * {@link TestConstructorUtils#isAutowirableConstructor(Executable, PropertyProvider)}
	 * returns {@code true}. Note that {@code isAutowirableConstructor()} will be
	 * invoked with a fallback {@link PropertyProvider} that delegates its lookup
	 * to {@link ExtensionContext#getConfigurationParameter(String)}.</li>
	 * <li>The parameter is of type {@link ApplicationContext} or a sub-type thereof.</li>
	 * <li>The parameter is of type {@link ApplicationEvents} or a sub-type thereof.</li>
	 * <li>{@link ParameterResolutionDelegate#isAutowirable} returns {@code true}.</li>
	 * </ol>
	 * <p>This method does not {@linkplain #getApplicationContext(ExtensionContext)
	 * load} the {@code ApplicationContext} or verify that the application context
	 * actually contains a matching candidate bean, since doing so would potentially
	 * load an application context too early or unnecessarily.
	 * <p><strong>WARNING</strong>: If a test class {@code Constructor} is annotated
	 * with {@code @Autowired} or automatically autowirable (see
	 * {@link org.springframework.test.context.TestConstructor @TestConstructor}),
	 * Spring will assume the responsibility for resolving all parameters in the
	 * constructor. Consequently, no other registered {@link ParameterResolver}
	 * will be able to resolve parameters.
	 * @see #resolveParameter
	 * @see TestConstructorUtils#isAutowirableConstructor(Executable, PropertyProvider)
	 * @see ParameterResolutionDelegate#isAutowirable
	 */
	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Parameter parameter = parameterContext.getParameter();
		Class<?> parameterType = parameter.getType();
		Executable executable = parameter.getDeclaringExecutable();
		PropertyProvider junitPropertyProvider = propertyName ->
				extensionContext.getConfigurationParameter(propertyName).orElse(null);
		return (TestConstructorUtils.isAutowirableConstructor(executable, junitPropertyProvider) ||
				ApplicationContext.class.isAssignableFrom(parameterType) ||
				supportsApplicationEvents(parameterType, executable) ||
				ParameterResolutionDelegate.isAutowirable(parameter, parameterContext.getIndex()));
	}

	private boolean supportsApplicationEvents(Class<?> parameterType, Executable executable) {
		if (ApplicationEvents.class.isAssignableFrom(parameterType)) {
			Assert.isTrue(executable instanceof Method,
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
	public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Parameter parameter = parameterContext.getParameter();
		int index = parameterContext.getIndex();
		Executable executable = parameterContext.getDeclaringExecutable();
		Class<?> testClass = extensionContext.getRequiredTestClass();
		if (executable instanceof Constructor<?> constructor) {
			testClass = constructor.getDeclaringClass();
			extensionContext = findProperlyScopedExtensionContext(testClass, extensionContext);
		}

		ApplicationContext applicationContext = getApplicationContext(extensionContext);
		return ParameterResolutionDelegate.resolveDependency(parameter, index, testClass,
				applicationContext.getAutowireCapableBeanFactory());
	}


	/**
	 * Get the {@link ApplicationContext} associated with the supplied {@link ExtensionContext}.
	 * <p><strong>NOTE</strong>: As of Spring Framework 7.0, the supplied
	 * {@code ExtensionContext} may not be properly <em>scoped</em>. See the
	 * {@linkplain SpringExtension class-level Javadoc} for further details.
	 * <p><strong>WARNING</strong>: Invoking this method ensures that the
	 * corresponding {@code ApplicationContext} is
	 * {@linkplain org.springframework.test.context.TestContext#getApplicationContext()
	 * loaded}. Consequently, this method should not be used if eager loading of
	 * the application context is undesired. For example,
	 * {@link #supportsParameter(ParameterContext, ExtensionContext)} intentionally
	 * does not invoke this method, since doing so would potentially load an
	 * application context too early or unnecessarily.
	 * @param context the current {@code ExtensionContext} (never {@code null})
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving the application context
	 * @see org.springframework.test.context.TestContext#getApplicationContext()
	 */
	public static ApplicationContext getApplicationContext(ExtensionContext context) {
		return getTestContextManager(context).getTestContext().getApplicationContext();
	}

	/**
	 * Get the {@link TestContextManager} associated with the supplied {@link ExtensionContext}.
	 * @return the {@code TestContextManager} (never {@code null})
	 */
	static TestContextManager getTestContextManager(ExtensionContext context) {
		Assert.notNull(context, "ExtensionContext must not be null");
		Class<?> testClass = context.getRequiredTestClass();
		Store store = getStore(context);
		return store.computeIfAbsent(testClass, TestContextManager::new, TestContextManager.class);
	}

	private static Store getStore(ExtensionContext context) {
		return context.getRoot().getStore(TEST_CONTEXT_MANAGER_NAMESPACE);
	}

	/**
	 * Register a {@link MethodInvoker} adaptor for Jupiter's
	 * {@link org.junit.jupiter.api.extension.ExecutableInvoker ExecutableInvoker}
	 * in the {@link org.springframework.test.context.TestContext TestContext} for
	 * the supplied {@link TestContextManager}.
	 * @since 6.1
	 */
	private static void registerMethodInvoker(TestContextManager testContextManager, ExtensionContext context) {
		testContextManager.getTestContext().setMethodInvoker(context.getExecutableInvoker()::invoke);
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

	/**
	 * Find the properly {@linkplain ExtensionContextScope scoped} {@link ExtensionContext}
	 * for the supplied test class.
	 * <p>If the supplied {@code ExtensionContext} is already properly scoped, it
	 * will be returned. Otherwise, if the test class is annotated with
	 * {@code @SpringExtensionConfig(useTestClassScopedExtensionContext = true)},
	 * this method searches the {@code ExtensionContext} hierarchy for an
	 * {@code ExtensionContext} whose test class is the same as the supplied
	 * test class.
	 * @since 7.0
	 * @see SpringExtensionConfig#useTestClassScopedExtensionContext()
	 * @see ExtensionContextScope
	 */
	private static ExtensionContext findProperlyScopedExtensionContext(Class<?> testClass, ExtensionContext context) {
		if (useTestClassScopedExtensionContextCache.get(testClass)) {
			while (context.getRequiredTestClass() != testClass) {
				context = context.getParent().get();
			}
		}
		return context;
	}

	/**
	 * Determine if the supplied test class, or one of its enclosing classes, is annotated
	 * with {@code @SpringExtensionConfig(useTestClassScopedExtensionContext = true)}.
	 * @since 7.0
	 * @see SpringExtensionConfig#useTestClassScopedExtensionContext()
	 * @see #useTestClassScopedExtensionContextCache
	 */
	private static boolean useTestClassScopedExtensionContext(Class<?> testClass) {
		MergedAnnotation<SpringExtensionConfig> mergedAnnotation =
				MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
						.withEnclosingClasses(ClassUtils::isInnerClass)
						.from(testClass)
						.get(SpringExtensionConfig.class);

		if (mergedAnnotation.isPresent()) {
			if (mergedAnnotation.getSource() instanceof Class<?> source && ClassUtils.isInnerClass(source)) {
				throw new IllegalStateException("""
						Test class [%s] must not be annotated with @SpringExtensionConfig. \
						@SpringExtensionConfig is only supported on top-level classes.\
						""".formatted(source.getName()));
			}
			return mergedAnnotation.getBoolean("useTestClassScopedExtensionContext");
		}

		return false;
	}

}
