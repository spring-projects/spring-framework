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
public class LiteralExpressionTests {

	@Test
	public void testGetValue() throws Exception {
		LiteralExpression lEx = new LiteralExpression("somevalue");
		assertThat(lEx.getValue()).isInstanceOf(String.class).isEqualTo("somevalue");
		assertThat(lEx.getValue(String.class)).isInstanceOf(String.class).isEqualTo("somevalue");
		EvaluationContext ctx = new StandardEvaluationContext();
		assertThat(lEx.getValue(ctx)).isInstanceOf(String.class).isEqualTo("somevalue");
		assertThat(lEx.getValue(ctx, String.class)).isInstanceOf(String.class).isEqualTo("somevalue");
		assertThat(lEx.getValue(new Rooty())).isInstanceOf(String.class).isEqualTo("somevalue");
		assertThat(lEx.getValue(new Rooty(), String.class)).isInstanceOf(String.class).isEqualTo("somevalue");
		assertThat(lEx.getValue(ctx, new Rooty())).isInstanceOf(String.class).isEqualTo("somevalue");
		assertThat(lEx.getValue(ctx, new Rooty(),String.class)).isInstanceOf(String.class).isEqualTo("somevalue");
		assertThat(lEx.getExpressionString()).isEqualTo("somevalue");
		assertThat(lEx.isWritable(new StandardEvaluationContext())).isFalse();
		assertThat(lEx.isWritable(new Rooty())).isFalse();
		assertThat(lEx.isWritable(new StandardEvaluationContext(), new Rooty())).isFalse();
	}

	static class Rooty {}

	@Test
	public void testSetValue() {
		assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
				new LiteralExpression("somevalue").setValue(new StandardEvaluationContext(), "flibble"))
			.satisfies(ex -> assertThat(ex.getExpressionString()).isEqualTo("somevalue"));
		assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
				new LiteralExpression("somevalue").setValue(new Rooty(), "flibble"))
			.satisfies(ex -> assertThat(ex.getExpressionString()).isEqualTo("somevalue"));
		assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
				new LiteralExpression("somevalue").setValue(new StandardEvaluationContext(), new Rooty(), "flibble"))
			.satisfies(ex -> assertThat(ex.getExpressionString()).isEqualTo("somevalue"));
	}

	@Test
	public void testGetValueType() throws Exception {
		LiteralExpression lEx = new LiteralExpression("somevalue");
		assertThat(lEx.getValueType()).isEqualTo(String.class);
		assertThat(lEx.getValueType(new StandardEvaluationContext())).isEqualTo(String.class);
		assertThat(lEx.getValueType(new Rooty())).isEqualTo(String.class);
		assertThat(lEx.getValueType(new StandardEvaluationContext(), new Rooty())).isEqualTo(String.class);
		assertThat(lEx.getValueTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(lEx.getValueTypeDescriptor(new StandardEvaluationContext()).getType()).isEqualTo(String.class);
		assertThat(lEx.getValueTypeDescriptor(new Rooty()).getType()).isEqualTo(String.class);
		assertThat(lEx.getValueTypeDescriptor(new StandardEvaluationContext(), new Rooty()).getType()).isEqualTo(String.class);
	}

}
