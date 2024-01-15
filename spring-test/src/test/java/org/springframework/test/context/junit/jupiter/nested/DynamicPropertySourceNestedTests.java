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

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link DynamicPropertySource @DynamicPropertySource} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @author Yanming Zhou
 * @since 5.3.2
 */
@SpringJUnitConfig
class DynamicPropertySourceNestedTests {

	private static final String TEST_CONTAINER_IP = "DynamicPropertySourceNestedTests.test.container.ip";

	private static final String TEST_CONTAINER_PORT = "DynamicPropertySourceNestedTests.test.container.port";

	static final DemoContainer container = new DemoContainer();

	@DynamicPropertySource
	static void containerProperties(DynamicPropertyRegistry registry) {
		registry.add(TEST_CONTAINER_IP, container::getIpAddress);
		registry.add(TEST_CONTAINER_PORT, container::getPort);
	}


	@Test
	@DisplayName("@Service has values injected from @DynamicPropertySource")
	void serviceHasInjectedValues(@Autowired Service service) {
		assertServiceHasInjectedValues(service);
	}

	private static void assertServiceHasInjectedValues(Service service) {
		assertThat(service.getIp()).isEqualTo("127.0.0.1");
		assertThat(service.getPort()).isEqualTo(4242);
	}

	@Nested
	@NestedTestConfiguration(OVERRIDE)
	@SpringJUnitConfig(Config.class)
	class DynamicPropertySourceFromSuperclassTests extends DynamicPropertySourceSuperclass {

		@Test
		@DisplayName("@Service has values injected from @DynamicPropertySource in superclass")
		void serviceHasInjectedValues(@Autowired Service service) {
			assertServiceHasInjectedValues(service);
		}
	}

	@Nested
	@NestedTestConfiguration(OVERRIDE)
	@SpringJUnitConfig(Config.class)
	class DynamicPropertySourceFromInterfaceTests implements DynamicPropertySourceInterface {

		@Test
		@DisplayName("@Service has values injected from @DynamicPropertySource in interface")
		void serviceHasInjectedValues(@Autowired Service service) {
			assertServiceHasInjectedValues(service);
		}
	}

	@Nested
	@NestedTestConfiguration(OVERRIDE)
	@SpringJUnitConfig(Config.class)
	class OverriddenConfigTests {

		@Test
		@DisplayName("@Service does not have values injected from @DynamicPropertySource in enclosing class")
		void serviceHasDefaultInjectedValues(@Autowired Service service) {
			assertThat(service.getIp()).isEqualTo("10.0.0.1");
			assertThat(service.getPort()).isEqualTo(-999);
		}
	}

	@Nested
	class DynamicPropertySourceFromEnclosingClassTests {

		@Test
		@DisplayName("@Service has values injected from @DynamicPropertySource in enclosing class")
		void serviceHasInjectedValues(@Autowired Service service) {
			assertServiceHasInjectedValues(service);
		}

		@Nested
		class DoubleNestedDynamicPropertySourceFromEnclosingClassTests {

			@Test
			@DisplayName("@Service has values injected from @DynamicPropertySource in enclosing class")
			void serviceHasInjectedValues(@Autowired Service service) {
				assertServiceHasInjectedValues(service);
			}
		}
	}

	@Nested
	class DynamicPropertySourceOverridesEnclosingClassTests {

		@DynamicPropertySource
		static void overrideDynamicPropertyFromEnclosingClass(DynamicPropertyRegistry registry) {
			registry.add(TEST_CONTAINER_PORT, () -> -999);
		}

		@Test
		@DisplayName("@Service has values injected from @DynamicPropertySource in enclosing class and nested class")
		void serviceHasInjectedValues(@Autowired Service service) {
			assertThat(service.getIp()).isEqualTo("127.0.0.1");
			assertThat(service.getPort()).isEqualTo(-999);
		}

	}

	abstract static class DynamicPropertySourceSuperclass {

		@DynamicPropertySource
		static void containerProperties(DynamicPropertyRegistry registry) {
			registry.add(TEST_CONTAINER_IP, container::getIpAddress);
			registry.add(TEST_CONTAINER_PORT, container::getPort);
		}
	}

	interface DynamicPropertySourceInterface {

		@DynamicPropertySource
		static void containerProperties(DynamicPropertyRegistry registry) {
			registry.add(TEST_CONTAINER_IP, container::getIpAddress);
			registry.add(TEST_CONTAINER_PORT, container::getPort);
		}
	}


	@Configuration
	@Import(Service.class)
	static class Config {
	}

	static class Service {

		private final String ip;

		private final int port;


		Service(@Value("${" + TEST_CONTAINER_IP + ":10.0.0.1}") String ip, @Value("${" + TEST_CONTAINER_PORT + ":-999}") int port) {
			this.ip = ip;
			this.port = port;
		}

		String getIp() {
			return this.ip;
		}

		int getPort() {
			return this.port;
		}
	}

	static class DemoContainer {

		String getIpAddress() {
			return "127.0.0.1";
		}

		int getPort() {
			return 4242;
		}
	}

}
