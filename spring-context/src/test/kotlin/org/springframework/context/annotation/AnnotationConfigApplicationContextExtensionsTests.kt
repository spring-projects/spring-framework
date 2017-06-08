package org.springframework.context.annotation

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.support.registerBean

/**
 * Tests for [AnnotationConfigApplicationContext] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class AnnotationConfigApplicationContextExtensionsTests {
 
	@Test
	fun `Instantiate AnnotationConfigApplicationContext`() {
		val applicationContext = AnnotationConfigApplicationContext {
			registerBean<Foo>()
		}
		applicationContext.refresh()
		assertNotNull(applicationContext)
		assertNotNull(applicationContext.getBean<Foo>())
	}

	class Foo
}
