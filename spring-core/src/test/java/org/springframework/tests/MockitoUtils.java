/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
