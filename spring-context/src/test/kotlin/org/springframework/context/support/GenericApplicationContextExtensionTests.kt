package org.springframework.context.support

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.springframework.context.support.GenericApplicationContextExtension.registerBean
import org.springframework.beans.factory.BeanFactoryExtension.getBean
import java.util.function.Supplier

class GenericApplicationContextExtensionTests {

	@Test
	fun registerBeanWithClass() {
		val context = GenericApplicationContext()
		context.registerBean(BeanA::class)
		context.refresh()
		assertNotNull(context.getBean(BeanA::class))
	}

	@Test
	fun registerBeanWithNameAndClass() {
		val context = GenericApplicationContext()
		context.registerBean("a", BeanA::class)
		context.refresh()
		assertNotNull(context.getBean("a"))
	}

	@Test
	fun registerBeanWithSupplier() {
		val context = GenericApplicationContext()
		context.registerBean(Supplier { BeanA() })
		context.refresh()
		assertNotNull(context.getBean(BeanA::class))
	}

	@Test
	fun registerBeanWithNameAndSupplier() {
		val context = GenericApplicationContext()
		context.registerBean("a",  Supplier { BeanA() })
		context.refresh()
		assertNotNull(context.getBean("a"))
	}

	internal class BeanA

}
