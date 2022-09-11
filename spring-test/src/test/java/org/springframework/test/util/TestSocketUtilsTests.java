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

import javax.net.ServerSocketFactory;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link  TestSocketUtils}.
 *
 * @author Sam Brannen
 * @author Gary Russell
 * @author Chris Bono
 */
class TestSocketUtilsTests {

	@Test
	void findAvailableTcpPort() {
		int port = TestSocketUtils.findAvailableTcpPort();
		assertThat(port >= 1024).as("port [" + port + "] >= " + 1024).isTrue();
		assertThat(port <= 65535).as("port [" + port + "] <= " + 65535).isTrue();
	}

	@Test
	void findAvailableTcpPortWhenNoAvailablePortFoundInMaxAttempts()  {
		try (MockedStatic<ServerSocketFactory> mockedServerSocketFactory = Mockito.mockStatic(ServerSocketFactory.class)) {
			mockedServerSocketFactory.when(ServerSocketFactory::getDefault).thenThrow(new RuntimeException("Boom"));
			assertThatIllegalStateException().isThrownBy(TestSocketUtils::findAvailableTcpPort)
					.withMessageStartingWith("Could not find an available TCP port")
					.withMessageEndingWith("after 1000 attempts");

		}
	}

}
