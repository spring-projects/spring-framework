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

package org.springframework.aot.test.generator.compile;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * JUnit {@link InvocationInterceptor} to support
 * {@link CompileWithTargetClassAccess @CompileWithTargetClassAccess}.
 *
 * @author Christoph Dreis
 * @author Phillip Webb
 * @since 6.0
 */
class CompileWithTargetClassAccessExtension implements InvocationInterceptor {

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
				.equals(CompileWithTargetClassAccessClassLoader.class.getName());
	}

	private void runTestWithModifiedClassPath(
			ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {

		Class<?> testClass = extensionContext.getRequiredTestClass();
		Method testMethod = invocationContext.getExecutable();
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader forkedClassPathClassLoader = new CompileWithTargetClassAccessClassLoader(
				testClass.getClassLoader());
		Thread.currentThread().setContextClassLoader(forkedClassPathClassLoader);
		try {
			runTest(forkedClassPathClassLoader, testClass.getName(), testMethod.getName());
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private void runTest(ClassLoader classLoader, String testClassName,
			String testMethodName) throws Throwable {

		Class<?> testClass = classLoader.loadClass(testClassName);
		Method testMethod = findMethod(testClass, testMethodName);
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(DiscoverySelectors.selectMethod(testClass, testMethod))
				.build();
		Launcher launcher = LauncherFactory.create();
		TestPlan testPlan = launcher.discover(request);
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(listener);
		launcher.execute(testPlan);
		TestExecutionSummary summary = listener.getSummary();
		if (!CollectionUtils.isEmpty(summary.getFailures())) {
			throw summary.getFailures().get(0).getException();
		}
	}

	private Method findMethod(Class<?> testClass, String testMethodName) {
		Method method = ReflectionUtils.findMethod(testClass, testMethodName);
		if (method == null) {
			Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(testClass);
			for (Method candidate : methods) {
				if (candidate.getName().equals(testMethodName)) {
					return candidate;
				}
			}
		}
		Assert.state(method != null, () -> "Unable to find " + testClass + "." + testMethodName);
		return method;
	}


	interface Action {

		static Action NONE = () -> {
		};


		void run() throws Throwable;

	}

}
