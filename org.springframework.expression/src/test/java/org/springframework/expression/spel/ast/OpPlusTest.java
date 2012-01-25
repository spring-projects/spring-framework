/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

import static org.junit.Assert.*;

/**
 * The plus operator test
 *
 * @author Ivo Smid
 * @since 3.0
 */
public class OpPlusTest {

	@Test(expected = IllegalArgumentException.class)
	public void test_emptyOperands() {
		new OpPlus(-1);
	}

	@Test(expected = SpelEvaluationException.class)
	public void test_unaryPlusWithStringLiteral() {
		ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

		StringLiteral str = new StringLiteral("word", -1, "word");

		OpPlus o = new OpPlus(-1, str);
		o.getValueInternal(expressionState);
	}

	@Test
	public void test_unaryPlusWithNumberOperand() {
		ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

		{
			RealLiteral realLiteral = new RealLiteral("123.00", -1, 123.0);
			OpPlus o = new OpPlus(-1, realLiteral);
			TypedValue value = o.getValueInternal(expressionState);

			assertEquals(Double.class, value.getTypeDescriptor().getObjectType());
			assertEquals(Double.class, value.getTypeDescriptor().getType());
			assertEquals(realLiteral.getLiteralValue().getValue(), value.getValue());
		}

		{
			IntLiteral intLiteral = new IntLiteral("123", -1, 123);
			OpPlus o = new OpPlus(-1, intLiteral);
			TypedValue value = o.getValueInternal(expressionState);

			assertEquals(Integer.class, value.getTypeDescriptor().getObjectType());
			assertEquals(Integer.class, value.getTypeDescriptor().getType());
			assertEquals(intLiteral.getLiteralValue().getValue(), value.getValue());
		}

		{
			LongLiteral longLiteral = new LongLiteral("123", -1, 123L);
			OpPlus o = new OpPlus(-1, longLiteral);
			TypedValue value = o.getValueInternal(expressionState);

			assertEquals(Long.class, value.getTypeDescriptor().getObjectType());
			assertEquals(Long.class, value.getTypeDescriptor().getType());
			assertEquals(longLiteral.getLiteralValue().getValue(), value.getValue());
		}


	}

	@Test
	public void test_binaryPlusWithNumberOperands() {
		ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

		{
			RealLiteral n1 = new RealLiteral("123.00", -1, 123.0);
			RealLiteral n2 = new RealLiteral("456.00", -1, 456.0);
			OpPlus o = new OpPlus(-1, n1, n2);
			TypedValue value = o.getValueInternal(expressionState);

			assertEquals(Double.class, value.getTypeDescriptor().getObjectType());
			assertEquals(Double.class, value.getTypeDescriptor().getType());
			assertEquals(Double.valueOf(123.0 + 456.0), value.getValue());
		}

		{
			LongLiteral n1 = new LongLiteral("123", -1, 123L);
			LongLiteral n2 = new LongLiteral("456", -1, 456L);
			OpPlus o = new OpPlus(-1, n1, n2);
			TypedValue value = o.getValueInternal(expressionState);

			assertEquals(Long.class, value.getTypeDescriptor().getObjectType());
			assertEquals(Long.class, value.getTypeDescriptor().getType());
			assertEquals(Long.valueOf(123L + 456L), value.getValue());
		}

		{
			IntLiteral n1 = new IntLiteral("123", -1, 123);
			IntLiteral n2 = new IntLiteral("456", -1, 456);
			OpPlus o = new OpPlus(-1, n1, n2);
			TypedValue value = o.getValueInternal(expressionState);

			assertEquals(Integer.class, value.getTypeDescriptor().getObjectType());
			assertEquals(Integer.class, value.getTypeDescriptor().getType());
			assertEquals(Integer.valueOf(123 + 456), value.getValue());
		}

	}

	@Test
	public void test_binaryPlusWithStringOperands() {
		ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

		StringLiteral n1 = new StringLiteral("\"foo\"", -1, "\"foo\"");
		StringLiteral n2 = new StringLiteral("\"bar\"", -1, "\"bar\"");
		OpPlus o = new OpPlus(-1, n1, n2);
		TypedValue value = o.getValueInternal(expressionState);

		assertEquals(String.class, value.getTypeDescriptor().getObjectType());
		assertEquals(String.class, value.getTypeDescriptor().getType());
		assertEquals("foobar", value.getValue());

	}

	@Test
	public void test_binaryPlusWithLeftStringOperand() {
		ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

		StringLiteral n1 = new StringLiteral("\"number is \"", -1, "\"number is \"");
		LongLiteral n2 = new LongLiteral("123", -1, 123);
		OpPlus o = new OpPlus(-1, n1, n2);
		TypedValue value = o.getValueInternal(expressionState);

		assertEquals(String.class, value.getTypeDescriptor().getObjectType());
		assertEquals(String.class, value.getTypeDescriptor().getType());
		assertEquals("number is 123", value.getValue());

	}

	@Test
	public void test_binaryPlusWithRightStringOperand() {
		ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

		LongLiteral n1 = new LongLiteral("123", -1, 123);
		StringLiteral n2 = new StringLiteral("\" is a number\"", -1, "\" is a number\"");
		OpPlus o = new OpPlus(-1, n1, n2);
		TypedValue value = o.getValueInternal(expressionState);

		assertEquals(String.class, value.getTypeDescriptor().getObjectType());
		assertEquals(String.class, value.getTypeDescriptor().getType());
		assertEquals("123 is a number", value.getValue());

	}

	@Test
	public void test_binaryPlusWithTime_ToString() {

		ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

		Time time = new Time(new Date().getTime());

		VariableReference var = new VariableReference("timeVar", -1);
		var.setValue(expressionState, time);

		StringLiteral n2 = new StringLiteral("\" is now\"", -1, "\" is now\"");
		OpPlus o = new OpPlus(-1, var, n2);
		TypedValue value = o.getValueInternal(expressionState);

		assertEquals(String.class, value.getTypeDescriptor().getObjectType());
		assertEquals(String.class, value.getTypeDescriptor().getType());
		assertEquals(time + " is now", value.getValue());

	}

	@Test
	public void test_binaryPlusWithTimeConverted() {

		final SimpleDateFormat format = new SimpleDateFormat("hh :--: mm :--: ss", Locale.ENGLISH);
		
		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(new Converter<Time, String>() {
			public String convert(Time source) {
				return format.format(source);
			}
		});

		StandardEvaluationContext evaluationContextConverter = new StandardEvaluationContext();
		evaluationContextConverter.setTypeConverter(new StandardTypeConverter(conversionService));

		ExpressionState expressionState = new ExpressionState(evaluationContextConverter);

		Time time = new Time(new Date().getTime());

		VariableReference var = new VariableReference("timeVar", -1);
		var.setValue(expressionState, time);


		StringLiteral n2 = new StringLiteral("\" is now\"", -1, "\" is now\"");
		OpPlus o = new OpPlus(-1, var, n2);
		TypedValue value = o.getValueInternal(expressionState);

		assertEquals(String.class, value.getTypeDescriptor().getObjectType());
		assertEquals(String.class, value.getTypeDescriptor().getType());
		assertEquals(format.format(time) + " is now", value.getValue());

	}


}
