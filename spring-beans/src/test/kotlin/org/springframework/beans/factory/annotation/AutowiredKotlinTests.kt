/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.annotation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.beans.testfixture.beans.Colour
import org.springframework.beans.testfixture.beans.TestBean

/**
 * Tests for Kotlin support with [Autowired].
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
class AutowiredKotlinTests {

	@Test
	fun `Autowiring with target`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBean::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("annotatedBean", bd)
		val tb = TestBean()
		bf.registerSingleton("testBean", tb)

		val kb = bf.getBean("annotatedBean", KotlinBean::class.java)
		assertThat(kb.injectedFromConstructor).isSameAs(tb)
		assertThat(kb.injectedFromMethod).isSameAs(tb)
		assertThat(kb.injectedField).isSameAs(tb)
	}

	@Test
	fun `Autowiring without target`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBean::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("annotatedBean", bd)

		val kb = bf.getBean("annotatedBean", KotlinBean::class.java)
		assertThat(kb.injectedFromConstructor).isNull()
		assertThat(kb.injectedFromMethod).isNull()
		assertThat(kb.injectedField).isNull()
	}
	
	@Test  // SPR-15847
	fun `Autowiring by primary constructor with mandatory and optional parameters`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBeanWithMandatoryAndOptionalParameters::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("bean", bd)
		val tb = TestBean()
		bf.registerSingleton("testBean", tb)

		val kb = bf.getBean("bean", KotlinBeanWithMandatoryAndOptionalParameters::class.java)
		assertThat(kb.injectedFromConstructor).isSameAs(tb)
		assertThat(kb.optional).isEqualTo("foo")
		assertThat(kb.initializedField).isEqualTo("bar")
	}

	@Test
	fun `Autowiring by primary constructor with optional parameters`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBeanWithOptionalParameters::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("bean", bd)

		val kb = bf.getBean("bean", KotlinBeanWithOptionalParameters::class.java)
		assertThat(kb.optional1).isNotNull()
		assertThat(kb.optional2).isEqualTo("foo")
		assertThat(kb.initializedField).isEqualTo("bar")
	}

	@Test  // SPR-15847
	fun `Autowiring by annotated primary constructor with optional parameter`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBeanWithOptionalParameterAndExplicitConstructor::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("bean", bd)
		val tb = TestBean()
		bf.registerSingleton("testBean", tb)

		val kb = bf.getBean("bean", KotlinBeanWithOptionalParameterAndExplicitConstructor::class.java)
		assertThat(kb.injectedFromConstructor).isSameAs(tb)
		assertThat(kb.optional).isEqualTo("foo")
	}

	@Test  // SPR-15847
	fun `Autowiring by annotated secondary constructor with optional parameter`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBeanWithAutowiredSecondaryConstructor::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("bean", bd)
		val tb = TestBean()
		bf.registerSingleton("testBean", tb)
		val colour = Colour.BLUE
		bf.registerSingleton("colour", colour)

		val kb = bf.getBean("bean", KotlinBeanWithAutowiredSecondaryConstructor::class.java)
		assertThat(kb.injectedFromConstructor).isSameAs(tb)
		assertThat(kb.optional).isEqualTo("bar")
		assertThat(kb.injectedFromSecondaryConstructor).isSameAs(colour)
	}

	@Test  // SPR-16012
	fun `Fallback on the default constructor when no autowirable primary constructor is defined`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBeanWithPrimaryAndDefaultConstructors::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("bean", bd)

		val kb = bf.getBean("bean", KotlinBeanWithPrimaryAndDefaultConstructors::class.java)
		assertThat(kb.testBean).isNotNull()
	}

	@Test  // SPR-16012
	fun `Instantiation via primary constructor when a default is defined`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBeanWithPrimaryAndDefaultConstructors::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("bean", bd)
		val tb = TestBean()
		bf.registerSingleton("testBean", tb)

		val kb = bf.getBean("bean", KotlinBeanWithPrimaryAndDefaultConstructors::class.java)
		assertThat(kb.testBean).isEqualTo(tb)
	}

	@Test  // SPR-16289
	fun `Instantiation via secondary constructor when a default primary is defined`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBeanWithPrimaryAndSecondaryConstructors::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("bean", bd)

		bf.getBean(KotlinBeanWithPrimaryAndSecondaryConstructors::class.java, "foo")
		bf.getBean(KotlinBeanWithPrimaryAndSecondaryConstructors::class.java)
	}

	@Test  // SPR-16022
	fun `No autowiring with primary and secondary non annotated constructors`() {
		val bf = DefaultListableBeanFactory()
		val bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		val bd = RootBeanDefinition(KotlinBeanWithSecondaryConstructor::class.java)
		bd.scope = RootBeanDefinition.SCOPE_PROTOTYPE
		bf.registerBeanDefinition("bean", bd)
		val tb = TestBean()
		bf.registerSingleton("testBean", tb)
		val colour = Colour.BLUE
		bf.registerSingleton("colour", colour)

		assertThatExceptionOfType(BeanCreationException::class.java).isThrownBy {
			bf.getBean("bean", KotlinBeanWithSecondaryConstructor::class.java)
		}
	}


	class KotlinBean(val injectedFromConstructor: TestBean?) {
		
		var injectedFromMethod: TestBean? = null

		@Autowired
		var injectedField: TestBean? = null

		@Autowired
		fun injectedMethod(p1: TestBean?) {
			injectedFromMethod = p1
		}
	}

	class KotlinBeanWithMandatoryAndOptionalParameters(
			val injectedFromConstructor: TestBean,
			val optional: String = "foo"
	) {
		var initializedField: String? = null

		init {
			initializedField = "bar"
		}
	}

	class KotlinBeanWithOptionalParameters(
			val optional1: TestBean = TestBean(),
			val optional2: String = "foo"
	) {
		var initializedField: String? = null

		init {
			initializedField = "bar"
		}
	}

	class KotlinBeanWithOptionalParameterAndExplicitConstructor @Autowired constructor(
			val optional: String = "foo",
			val injectedFromConstructor: TestBean
	)

	class KotlinBeanWithAutowiredSecondaryConstructor(
			val optional: String = "foo",
			val injectedFromConstructor: TestBean
	) {
		@Autowired constructor(injectedFromSecondaryConstructor: Colour, injectedFromConstructor: TestBean,
							   optional: String = "bar") : this(optional, injectedFromConstructor) {
			this.injectedFromSecondaryConstructor = injectedFromSecondaryConstructor
		}

		var injectedFromSecondaryConstructor: Colour? = null
	}

	@Suppress("unused")
	class KotlinBeanWithPrimaryAndDefaultConstructors(val testBean: TestBean) {
		constructor() : this(TestBean())
	}

	@Suppress("unused", "UNUSED_PARAMETER")
	class KotlinBeanWithPrimaryAndSecondaryConstructors() {
		constructor(p: String) : this()
	}

	class KotlinBeanWithSecondaryConstructor(
			val optional: String = "foo",
			val injectedFromConstructor: TestBean
	) {
		constructor(injectedFromSecondaryConstructor: Colour, injectedFromConstructor: TestBean,
					optional: String = "bar") : this(optional, injectedFromConstructor) {
			this.injectedFromSecondaryConstructor = injectedFromSecondaryConstructor
		}

		var injectedFromSecondaryConstructor: Colour? = null
	}

}
