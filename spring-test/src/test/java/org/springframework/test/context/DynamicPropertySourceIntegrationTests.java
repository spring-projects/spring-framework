/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Integration tests for {@link DynamicPropertySource @DynamicPropertySource} and
 * {@link DynamicPropertyRegistrar} bean support.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see DynamicPropertyRegistrarIntegrationTests
 */
@SpringJUnitConfig
@TestPropertySource(properties = "test.container.ip: test")
@TestInstance(PER_CLASS)
@DisplayName("@DynamicPropertySource integration tests")
class DynamicPropertySourceIntegrationTests {

	private static final String TEST_CONTAINER_IP = "test.container.ip";
	private static final String MAGIC_WORD = "magic.word";

	static {
		System.setProperty(TEST_CONTAINER_IP, "system");
	}

	static final DemoContainer container = new DemoContainer();

	@DynamicPropertySource
	static void containerPropertiesIpAddress(DynamicPropertyRegistry registry) {
		registry.add(TEST_CONTAINER_IP, container::getIpAddress);
	}

	@DynamicPropertySource
	static void containerPropertiesPort(DynamicPropertyRegistry registry) {
		registry.add("test.container.port", container::getPort);
	}


	@AfterAll
	void clearSystemProperty() {
		System.clearProperty(TEST_CONTAINER_IP);
	}

	@Test
	@DisplayName("A custom DynamicPropertyRegistry bean can exist in the ApplicationContext")
	void customDynamicPropertyRegistryCanExistInApplicationContext(
			@Autowired DynamicPropertyRegistry dynamicPropertyRegistry) {

		assertThatRuntimeException()
				.isThrownBy(() -> dynamicPropertyRegistry.add("test", () -> "test"))
				.withMessage("Boom!");
	}

	@Test
	@DisplayName("@DynamicPropertySource overrides @TestPropertySource and JVM system property")
	void dynamicPropertySourceOverridesTestPropertySourceAndSystemProperty(@Autowired ConfigurableEnvironment env) {
		MutablePropertySources propertySources = env.getPropertySources();
		assertThat(propertySources.size()).isGreaterThanOrEqualTo(4);
		assertThat(propertySources.contains("Dynamic Test Properties")).isTrue();
		assertThat(propertySources.contains("Inlined Test Properties")).isTrue();
		assertThat(propertySources.contains("systemProperties")).isTrue();
		assertThat(propertySources.get("Dynamic Test Properties").getProperty(TEST_CONTAINER_IP)).isEqualTo("127.0.0.1");
		assertThat(propertySources.get("Dynamic Test Properties").getProperty(MAGIC_WORD)).isEqualTo("enigma");
		assertThat(propertySources.get("Inlined Test Properties").getProperty(TEST_CONTAINER_IP)).isEqualTo("test");
		assertThat(propertySources.get("systemProperties").getProperty(TEST_CONTAINER_IP)).isEqualTo("system");
		assertThat(env.getProperty(TEST_CONTAINER_IP)).isEqualTo("127.0.0.1");
		assertThat(env.getProperty(MAGIC_WORD)).isEqualTo("enigma");
	}

	@Test
	@DisplayName("@Service has values injected from @DynamicPropertySource")
	void serviceHasInjectedValues(@Autowired Service service) {
		assertThat(service.getIp()).isEqualTo("127.0.0.1");
		assertThat(service.getPort()).isEqualTo(4242);
	}


	@Configuration
	@Import(Service.class)
	static class Config {

		@Bean
		DynamicPropertyRegistrar magicPropertiesRegistrar() {
			return registry -> registry.add(MAGIC_WORD, () -> "enigma");
		}

		@Bean
		DynamicPropertyRegistry dynamicPropertyRegistry() {
			return (name, valueSupplier) -> {
				throw new RuntimeException("Boom!");
			};
		}

	}

	static class Service {

		private final String ip;

		private final int port;


		Service(@Value("${test.container.ip}") String ip, @Value("${test.container.port}") int port) {
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
