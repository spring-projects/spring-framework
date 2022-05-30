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

package org.springframework.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.GenericTypeResolver.resolveReturnTypeArgument
import java.lang.reflect.Method

/**
 * Tests for Kotlin support in [GenericTypeResolver].
 *
 * @author Konrad Kaminski
 * @author Sebastien Deleuze
 */
class KotlinGenericTypeResolverTests {

	@Test
	fun methodReturnTypes() {
		assertThat(resolveReturnTypeArgument(findMethod(MyTypeWithMethods::class.java, "integer")!!,
				MyInterfaceType::class.java)).isEqualTo(Integer::class.java)
		assertThat(resolveReturnTypeArgument(findMethod(MyTypeWithMethods::class.java, "string")!!,
				MyInterfaceType::class.java)).isEqualTo(String::class.java)
		assertThat(resolveReturnTypeArgument(findMethod(MyTypeWithMethods::class.java, "raw")!!,
				MyInterfaceType::class.java)).isNull()
		assertThat(resolveReturnTypeArgument(findMethod(MyTypeWithMethods::class.java, "object")!!,
				MyInterfaceType::class.java)).isNull()
	}

	private fun findMethod(clazz: Class<*>, name: String): Method? =
			clazz.methods.firstOrNull { it.name == name }

	open class MyTypeWithMethods<T> {
		suspend fun integer(): MyInterfaceType<Int>? = null

		suspend fun string(): MySimpleInterfaceType? = null

		suspend fun `object`(): Any? = null

		suspend fun raw(): MyInterfaceType<*>? = null
	}

	interface MyInterfaceType<T>

	interface MySimpleInterfaceType: MyInterfaceType<String>

	open class MySimpleTypeWithMethods: MyTypeWithMethods<Int>()
}
