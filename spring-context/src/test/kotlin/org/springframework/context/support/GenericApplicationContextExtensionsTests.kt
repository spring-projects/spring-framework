/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.support

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.springframework.beans.factory.getBean

/**
 * Tests for [GenericApplicationContext] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class GenericApplicationContextExtensionsTests {

	@Test
	fun registerBeanWithClass() {
		val context = GenericApplicationContext()
		context.registerBean<BeanA>()
		context.refresh()
		assertNotNull(context.getBean<BeanA>())
	}

	@Test
	fun registerBeanWithNameAndClass() {
		val context = GenericApplicationContext()
		context.registerBean<BeanA>("a")
		context.refresh()
		assertNotNull(context.getBean("a"))
	}

	@Test
	fun registerBeanWithSupplier() {
		val context = GenericApplicationContext()
		context.registerBean { BeanA() }
		context.refresh()
		assertNotNull(context.getBean<BeanA>())
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
		context.registerBean<BeanA>()
		context.registerBean { BeanB(it.getBean<BeanA>()) }
		context.refresh()
		assertNotNull(context.getBean<BeanA>())
		assertNotNull(context.getBean<BeanB>())
	}

	@Test
	fun registerBeanWithNameAndFunction() {
		val context = GenericApplicationContext()
		context.registerBean<BeanA>("a")
		context.registerBean("b") { BeanB(it.getBean<BeanA>()) }
		context.refresh()
		assertNotNull(context.getBean("a"))
		assertNotNull(context.getBean("b"))
	}

	class BeanA

	class BeanB(val a: BeanA)

}
