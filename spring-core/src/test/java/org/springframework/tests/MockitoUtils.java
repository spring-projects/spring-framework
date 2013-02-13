/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.tests;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.Invocation;

/**
 * General test utilities for use with {@link Mockito}.
 *
 * @author Phillip Webb
 */
public class MockitoUtils {

	private static MockUtil mockUtil = new MockUtil();

	/**
	 * Verify the same invocations have been applied to two mocks. This is generally not
	 * the preferred way test with mockito and should be avoided if possible.
	 * @param expected the mock containing expected invocations
	 * @param actual the mock containing actual invocations
	 * @param argumentAdapters adapters that can be used to change argument values before
	 *        they are compared
	 */
	public static <T> void verifySameInvocations(T expected, T actual, InvocationArgumentsAdapter... argumentAdapters) {
		List<Invocation> expectedInvocations = mockUtil.getMockHandler(expected).getInvocationContainer().getInvocations();
		List<Invocation> actualInvocations = mockUtil.getMockHandler(actual).getInvocationContainer().getInvocations();
		verifySameInvocations(expectedInvocations, actualInvocations, argumentAdapters);
	}

	private static void verifySameInvocations(List<Invocation> expectedInvocations, List<Invocation> actualInvocations, InvocationArgumentsAdapter... argumentAdapters) {
		assertThat(expectedInvocations.size(), is(equalTo(actualInvocations.size())));
		for (int i = 0; i < expectedInvocations.size(); i++) {
			verifySameInvocation(expectedInvocations.get(i), actualInvocations.get(i), argumentAdapters);
		}
	}

	private static void verifySameInvocation(Invocation expectedInvocation, Invocation actualInvocation, InvocationArgumentsAdapter... argumentAdapters) {
		System.out.println(expectedInvocation);
		System.out.println(actualInvocation);
		assertThat(expectedInvocation.getMethod(), is(equalTo(actualInvocation.getMethod())));
		Object[] expectedArguments = getInvocationArguments(expectedInvocation, argumentAdapters);
		Object[] actualArguments = getInvocationArguments(actualInvocation, argumentAdapters);
		assertThat(expectedArguments, is(equalTo(actualArguments)));
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
	public static interface InvocationArgumentsAdapter {

		/**
		 * Change the arguments if required
		 * @param arguments the source arguments
		 * @return updated or original arguments (never {@code null})
		 */
		Object[] adaptArguments(Object[] arguments);
	}
}
