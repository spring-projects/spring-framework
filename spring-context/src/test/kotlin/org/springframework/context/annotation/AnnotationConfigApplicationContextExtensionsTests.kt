/*
 * Copyright 2002-2019 the original author or authors.
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
