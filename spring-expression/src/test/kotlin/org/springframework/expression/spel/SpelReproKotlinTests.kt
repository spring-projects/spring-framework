/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		assertThat(expr.getValue(context, Boolean::class.java)!!).isTrue()
		context.setVariable("key", "")
		assertThat(expr.getValue(context, Boolean::class.java)!!).isFalse()
	}

	@Test
	fun `gh-26867 SpEL can process Kotlin suspending function parameter`() {
		val expr = parser.parseExpression("#key.startsWith('hello')")
		context.registerFunction("suspendingGet", Config::class.java.getMethod("suspendingGet", String::class.java, Continuation::class.java))
		context.setVariable("key", "hello world")
		assertThat(expr.getValue(context, Boolean::class.java)!!).isTrue()
		context.setVariable("key", "")
		assertThat(expr.getValue(context, Boolean::class.java)!!).isFalse()
	}

	@Test
	fun `gh-30468 Unmangle Kotlin inlined class getter`() {
		context.setVariable("something", Something(UUID(123), "name"))
		val expr = parser.parseExpression("#something.id")
		assertThat(expr.getValue(context, Int::class.java)!!).isEqualTo(123)
	}

	@Test
	fun `gh-30468 Unmangle Kotlin inlined class setter`() {
		context.setVariable("something", Something(UUID(123), "name"))
		val expr = parser.parseExpression("#something.id = 456")
		assertThat(expr.getValue(context, Int::class.java)!!).isEqualTo(456)
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
