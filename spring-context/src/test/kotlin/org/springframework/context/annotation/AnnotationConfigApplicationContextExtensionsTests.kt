/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.support.registerBean

/**
 * Tests for [AnnotationConfigApplicationContext] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class AnnotationConfigApplicationContextExtensionsTests {

	@Test
	@Suppress("DEPRECATION")
	fun `Instantiate AnnotationConfigApplicationContext`() {
		val applicationContext = AnnotationConfigApplicationContext {
			registerBean<Foo>()
		}
		assertThat(applicationContext).isNotNull()
		applicationContext.refresh()
		applicationContext.getBean<Foo>()
	}

	class Foo
}
