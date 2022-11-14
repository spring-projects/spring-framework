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

package org.springframework.test.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link  TestSocketUtils}.
 *
 * @author Sam Brannen
 * @author Gary Russell
 * @since 5.3.24
 */
class TestSocketUtilsTests {

	@Test
	void canBeInstantiated() {
		// Just making sure somebody doesn't try to make SocketUtils abstract,
		// since that would be a breaking change due to the intentional public
		// constructor.
		new TestSocketUtils();
	}

	@RepeatedTest(10)
	void findAvailableTcpPort() {
		assertThat(TestSocketUtils.findAvailableTcpPort())
			.isBetween(TestSocketUtils.PORT_RANGE_MIN, TestSocketUtils.PORT_RANGE_MAX);
	}

	@Test
	void findAvailableTcpPortWhenNoAvailablePortFoundInMaxAttempts() {
		TestSocketUtils socketUtils = new TestSocketUtils() {
			@Override
			boolean isPortAvailable(int port) {
				return false;
			}
		};
		assertThatIllegalStateException()
			.isThrownBy(socketUtils::findAvailableTcpPortInternal)
			.withMessage("Could not find an available TCP port in the range [1024, 65535] after 1000 attempts");
	}

}
