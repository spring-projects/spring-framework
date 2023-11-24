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

package org.springframework.expression.spel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import kotlin.coroutines.Continuation

class SpelReproKotlinTests {

	private val parser: ExpressionParser = SpelExpressionParser()

	private val context = TestScenarioCreator.getTestEvaluationContext()


	@Test
	fun `gh-23812 SpEL cannot invoke Kotlin synthetic classes`() {
		val expr = parser.parseExpression("new org.springframework.expression.spel.SpelReproKotlinTests\$Config().kotlinSupplier().invoke()")
		assertThat(expr.getValue(context)).isEqualTo("test")
	}

	@Test
	fun `gh-26867 SpEL can process Kotlin regular function parameter`() {
		val expr = parser.parseExpression("#key.startsWith('hello')")
		context.registerFunction("get", Config::class.java.getMethod("get", String::class.java))
		context.setVariable("key", "hello world")
		assertThat(expr.getValue(context, Boolean::class.java)).isTrue()
		context.setVariable("key", "")
		assertThat(expr.getValue(context, Boolean::class.java)).isFalse()
	}

	@Test
	fun `gh-26867 SpEL can process Kotlin suspending function parameter`() {
		val expr = parser.parseExpression("#key.startsWith('hello')")
		context.registerFunction("suspendingGet", Config::class.java.getMethod("suspendingGet", String::class.java, Continuation::class.java))
		context.setVariable("key", "hello world")
		assertThat(expr.getValue(context, Boolean::class.java)).isTrue()
		context.setVariable("key", "")
		assertThat(expr.getValue(context, Boolean::class.java)).isFalse()
	}

	@Test
	fun `gh-30468 Unmangle Kotlin inlined class getter`() {
		context.setVariable("something", Something(UUID(123), "name"))
		val expr = parser.parseExpression("#something.id")
		assertThat(expr.getValue(context, Int::class.java)).isEqualTo(123)
	}

	@Test
	fun `gh-30468 Unmangle Kotlin inlined class setter`() {
		context.setVariable("something", Something(UUID(123), "name"))
		val expr = parser.parseExpression("#something.id = 456")
		assertThat(expr.getValue(context, Int::class.java)).isEqualTo(456)
	}

	@Suppress("UNUSED_PARAMETER")
	class Config {

		fun kotlinSupplier(): () -> String {
			return { "test" }
		}

		fun get(key: String): Any {
			throw NotImplementedError()
		}

		suspend fun suspendingGet(key: String) {
			throw NotImplementedError()
		}

	}

	@JvmInline value class UUID(val value: Int)

	data class Something(
		var id: UUID,
		var name: String,
	)
}
