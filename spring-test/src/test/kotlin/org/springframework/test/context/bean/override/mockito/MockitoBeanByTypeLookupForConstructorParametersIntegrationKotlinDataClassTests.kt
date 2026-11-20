/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito

import org.junit.jupiter.api.Test

import org.springframework.test.context.bean.override.example.ExampleService
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.mockito.MockitoAssertions.assertIsMock

/**
 * Integration tests for [@MockitoBean][MockitoBean] that use by-type lookup
 * on constructor parameters in a Kotlin data class.
 *
 * @author Sam Brannen
 * @since 7.1
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/36096">gh-36096</a>
 * @see MockitoBeanByTypeLookupForConstructorParametersIntegrationKotlinTests
 */
@SpringJUnitConfig
data class MockitoBeanByTypeLookupForConstructorParametersIntegrationKotlinDataClassTests(
		@MockitoBean val exampleService: ExampleService) {

	@Test
	fun test() {
		assertIsMock(exampleService)
	}

}
