/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.annotation

import java.lang.reflect.Method

import org.junit.Before
import org.junit.Test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.tests.sample.beans.TestBean

import org.junit.Assert.*

/**
 * Tests for Kotlin support with [@Autowired].
 *
 * @author Juergen Hoeller
 */
class KotlinAutowiredTests {

	@Test
	fun autowiringWithTarget() {
		var bf = DefaultListableBeanFactory()
		var bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		var bd = RootBeanDefinition(KotlinBean::class.java)
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE)
		bf.registerBeanDefinition("annotatedBean", bd)
		var tb = TestBean()
		bf.registerSingleton("testBean", tb)

		var kb = bf.getBean("annotatedBean", KotlinBean::class.java)
		assertSame(tb, kb.injectedFromConstructor)
		assertSame(tb, kb.injectedFromMethod)
		assertSame(tb, kb.injectedField)
	}

	@Test
	fun autowiringWithoutTarget() {
		var bf = DefaultListableBeanFactory()
		var bpp = AutowiredAnnotationBeanPostProcessor()
		bpp.setBeanFactory(bf)
		bf.addBeanPostProcessor(bpp)
		var bd = RootBeanDefinition(KotlinBean::class.java)
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE)
		bf.registerBeanDefinition("annotatedBean", bd)

		var kb = bf.getBean("annotatedBean", KotlinBean::class.java)
		assertNull(kb.injectedFromConstructor)
		assertNull(kb.injectedFromMethod)
		assertNull(kb.injectedField)
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

}
