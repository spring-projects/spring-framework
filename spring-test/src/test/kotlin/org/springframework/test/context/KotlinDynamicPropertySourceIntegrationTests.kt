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
package org.springframework.test.context

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Kotlin integration test for [@DynamicPropertySource][DynamicPropertySource].
 *
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @author Sam Brannen
 */
@SpringJUnitConfig
class KotlinDynamicPropertySourceIntegrationTests {

	@Test
	fun hasInjectedValues(@Autowired service: Service) {
		Assertions.assertThat(service.ip).isEqualTo("127.0.0.1")
		Assertions.assertThat(service.port).isEqualTo(4242)
	}

	@Configuration
	@Import(Service::class)
	open class Config

	@Component
	class Service(@Value("\${test.container.ip}") val ip: String, @Value("\${test.container.port}") val port: Int)

	class DemoContainer(val ipAddress: String = "127.0.0.1", val port: Int = 4242)

	companion object {

		@JvmStatic
		val container = DemoContainer()

		@DynamicPropertySource
		@JvmStatic
		fun containerProperties(registry: DynamicPropertyRegistry) {
			registry.add("test.container.ip") { container.ipAddress }
			registry.add("test.container.port") { container.port }
		}
	}
}
