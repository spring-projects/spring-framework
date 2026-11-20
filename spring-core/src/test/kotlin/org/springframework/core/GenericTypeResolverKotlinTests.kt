/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class GenericTypeResolverKotlinTests {

	@Test
	fun methodReturnTypes() {
		assertThat(resolveReturnTypeArgument(findMethod(MyTypeWithMethods::class.java, "integer")!!,
				MyInterfaceType::class.java)).isEqualTo(Int::class.javaObjectType)
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
