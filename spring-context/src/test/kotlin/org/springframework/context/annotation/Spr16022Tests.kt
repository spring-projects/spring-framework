/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.support.ClassPathXmlApplicationContext

/**
 * @author Sebastien Deleuze
 */
class Spr16022Tests {

	@Test
	fun `Register beans with multiple constructors with AnnotationConfigApplicationContext`() {
		assert(AnnotationConfigApplicationContext(Config::class.java))
	}

	@Test
	fun `Register beans with multiple constructors with ClassPathXmlApplicationContext`() {
		assert(ClassPathXmlApplicationContext(CONTEXT))
	}

	private fun assert(context: BeanFactory) {
		val bean1 = context.getBean<MultipleConstructorsTestBean>("bean1")
		assertThat(bean1.foo).isEqualTo(0)
		val bean2 = context.getBean<MultipleConstructorsTestBean>("bean2")
		assertThat(bean2.foo).isEqualTo(1)
		val bean3 = context.getBean<MultipleConstructorsTestBean>("bean3")
		assertThat(bean3.foo).isEqualTo(3)

	}

	@Suppress("unused")
	class MultipleConstructorsTestBean(val foo: Int) {
		constructor(bar: String) : this(bar.length)
		constructor(foo: Int, bar: String) : this(foo + bar.length)
	}

	@Configuration @ImportResource(CONTEXT)
	open class Config
}

private const val CONTEXT = "org/springframework/context/annotation/multipleConstructors.xml"