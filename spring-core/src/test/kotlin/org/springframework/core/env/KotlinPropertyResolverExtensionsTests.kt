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

package org.springframework.core.env

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * Mock object based tests for PropertyResolver Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class KotlinPropertyResolverExtensionsTests {

	val propertyResolver = mockk<PropertyResolver>()

	@Test
	fun `get operator`() {
		every { propertyResolver.getProperty("name") } returns "foo"
		propertyResolver["name"]
		verify { propertyResolver.getProperty("name") }
	}

	@Test
	fun `getProperty extension`() {
		every { propertyResolver.getProperty("name", String::class.java) } returns "foo"
		propertyResolver.getProperty<String>("name")
		verify { propertyResolver.getProperty("name", String::class.java) }
	}

	@Test
	fun `getRequiredProperty extension`() {
		every { propertyResolver.getRequiredProperty("name", String::class.java) } returns "foo"
		propertyResolver.getRequiredProperty<String>("name")
		verify { propertyResolver.getRequiredProperty("name", String::class.java) }
	}

}
