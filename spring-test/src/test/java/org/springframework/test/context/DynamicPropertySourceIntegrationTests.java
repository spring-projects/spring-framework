/*
 * Copyright 2002-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DynamicPropertySource @DynamicPropertySource}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
@SpringJUnitConfig
class DynamicPropertySourceIntegrationTests {

	static DemoContainer container = new DemoContainer();


	@DynamicPropertySource
	static void containerProperties(DynamicPropertyRegistry registry) {
		registry.add("test.container.ip", container::getIpAddress);
		registry.add("test.container.port", container::getPort);
	}


	@Test
	void hasInjectedValues(@Autowired Service service) {
		assertThat(service.getIp()).isEqualTo("127.0.0.1");
		assertThat(service.getPort()).isEqualTo(4242);
	}


	@Configuration
	@Import(Service.class)
	static class Config {
	}

	@Component
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
