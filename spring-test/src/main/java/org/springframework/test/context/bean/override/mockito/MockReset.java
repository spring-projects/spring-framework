/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.mockito.MockSettings;
import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.mockito.listeners.InvocationListener;
import org.mockito.listeners.MethodInvocationReport;
import org.mockito.mock.MockCreationSettings;

import org.springframework.util.Assert;

/**
 * Reset strategy used on a mock bean.
 *
 * <p>Usually applied to a mock via the {@link MockitoBean @MockitoBean} or
 * {@link MockitoSpyBean @MockitoSpyBean} annotation but can also be directly
 * applied to any mock in the {@code ApplicationContext} using the static methods
 * in this class.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoResetTestExecutionListener
 */
public enum MockReset {

	/**
	 * Reset the mock before the test method runs.
	 */
	BEFORE,

	/**
	 * Reset the mock after the test method runs.
	 */
	AFTER,

	/**
	 * Do not reset the mock.
	 */
	NONE;


	/**
	 * Create {@link MockSettings settings} to be used with mocks where reset
	 * should occur before each test method runs.
	 * @return mock settings
	 */
	public static MockSettings before() {
		return withSettings(BEFORE);
	}

	/**
	 * Create {@link MockSettings settings} to be used with mocks where reset
	 * should occur after each test method runs.
	 * @return mock settings
	 */
	public static MockSettings after() {
		return withSettings(AFTER);
	}

	/**
	 * Create {@link MockSettings settings} to be used with mocks where a
	 * specific reset should occur.
	 * @param reset the reset type
	 * @return mock settings
	 */
	public static MockSettings withSettings(MockReset reset) {
		return apply(reset, Mockito.withSettings());
	}

	/**
	 * Apply {@link MockReset} to existing {@link MockSettings settings}.
	 * @param reset the reset type
	 * @param settings the settings
	 * @return the configured settings
	 */
	public static MockSettings apply(MockReset reset, MockSettings settings) {
		Assert.notNull(settings, "Settings must not be null");
		if (reset != null && reset != NONE) {
			settings.invocationListeners(new ResetInvocationListener(reset));
		}
		return settings;
	}

	/**
	 * Get the {@link MockReset} strategy associated with the given mock.
	 * @param mock the mock
	 * @return the reset strategy for the given mock, or {@link MockReset#NONE}
	 * if no strategy is associated with the given mock
	 */
	static MockReset get(Object mock) {
		MockingDetails mockingDetails = Mockito.mockingDetails(mock);
		if (mockingDetails.isMock()) {
			MockCreationSettings<?> settings = mockingDetails.getMockCreationSettings();
			for (InvocationListener listener : settings.getInvocationListeners()) {
				if (listener instanceof ResetInvocationListener resetInvocationListener) {
					return resetInvocationListener.reset;
				}
			}
		}
		return MockReset.NONE;
	}

	/**
	 * Dummy {@link InvocationListener} used to hold the {@link MockReset} value.
	 */
	private record ResetInvocationListener(MockReset reset) implements InvocationListener {

		@Override
		public void reportInvocation(MethodInvocationReport methodInvocationReport) {
		}
	}

}
