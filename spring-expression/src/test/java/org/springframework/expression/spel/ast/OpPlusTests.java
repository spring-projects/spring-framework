/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel.ast;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for SpEL's {@link OpPlus} operator.
 *
 * @author Ivo Smid
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.2
 */
class OpPlusTests {

	private final ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());


	@Test
	void emptyOperands() {
		assertThatIllegalArgumentException().isThrownBy(() -> new OpPlus(-1, -1));
	}

	@Test
	void unaryPlusWithStringLiteral() {
		StringLiteral stringLiteral = new StringLiteral("word", -1, -1, "word");

		OpPlus operator = new OpPlus(-1, -1, stringLiteral);
		assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> operator.getValueInternal(expressionState));
	}

	@Test
	void unaryPlusWithIntegerOperand() {
		IntLiteral intLiteral = new IntLiteral("123", -1, -1, 123);
		OpPlus operator = new OpPlus(-1, -1, intLiteral);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Integer.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(value.getValue()).isEqualTo(intLiteral.getLiteralValue().getValue());
	}

	@Test
	void unaryPlusWithLongOperand() {
		LongLiteral longLiteral = new LongLiteral("123", -1, -1, 123L);
		OpPlus operator = new OpPlus(-1, -1, longLiteral);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Long.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(Long.class);
		assertThat(value.getValue()).isEqualTo(longLiteral.getLiteralValue().getValue());
	}

	@Test
	void unaryPlusWithRealOperand() {
		RealLiteral realLiteral = new RealLiteral("123.00", -1, -1, 123.0);
		OpPlus operator = new OpPlus(-1, -1, realLiteral);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Double.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(Double.class);
		assertThat(value.getValue()).isEqualTo(realLiteral.getLiteralValue().getValue());
	}

	@Test
	void binaryPlusWithIntegerOperands() {
		IntLiteral n1 = new IntLiteral("123", -1, -1, 123);
		IntLiteral n2 = new IntLiteral("456", -1, -1, 456);
		OpPlus operator = new OpPlus(-1, -1, n1, n2);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Integer.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(value.getValue()).isEqualTo(123 + 456);
	}

	@Test
	void binaryPlusWithLongOperands() {
		LongLiteral n1 = new LongLiteral("123", -1, -1, 123L);
		LongLiteral n2 = new LongLiteral("456", -1, -1, 456L);
		OpPlus operator = new OpPlus(-1, -1, n1, n2);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Long.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(Long.class);
		assertThat(value.getValue()).isEqualTo(123L + 456L);
	}

	@Test
	void binaryPlusWithRealOperands() {
		RealLiteral n1 = new RealLiteral("123.00", -1, -1, 123.0);
		RealLiteral n2 = new RealLiteral("456.00", -1, -1, 456.0);
		OpPlus operator = new OpPlus(-1, -1, n1, n2);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Double.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(Double.class);
		assertThat(value.getValue()).isEqualTo(123.0 + 456.0);
	}

	@Test
	void binaryPlusWithStringOperands() {
		StringLiteral str1 = new StringLiteral("\"foo\"", -1, -1, "\"foo\"");
		StringLiteral str2 = new StringLiteral("\"bar\"", -1, -1, "\"bar\"");
		OpPlus operator = new OpPlus(-1, -1, str1, str2);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(value.getValue()).isEqualTo("foobar");
	}

	@Test
	void binaryPlusWithLeftStringOperand() {
		StringLiteral stringLiteral = new StringLiteral("\"number is \"", -1, -1, "\"number is \"");
		LongLiteral longLiteral = new LongLiteral("123", -1, -1, 123);
		OpPlus operator = new OpPlus(-1, -1, stringLiteral, longLiteral);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(value.getValue()).isEqualTo("number is 123");
	}

	@Test
	void binaryPlusWithRightStringOperand() {
		LongLiteral longLiteral = new LongLiteral("123", -1, -1, 123);
		StringLiteral stringLiteral = new StringLiteral("\" is a number\"", -1, -1, "\" is a number\"");
		OpPlus operator = new OpPlus(-1, -1, longLiteral, stringLiteral);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(value.getValue()).isEqualTo("123 is a number");
	}

	@Test
	void binaryPlusWithSqlTimeToString() {
		Time time = new Time(new Date().getTime());

		VariableReference var = new VariableReference("timeVar", -1, -1);
		var.setValue(expressionState, time);

		StringLiteral stringLiteral = new StringLiteral("\" is now\"", -1, -1, "\" is now\"");
		OpPlus operator = new OpPlus(-1, -1, var, stringLiteral);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(value.getValue()).isEqualTo(time + " is now");
	}

	@Test
	void binaryPlusWithTimeConverted() {
		SimpleDateFormat format = new SimpleDateFormat("hh :--: mm :--: ss", Locale.ENGLISH);

		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(Time.class, String.class, format::format);

		StandardEvaluationContext evaluationContextConverter = new StandardEvaluationContext();
		evaluationContextConverter.setTypeConverter(new StandardTypeConverter(conversionService));

		ExpressionState expressionState = new ExpressionState(evaluationContextConverter);
		Time time = new Time(new Date().getTime());

		VariableReference var = new VariableReference("timeVar", -1, -1);
		var.setValue(expressionState, time);

		StringLiteral stringLiteral = new StringLiteral("\" is now\"", -1, -1, "\" is now\"");
		OpPlus operator = new OpPlus(-1, -1, var, stringLiteral);
		TypedValue value = operator.getValueInternal(expressionState);

		assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
		assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(value.getValue()).isEqualTo(format.format(time) + " is now");
	}

}
