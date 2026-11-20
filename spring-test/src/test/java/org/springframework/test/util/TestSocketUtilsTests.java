/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link  TestSocketUtils}.
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
