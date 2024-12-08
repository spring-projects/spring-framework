/*
 * Copyright 2002-2024 the original author or authors.
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
	}

	public static void assertIsMock(Object obj, String message) {
		assertThat(isMock(obj)).as("%s is a Mockito mock", message).isTrue();
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
