/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.expression.spel;

import junit.framework.Assert;

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

/**
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public class TemplateExpressionParsingTests extends ExpressionTestCase {

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
		Assert.assertEquals("hello world", o.toString());
	}

	@Test
	public void testParsingSimpleTemplateExpression02() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("hello ${'to'} you", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		Assert.assertEquals("hello to you", o.toString());
	}

	@Test
	public void testParsingSimpleTemplateExpression03() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("The quick ${'brown'} fox jumped over the ${'lazy'} dog",
				DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		Assert.assertEquals("The quick brown fox jumped over the lazy dog", o.toString());
	}

	@Test
	public void testParsingSimpleTemplateExpression04() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("${'hello'} world", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Object o = expr.getValue();
		Assert.assertEquals("hello world", o.toString());

		expr = parser.parseExpression("", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		o = expr.getValue();
		Assert.assertEquals("", o.toString());

		expr = parser.parseExpression("abc", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		o = expr.getValue();
		Assert.assertEquals("abc", o.toString());

		expr = parser.parseExpression("abc", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		o = expr.getValue((Object)null);
		Assert.assertEquals("abc", o.toString());
	}

	@Test
	public void testCompositeStringExpression() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression ex = parser.parseExpression("hello ${'world'}", DEFAULT_TEMPLATE_PARSER_CONTEXT);
		checkString("hello world", ex.getValue());
		checkString("hello world", ex.getValue(String.class));
		checkString("hello world", ex.getValue((Object)null, String.class));
		checkString("hello world", ex.getValue(new Rooty()));
		checkString("hello world", ex.getValue(new Rooty(), String.class));

		EvaluationContext ctx = new StandardEvaluationContext();
		checkString("hello world", ex.getValue(ctx));
		checkString("hello world", ex.getValue(ctx, String.class));
		checkString("hello world", ex.getValue(ctx, null, String.class));
		checkString("hello world", ex.getValue(ctx, new Rooty()));
		checkString("hello world", ex.getValue(ctx, new Rooty(), String.class));
		checkString("hello world", ex.getValue(ctx, new Rooty(), String.class));
		Assert.assertEquals("hello ${'world'}", ex.getExpressionString());
		Assert.assertFalse(ex.isWritable(new StandardEvaluationContext()));
		Assert.assertFalse(ex.isWritable(new Rooty()));
		Assert.assertFalse(ex.isWritable(new StandardEvaluationContext(), new Rooty()));

		Assert.assertEquals(String.class,ex.getValueType());
		Assert.assertEquals(String.class,ex.getValueType(ctx));
		Assert.assertEquals(String.class,ex.getValueTypeDescriptor().getType());
		Assert.assertEquals(String.class,ex.getValueTypeDescriptor(ctx).getType());
		Assert.assertEquals(String.class,ex.getValueType(new Rooty()));
		Assert.assertEquals(String.class,ex.getValueType(ctx, new Rooty()));
		Assert.assertEquals(String.class,ex.getValueTypeDescriptor(new Rooty()).getType());
		Assert.assertEquals(String.class,ex.getValueTypeDescriptor(ctx, new Rooty()).getType());

		try {
			ex.setValue(ctx, null);
			Assert.fail();
		} catch (EvaluationException ee) {
			// success
		}
		try {
			ex.setValue((Object)null, null);
			Assert.fail();
		} catch (EvaluationException ee) {
			// success
		}
		try {
			ex.setValue(ctx, null, null);
			Assert.fail();
		} catch (EvaluationException ee) {
			// success
		}
	}

	static class Rooty {}

	@Test
	public void testNestedExpressions() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		// treat the nested ${..} as a part of the expression
		Expression ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		String s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		Assert.assertEquals("hello 4 world",s);

		// not a useful expression but tests nested expression syntax that clashes with template prefix/suffix
		ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1]==3]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		Assert.assertEquals(CompositeStringExpression.class,ex.getClass());
		CompositeStringExpression cse = (CompositeStringExpression)ex;
		Expression[] exprs = cse.getExpressions();
		Assert.assertEquals(3,exprs.length);
		Assert.assertEquals("listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1]==3]",exprs[1].getExpressionString());
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		Assert.assertEquals("hello  world",s);

		ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} ${listOfNumbersUpToTen.$[#this>5]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		Assert.assertEquals("hello 4 10 world",s);

		try {
			ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} ${listOfNumbersUpToTen.$[#this>5] world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
			Assert.fail("Should have failed");
		} catch (ParseException pe) {
			Assert.assertEquals("No ending suffix '}' for expression starting at character 41: ${listOfNumbersUpToTen.$[#this>5] world",pe.getMessage());
		}

		try {
			ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1==3]} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
			Assert.fail("Should have failed");
		} catch (ParseException pe) {
			Assert.assertEquals("Found closing '}' at position 74 but most recent opening is '[' at position 30",pe.getMessage());
		}
	}

	@Test

	public void testClashingWithSuffixes() throws Exception {
		// Just wanting to use the prefix or suffix within the template:
		Expression ex = parser.parseExpression("hello ${3+4} world",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		String s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		Assert.assertEquals("hello 7 world",s);

		ex = parser.parseExpression("hello ${3+4} wo${'${'}rld",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		Assert.assertEquals("hello 7 wo${rld",s);

		ex = parser.parseExpression("hello ${3+4} wo}rld",DEFAULT_TEMPLATE_PARSER_CONTEXT);
		s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(),String.class);
		Assert.assertEquals("hello 7 wo}rld",s);
	}

	@Test
	public void testParsingNormalExpressionThroughTemplateParser() throws Exception {
		Expression expr = parser.parseExpression("1+2+3");
		Assert.assertEquals(6,expr.getValue());
		expr = parser.parseExpression("1+2+3",null);
		Assert.assertEquals(6,expr.getValue());
	}

	@Test
	public void testErrorCases() throws Exception {
		try {
			parser.parseExpression("hello ${'world'", DEFAULT_TEMPLATE_PARSER_CONTEXT);
			Assert.fail("Should have failed");
		} catch (ParseException pe) {
			Assert.assertEquals("No ending suffix '}' for expression starting at character 6: ${'world'",pe.getMessage());
			Assert.assertEquals("hello ${'world'",pe.getExpressionString());
		}
		try {
			parser.parseExpression("hello ${'wibble'${'world'}", DEFAULT_TEMPLATE_PARSER_CONTEXT);
			Assert.fail("Should have failed");
		} catch (ParseException pe) {
			Assert.assertEquals("No ending suffix '}' for expression starting at character 6: ${'wibble'${'world'}",pe.getMessage());
		}
		try {
			parser.parseExpression("hello ${} world", DEFAULT_TEMPLATE_PARSER_CONTEXT);
			Assert.fail("Should have failed");
		} catch (ParseException pe) {
			Assert.assertEquals("No expression defined within delimiter '${}' at character 6",pe.getMessage());
		}
	}

	@Test
	public void testTemplateParserContext() {
		TemplateParserContext tpc = new TemplateParserContext("abc","def");
		Assert.assertEquals("abc", tpc.getExpressionPrefix());
		Assert.assertEquals("def", tpc.getExpressionSuffix());
		Assert.assertTrue(tpc.isTemplate());

		tpc = new TemplateParserContext();
		Assert.assertEquals("#{", tpc.getExpressionPrefix());
		Assert.assertEquals("}", tpc.getExpressionSuffix());
		Assert.assertTrue(tpc.isTemplate());

		ParserContext pc = ParserContext.TEMPLATE_EXPRESSION;
		Assert.assertEquals("#{", pc.getExpressionPrefix());
		Assert.assertEquals("}", pc.getExpressionSuffix());
		Assert.assertTrue(pc.isTemplate());
	}

	// ---

	private void checkString(String expectedString, Object value) {
		if (!(value instanceof String)) {
			Assert.fail("Result was not a string, it was of type " + value.getClass() + "  (value=" + value + ")");
		}
		if (!value.equals(expectedString)) {
			Assert.fail("Did not get expected result.  Should have been '" + expectedString + "' but was '" + value + "'");
		}
	}

}
