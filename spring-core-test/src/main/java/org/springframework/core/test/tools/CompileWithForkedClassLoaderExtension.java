/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.test.tools;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.platform.commons.util.ReflectionUtils.getFullyQualifiedMethodName;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

/**
 * JUnit Jupiter {@link InvocationInterceptor} to support
 * {@link CompileWithForkedClassLoader @CompileWithForkedClassLoader}.
 *
 * @author Christoph Dreis
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.0
 */
class CompileWithForkedClassLoaderExtension implements InvocationInterceptor {

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {

		intercept(invocation, extensionContext);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {

		intercept(invocation, extensionContext);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {

		intercept(invocation, extensionContext);
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {

		intercept(invocation, extensionContext);
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {

		intercept(invocation, extensionContext,
				() -> runTestWithModifiedClassPath(invocationContext, extensionContext));
	}

	private void intercept(Invocation<Void> invocation, ExtensionContext extensionContext)
			throws Throwable {

		intercept(invocation, extensionContext, Action.NONE);
	}

	private void intercept(Invocation<Void> invocation, ExtensionContext extensionContext,
			Action action) throws Throwable {

		if (isUsingForkedClassPathLoader(extensionContext)) {
			invocation.proceed();
			return;
		}
		invocation.skip();
		action.run();
	}

	private boolean isUsingForkedClassPathLoader(ExtensionContext extensionContext) {
		Class<?> testClass = extensionContext.getRequiredTestClass();
		ClassLoader classLoader = testClass.getClassLoader();
		return classLoader.getClass().getName()
				.equals(CompileWithForkedClassLoaderClassLoader.class.getName());
	}

	private void runTestWithModifiedClassPath(
			ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {

		Class<?> testClass = extensionContext.getRequiredTestClass();
		Method testMethod = invocationContext.getExecutable();
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader forkedClassPathClassLoader = new CompileWithForkedClassLoaderClassLoader(
				testClass.getClassLoader());
		Thread.currentThread().setContextClassLoader(forkedClassPathClassLoader);
		try {
			runTest(testClass, testMethod);
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private void runTest(Class<?> testClass, Method testMethod) throws Throwable {
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(selectMethod(getFullyQualifiedMethodName(testClass, testMethod)))
				.filters(includeEngines("junit-jupiter"))
				.build();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		Launcher launcher = LauncherFactory.create();
		launcher.execute(request, listener);
		TestExecutionSummary summary = listener.getSummary();
		if (summary.getTotalFailureCount() > 0) {
			throw summary.getFailures().get(0).getException();
		}
	}


	@FunctionalInterface
	interface Action {

		Action NONE = () -> {};


		void run() throws Throwable;

	}

}
