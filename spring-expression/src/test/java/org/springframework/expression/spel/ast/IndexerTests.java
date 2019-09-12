package org.springframework.expression.spel.ast;

import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.testresources.Inventor;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Joyce Zhan
 */
public class IndexerTests {
	@Test
	public void testAccess() {
		SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
		SpelExpression expression = (SpelExpression) new SpelExpressionParser().parseExpression("stringArrayOfThreeItems[3]");
		assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
				expression.getValue(context, new Inventor()));
	}
}