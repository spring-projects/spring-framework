package org.springframework.expression.spel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser

class KotlinSpelReproTests {

	private val parser: ExpressionParser = SpelExpressionParser()

	private val context = TestScenarioCreator.getTestEvaluationContext()


	@Test
	fun `gh-23812 SpEL cannot invoke Kotlin synthetic classes`() {
		val expr = parser.parseExpression("new org.springframework.expression.spel.KotlinSpelReproTests\$Config().kotlinSupplier().invoke()")
		assertThat(expr.getValue(context)).isEqualTo("test")
	}

	class Config {

		fun kotlinSupplier(): () -> String {
			return { "test" }
		}

	}
}
