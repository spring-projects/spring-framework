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
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
		assertThat(o.toString()).isEqualTo("hello world");
	}

	@Test
	public void testParsingSimpleTemplateExpression02() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("hello ${'to'} you", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertThat(o.toString()).isEqualTo("hello to you");
	}

	@Test
	public void testParsingSimpleTemplateExpression03() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("The quick ${'brown'} fox jumped over the ${'lazy'} dog",
				DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertThat(o.toString()).isEqualTo("The quick brown fox jumped over the lazy dog");
	}

	@Test
	public void testParsingSimpleTemplateExpression04() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("${'hello'} world", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		assertThat(o.toString()).isEqualTo("hello world");

		expr = parser.parseExpression("", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		o = expr.getValue();
		assertThat(o.toString()).isEqualTo("");

		expr = parser.parseExpression("abc", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		o = expr.getValue();
		assertThat(o.toString()).isEqualTo("abc");

		expr = parser.parseExpression("abc", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		o = expr.getValue((Object)null);
		assertThat(o.toString()).isEqualTo("abc");
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
		assertThat(ex.getExpressionString()).isEqualTo("hello ${'world'}");
		assertThat(ex.isWritable(new StandardEvaluationContext())).isFalse();
		assertThat(ex.isWritable(new Rooty())).isFalse();
		assertThat(ex.isWritable(new StandardEvaluationContext(), new Rooty())).isFalse();

		assertThat(ex.getValueType()).isEqualTo(String.class);
		assertThat(ex.getValueType(ctx)).isEqualTo(String.class);
		assertThat(ex.getValueTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(ex.getValueTypeDescriptor(ctx).getType()).isEqualTo(String.class);
		assertThat(ex.getValueType(new Rooty())).isEqualTo(String.class);
		assertThat(ex.getValueType(ctx, new Rooty())).isEqualTo(String.class);
		assertThat(ex.getValueTypeDescriptor(new Rooty()).getType()).isEqualTo(String.class);
		assertThat(ex.getValueTypeDescriptor(ctx, new Rooty()).getType()).isEqualTo(String.class);
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
		assertThat(s).isEqualTo("hello 4 world");

		// not a useful expression but tests nested expression syntax that clashes with template prefix/suffix
		ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1]==3]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		assertThat(ex.getClass()).isEqualTo(CompositeStringExpression.class);
		CompositeStringExpression cse = (CompositeStringExpression)ex;
		Expression[] exprs = cse.getExpressions();
		assertThat(exprs.length).isEqualTo(3);
		assertThat(exprs[1].getExpressionString()).isEqualTo("listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1]==3]");
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertThat(s).isEqualTo("hello  world");

		ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} ${listOfNumbersUpToTen.$[#this>5]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertThat(s).isEqualTo("hello 4 10 world");

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
		assertThat(s).isEqualTo("hello 7 world");

		ex = parser.parseExpression("hello ${3+4} wo${'${'}rld",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertThat(s).isEqualTo("hello 7 wo${rld");

		ex = parser.parseExpression("hello ${3+4} wo}rld",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		assertThat(s).isEqualTo("hello 7 wo}rld");
	}

	@Test
	public void testParsingNormalExpressionThroughTemplateParser() throws Exception {
		Expression expr = parser.parseExpression("1+2+3");
		assertThat(expr.getValue()).isEqualTo(6);
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
		assertThat(tpc.getExpressionPrefix()).isEqualTo("abc");
		assertThat(tpc.getExpressionSuffix()).isEqualTo("def");
		assertThat(tpc.isTemplate()).isTrue();

		tpc = new TemplateParserContext();
		assertThat(tpc.getExpressionPrefix()).isEqualTo("#{");
		assertThat(tpc.getExpressionSuffix()).isEqualTo("}");
		assertThat(tpc.isTemplate()).isTrue();

		ParserContext pc = ParserContext.TEMPLATE_EXPRESSION;
		assertThat(pc.getExpressionPrefix()).isEqualTo("#{");
		assertThat(pc.getExpressionSuffix()).isEqualTo("}");
		assertThat(pc.isTemplate()).isTrue();
	}

}
