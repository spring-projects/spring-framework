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

package org.springframework.tests;

import java.util.List;

import org.mockito.Mockito;
import org.mockito.internal.stubbing.InvocationContainerImpl;
import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.Invocation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * General test utilities for use with {@link Mockito}.
 *
 * @author Phillip Webb
 */
public abstract class MockitoUtils {

	/**
	 * Verify the same invocations have been applied to two mocks. This is generally not
	 * the preferred way test with mockito and should be avoided if possible.
	 * @param expected the mock containing expected invocations
	 * @param actual the mock containing actual invocations
	 * @param argumentAdapters adapters that can be used to change argument values before they are compared
	 */
	public static <T> void verifySameInvocations(T expected, T actual, InvocationArgumentsAdapter... argumentAdapters) {
		List<Invocation> expectedInvocations =
				((InvocationContainerImpl) MockUtil.getMockHandler(expected).getInvocationContainer()).getInvocations();
		List<Invocation> actualInvocations =
				((InvocationContainerImpl) MockUtil.getMockHandler(actual).getInvocationContainer()).getInvocations();
		verifySameInvocations(expectedInvocations, actualInvocations, argumentAdapters);
	}

	private static void verifySameInvocations(List<Invocation> expectedInvocations, List<Invocation> actualInvocations,
			InvocationArgumentsAdapter... argumentAdapters) {

		assertThat(expectedInvocations).hasSameSizeAs(actualInvocations);
		for (int i = 0; i < expectedInvocations.size(); i++) {
			verifySameInvocation(expectedInvocations.get(i), actualInvocations.get(i), argumentAdapters);
		}
	}

	private static void verifySameInvocation(Invocation expectedInvocation, Invocation actualInvocation,
			InvocationArgumentsAdapter... argumentAdapters) {

		assertThat(expectedInvocation.getMethod()).isEqualTo(actualInvocation.getMethod());
		Object[] expectedArguments = getInvocationArguments(expectedInvocation, argumentAdapters);
		Object[] actualArguments = getInvocationArguments(actualInvocation, argumentAdapters);
		assertThat(expectedArguments).isEqualTo(actualArguments);
	}

	private static Object[] getInvocationArguments(Invocation invocation, InvocationArgumentsAdapter... argumentAdapters) {
		Object[] arguments = invocation.getArguments();
		for (InvocationArgumentsAdapter adapter : argumentAdapters) {
			arguments = adapter.adaptArguments(arguments);
		}
		return arguments;
	}


	/**
	 * Adapter strategy that can be used to change invocation arguments.
	 */
	public interface InvocationArgumentsAdapter {

		/**
		 * Change the arguments if required.
		 * @param arguments the source arguments
		 * @return updated or original arguments (never {@code null})
		 */
		Object[] adaptArguments(Object[] arguments);
	}

}
