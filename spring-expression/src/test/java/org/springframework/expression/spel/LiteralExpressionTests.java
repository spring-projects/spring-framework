/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Andy Clement
 */
class LiteralExpressionTests {

	private final LiteralExpression lEx = new LiteralExpression("somevalue");


	@Test
	void getValue() {
		assertThat(lEx.getValue()).isEqualTo("somevalue");
		assertThat(lEx.getValue(String.class)).isEqualTo("somevalue");
		assertThat(lEx.getValue(new Rooty())).isEqualTo("somevalue");
		assertThat(lEx.getValue(new Rooty(), String.class)).isEqualTo("somevalue");
	}

	@Test
	void getValueWithSuppliedEvaluationContext() {
		EvaluationContext ctx = new StandardEvaluationContext();
		assertThat(lEx.getValue(ctx)).isEqualTo("somevalue");
		assertThat(lEx.getValue(ctx, String.class)).isEqualTo("somevalue");
		assertThat(lEx.getValue(ctx, new Rooty())).isEqualTo("somevalue");
		assertThat(lEx.getValue(ctx, new Rooty(), String.class)).isEqualTo("somevalue");
	}

	@Test
	void getExpressionString() {
		assertThat(lEx.getExpressionString()).isEqualTo("somevalue");
	}

	@Test
	void isWritable() {
		assertThat(lEx.isWritable(new StandardEvaluationContext())).isFalse();
		assertThat(lEx.isWritable(new Rooty())).isFalse();
		assertThat(lEx.isWritable(new StandardEvaluationContext(), new Rooty())).isFalse();
	}

	@Test
	void setValue() {
		assertThatExceptionOfType(EvaluationException.class)
			.isThrownBy(() -> lEx.setValue(new StandardEvaluationContext(), "flibble"))
			.satisfies(ex -> assertThat(ex.getExpressionString()).isEqualTo("somevalue"));
		assertThatExceptionOfType(EvaluationException.class)
			.isThrownBy(() -> lEx.setValue(new Rooty(), "flibble"))
			.satisfies(ex -> assertThat(ex.getExpressionString()).isEqualTo("somevalue"));
		assertThatExceptionOfType(EvaluationException.class)
			.isThrownBy(() -> lEx.setValue(new StandardEvaluationContext(), new Rooty(), "flibble"))
			.satisfies(ex -> assertThat(ex.getExpressionString()).isEqualTo("somevalue"));
	}

	@Test
	void getValueType() {
		assertThat(lEx.getValueType()).isEqualTo(String.class);
		assertThat(lEx.getValueType(new StandardEvaluationContext())).isEqualTo(String.class);
		assertThat(lEx.getValueType(new Rooty())).isEqualTo(String.class);
		assertThat(lEx.getValueType(new StandardEvaluationContext(), new Rooty())).isEqualTo(String.class);
	}

	@Test
	void getValueTypeDescriptor() {
		assertThat(lEx.getValueTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(lEx.getValueTypeDescriptor(new StandardEvaluationContext()).getType()).isEqualTo(String.class);
		assertThat(lEx.getValueTypeDescriptor(new Rooty()).getType()).isEqualTo(String.class);
		assertThat(lEx.getValueTypeDescriptor(new StandardEvaluationContext(), new Rooty()).getType()).isEqualTo(String.class);
	}


	static class Rooty {}

}
