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

import org.junit.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public class TemplateExpressionParsingTests extends AbstractExpressionTests {

	public static final ParserContext DEFAULT_TEMPLATE_PARSER_CONTEXT = new ParserContext() {
		@Override
		public String getExpressionPrefix() {
			return "${";
		}
		@Override
		public String getExpressionSuffix() {
			return "}";
		}
		@Override
		public boolean isTemplate() {
			return true;
		}
	};

	public static final ParserContext HASH_DELIMITED_PARSER_CONTEXT = new ParserContext() {
		@Override
		public String getExpressionPrefix() {
			return "#{";
		}
		@Override
		public String getExpressionSuffix() {
			return "}";
		}
		@Override
		public boolean isTemplate() {
			return true;
		}
	};


	@Test
	public void testParsingSimpleTemplateExpression01() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("hello ${'world'}", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertEquals("hello world", o.toString());
	}

	@Test
	public void testParsingSimpleTemplateExpression02() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("hello ${'to'} you", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertEquals("hello to you", o.toString());
	}

	@Test
	public void testParsingSimpleTemplateExpression03() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("The quick ${'brown'} fox jumped over the ${'lazy'} dog",
				DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertEquals("The quick brown fox jumped over the lazy dog", o.toString());
	}

	@Test
	public void testParsingSimpleTemplateExpression04() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("${'hello'} world", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertEquals("hello world", o.toString());

		expr = parser.parseExpression("", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		o = expr.getValue();
		assertEquals("", o.toString());

		expr = parser.parseExpression("abc", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		o = expr.getValue();
		assertEquals("abc", o.toString());

		expr = parser.parseExpression("abc", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		o = expr.getValue((Object)null);
		assertEquals("abc", o.toString());
	}

	@Test
	public void testCompositeStringExpression() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression ex = parser.parseExpression("hello ${'world'}", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		assertThat(ex.getValue()).isInstanceOf(String.class).isEqualTo("hello world");
		assertThat(ex.getValue(String.class)).isInstanceOf(String.class).isEqualTo("hello world");
		assertThat(ex.getValue((Object)null, String.class)).isInstanceOf(String.class).isEqualTo("hello world");
		assertThat(ex.getValue(new Rooty())).isInstanceOf(String.class).isEqualTo("hello world");
		assertThat(ex.getValue(new Rooty(), String.class)).isInstanceOf(String.class).isEqualTo("hello world");

		EvaluationContext ctx = new StandardEvaluationContext();
		assertThat(ex.getValue(ctx)).isInstanceOf(String.class).isEqualTo("hello world");
		assertThat(ex.getValue(ctx, String.class)).isInstanceOf(String.class).isEqualTo("hello world");
		assertThat(ex.getValue(ctx, null, String.class)).isInstanceOf(String.class).isEqualTo("hello world");
		assertThat(ex.getValue(ctx, new Rooty())).isInstanceOf(String.class).isEqualTo("hello world");
		assertThat(ex.getValue(ctx, new Rooty(), String.class)).isInstanceOf(String.class).isEqualTo("hello world");
		assertThat(ex.getValue(ctx, new Rooty(), String.class)).isInstanceOf(String.class).isEqualTo("hello world");
		assertEquals("hello ${'world'}", ex.getExpressionString());
		assertFalse(ex.isWritable(new StandardEvaluationContext()));
		assertFalse(ex.isWritable(new Rooty()));
		assertFalse(ex.isWritable(new StandardEvaluationContext(), new Rooty()));

		assertEquals(String.class,ex.getValueType());
		assertEquals(String.class,ex.getValueType(ctx));
		assertEquals(String.class,ex.getValueTypeDescriptor().getType());
		assertEquals(String.class,ex.getValueTypeDescriptor(ctx).getType());
		assertEquals(String.class,ex.getValueType(new Rooty()));
		assertEquals(String.class,ex.getValueType(ctx, new Rooty()));
		assertEquals(String.class,ex.getValueTypeDescriptor(new Rooty()).getType());
		assertEquals(String.class,ex.getValueTypeDescriptor(ctx, new Rooty()).getType());
		assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
				ex.setValue(ctx, null));
		assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
				ex.setValue((Object)null, null));
		assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
				ex.setValue(ctx, null, null));
	}

	static class Rooty {}

	@Test
	public void testNestedExpressions() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		// treat the nested ${..} as a part of the expression
		Expression ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		String s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertEquals("hello 4 world",s);

		// not a useful expression but tests nested expression syntax that clashes with template prefix/suffix
		ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1]==3]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		assertEquals(CompositeStringExpression.class,ex.getClass());
		CompositeStringExpression cse = (CompositeStringExpression)ex;
		Expression[] exprs = cse.getExpressions();
		assertEquals(3,exprs.length);
		assertEquals("listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1]==3]",exprs[1].getExpressionString());
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertEquals("hello  world",s);

		ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} ${listOfNumbersUpToTen.$[#this>5]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertEquals("hello 4 10 world",s);

		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} ${listOfNumbersUpToTen.$[#this>5] world",DEFAULT_TEMPLATE_PARSER_CONTEXT))
			.satisfies(pex -> assertThat(pex.getSimpleMessage()).isEqualTo("No ending suffix '}' for expression starting at character 41: ${listOfNumbersUpToTen.$[#this>5] world"));

		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				parser.parseExpression("hello ${listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1==3]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT))
			.satisfies(pex -> assertThat(pex.getSimpleMessage()).isEqualTo("Found closing '}' at position 74 but most recent opening is '[' at position 30"));
	}

	@Test

	public void testClashingWithSuffixes() throws Exception {
		// Just wanting to use the prefix or suffix within the template:
		Expression ex = parser.parseExpression("hello ${3+4} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		String s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertEquals("hello 7 world", s);

		ex = parser.parseExpression("hello ${3+4} wo${'${'}rld",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertEquals("hello 7 wo${rld", s);

		ex = parser.parseExpression("hello ${3+4} wo}rld",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertEquals("hello 7 wo}rld", s);
	}

	@Test
	public void testParsingNormalExpressionThroughTemplateParser() throws Exception {
		Expression expr = parser.parseExpression("1+2+3");
		assertEquals(6, expr.getValue());
	}

	@Test
	public void testErrorCases() throws Exception {
		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				parser.parseExpression("hello ${'world'", DEFAULT_TEMPLATE_PARSER_CONTEXT))
			.satisfies(pex -> {
				assertThat(pex.getSimpleMessage()).isEqualTo("No ending suffix '}' for expression starting at character 6: ${'world'");
				assertThat(pex.getExpressionString()).isEqualTo("hello ${'world'");
			});
		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				parser.parseExpression("hello ${'wibble'${'world'}", DEFAULT_TEMPLATE_PARSER_CONTEXT))
			.satisfies(pex -> assertThat(pex.getSimpleMessage()).isEqualTo("No ending suffix '}' for expression starting at character 6: ${'wibble'${'world'}"));
		assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
				parser.parseExpression("hello ${} world", DEFAULT_TEMPLATE_PARSER_CONTEXT))
			.satisfies(pex -> assertThat(pex.getSimpleMessage()).isEqualTo("No expression defined within delimiter '${}' at character 6"));
	}

	@Test
	public void testTemplateParserContext() {
		TemplateParserContext tpc = new TemplateParserContext("abc","def");
		assertEquals("abc", tpc.getExpressionPrefix());
		assertEquals("def", tpc.getExpressionSuffix());
		assertTrue(tpc.isTemplate());

		tpc = new TemplateParserContext();
		assertEquals("#{", tpc.getExpressionPrefix());
		assertEquals("}", tpc.getExpressionSuffix());
		assertTrue(tpc.isTemplate());

		ParserContext pc = ParserContext.TEMPLATE_EXPRESSION;
		assertEquals("#{", pc.getExpressionPrefix());
		assertEquals("}", pc.getExpressionSuffix());
		assertTrue(pc.isTemplate());
	}

}
