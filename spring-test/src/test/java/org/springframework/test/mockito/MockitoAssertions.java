/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.mockito;

import org.mockito.mock.MockName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

/**
 * Assertions for Mockito mocks and spies.
 *
 * @author Sam Brannen
 * @since 6.2.1
 */
public abstract class MockitoAssertions {

	public static void assertIsMock(Object obj) {
		assertThat(isMock(obj)).as("is a Mockito mock").isTrue();
		assertIsNotSpy(obj);
	}

	public static void assertIsMock(Object obj, String message) {
		assertThat(isMock(obj)).as("%s is a Mockito mock", message).isTrue();
		assertIsNotSpy(obj, message);
	}

	public static void assertIsNotMock(Object obj) {
		assertThat(isMock(obj)).as("is a Mockito mock").isFalse();
	}

	public static void assertIsNotMock(Object obj, String message) {
		assertThat(isMock(obj)).as("%s is a Mockito mock", message).isFalse();
	}

	public static void assertIsSpy(Object obj) {
		assertThat(isSpy(obj)).as("is a Mockito spy").isTrue();
	}

	public static void assertIsSpy(Object obj, String message) {
		assertThat(isSpy(obj)).as("%s is a Mockito spy", message).isTrue();
	}

	public static void assertIsNotSpy(Object obj) {
		assertThat(isSpy(obj)).as("is a Mockito spy").isFalse();
	}

	public static void assertIsNotSpy(Object obj, String message) {
		assertThat(isSpy(obj)).as("%s is a Mockito spy", message).isFalse();
	}

	public static void assertMockName(Object mock, String name) {
		MockName mockName = mockingDetails(mock).getMockCreationSettings().getMockName();
		assertThat(mockName.toString()).as("mock name").isEqualTo(name);
	}

	private static boolean isMock(Object obj) {
		return mockingDetails(obj).isMock();
	}

	private static boolean isSpy(Object obj) {
		return mockingDetails(obj).isSpy();
	}

}
