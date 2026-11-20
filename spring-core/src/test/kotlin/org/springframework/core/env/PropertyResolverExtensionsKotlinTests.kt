/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.env

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Mock object based tests for PropertyResolver Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class PropertyResolverExtensionsKotlinTests {

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
	fun `getProperty with default extension`() {
		every { propertyResolver.getProperty("name", String::class.java, "default") } returns "default"
		assertThat(propertyResolver.getProperty<String>("name", "default")).isEqualTo("default")
		verify { propertyResolver.getProperty("name", String::class.java, "default") }
	}

	@Test
	fun `getRequiredProperty extension`() {
		every { propertyResolver.getRequiredProperty("name", String::class.java) } returns "foo"
		propertyResolver.getRequiredProperty<String>("name")
		verify { propertyResolver.getRequiredProperty("name", String::class.java) }
	}

}
