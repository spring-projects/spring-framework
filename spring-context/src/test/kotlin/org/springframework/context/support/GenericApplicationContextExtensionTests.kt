package org.springframework.context.support

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.springframework.context.support.GenericApplicationContextExtension.registerBean
import org.springframework.beans.factory.BeanFactoryExtension.getBean
import org.springframework.context.support.GenericApplicationContextExtension.GenericApplicationContext

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
		context.registerBean { BeanA() }
		context.refresh()
		assertNotNull(context.getBean(BeanA::class))
	}

	@Test
	fun registerBeanWithNameAndSupplier() {
		val context = GenericApplicationContext()
		context.registerBean("a") { BeanA() }
		context.refresh()
		assertNotNull(context.getBean("a"))
	}

	@Test
	fun registerBeanWithFunction() {
		val context = GenericApplicationContext()
		context.registerBean(BeanA::class)
		context.registerBean { BeanB(it.getBean(BeanA::class)) }
		context.refresh()
		assertNotNull(context.getBean(BeanA::class))
		assertNotNull(context.getBean(BeanB::class))
	}

	@Test
	fun registerBeanWithNameAndFunction() {
		val context = GenericApplicationContext()
		context.registerBean("a", BeanA::class)
		context.registerBean("b") { BeanB(it.getBean(BeanA::class)) }
		context.refresh()
		assertNotNull(context.getBean("a"))
		assertNotNull(context.getBean("b"))
	}

	@Test
	fun registerBeanWithGradleStyleApi() {
		val context = GenericApplicationContext {
			registerBean<BeanA>()
			registerBean { BeanB(it.getBean<BeanA>()) }
		}
		context.refresh()
		assertNotNull(context.getBean<BeanA>())
		assertNotNull(context.getBean<BeanB>())
	}

	internal class BeanA

	internal class BeanB(val a: BeanA)

}
