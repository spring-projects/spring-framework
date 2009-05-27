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
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectivePropertyResolver;

/**
 * Tests based on Jiras up to the release of Spring 3.0.0
 * 
 * @author Andy Clement
 */
public class SpringEL300Tests extends ExpressionTestCase {

	@Test
	public void testNPE_SPR5661() {
		evaluate("joinThreeStrings('a',null,'c')", "anullc", String.class);
	}
	
	@Test
	public void testNPE_SPR5673() throws Exception {
		ParserContext hashes = TemplateExpressionParsingTests.HASH_DELIMITED_PARSER_CONTEXT;
		ParserContext dollars = TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT;
		
		checkTemplateParsing("abc${'def'} ghi","abcdef ghi");
		
		checkTemplateParsingError("abc${ {}( 'abc'","Missing closing ')' for '(' at position 8");
		checkTemplateParsingError("abc${ {}[ 'abc'","Missing closing ']' for '[' at position 8");
		checkTemplateParsingError("abc${ {}{ 'abc'","Missing closing '}' for '{' at position 8");
		checkTemplateParsingError("abc${ ( 'abc' }","Found closing '}' at position 14 but most recent opening is '(' at position 6");
		checkTemplateParsingError("abc${ '... }","Found non terminating string literal starting at position 6");
		checkTemplateParsingError("abc${ \"... }","Found non terminating string literal starting at position 6");
		checkTemplateParsingError("abc${ ) }","Found closing ')' at position 6 without an opening '('");
		checkTemplateParsingError("abc${ ] }","Found closing ']' at position 6 without an opening '['");
		checkTemplateParsingError("abc${ } }","No expression defined within delimiter '${}' at character 3");
		checkTemplateParsingError("abc$[ } ]",DOLLARSQUARE_TEMPLATE_PARSER_CONTEXT,"Found closing '}' at position 6 without an opening '{'");
		
		checkTemplateParsing("abc ${\"def''g}hi\"} jkl","abc def'g}hi jkl");
		checkTemplateParsing("abc ${'def''g}hi'} jkl","abc def'g}hi jkl");
		checkTemplateParsing("}","}");
		checkTemplateParsing("${'hello'} world","hello world");
		checkTemplateParsing("Hello ${'}'}]","Hello }]");
		checkTemplateParsing("Hello ${'}'}","Hello }");
		checkTemplateParsingError("Hello ${ ( ","No ending suffix '}' for expression starting at character 6: ${ ( ");
		checkTemplateParsingError("Hello ${ ( }","Found closing '}' at position 11 but most recent opening is '(' at position 9");
		checkTemplateParsing("#{'Unable to render embedded object: File ({#this == 2}'}", hashes,"Unable to render embedded object: File ({#this == 2}");
		checkTemplateParsing("This is the last odd number in the list: ${listOfNumbersUpToTen.$[#this%2==1]}",dollars,"This is the last odd number in the list: 9");
		checkTemplateParsing("Hello ${'here is a curly bracket }'}",dollars,"Hello here is a curly bracket }");
		checkTemplateParsing("He${'${'}llo ${'here is a curly bracket }'}}",dollars,"He${llo here is a curly bracket }}");
		checkTemplateParsing("Hello ${'()()()}{}{}{][]{}{][}[][][}{()()'} World",dollars,"Hello ()()()}{}{}{][]{}{][}[][][}{()() World");
		checkTemplateParsing("Hello ${'inner literal that''s got {[(])]}an escaped quote in it'} World","Hello inner literal that's got {[(])]}an escaped quote in it World");
		checkTemplateParsingError("Hello ${","No ending suffix '}' for expression starting at character 6: ${");
	}
	
	@Test
	public void testAccessingNullPropertyViaReflection_SPR5663() throws AccessException {
		PropertyAccessor propertyAccessor = new ReflectivePropertyResolver();
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		Assert.assertFalse(propertyAccessor.canRead(context, null, "abc"));
		Assert.assertFalse(propertyAccessor.canWrite(context, null, "abc"));
		try {
			propertyAccessor.read(context, null, "abc");
			Assert.fail("Should have failed with an AccessException");
		} catch (AccessException ae) {
			// success
		}
		try {
			propertyAccessor.write(context, null, "abc","foo");
			Assert.fail("Should have failed with an AccessException");
		} catch (AccessException ae) {
			// success
		}
	}
	
	
	// ---

	private void checkTemplateParsing(String expression, String expectedValue) throws Exception {
		checkTemplateParsing(expression,TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT, expectedValue);
	}
	
	private void checkTemplateParsing(String expression, ParserContext context, String expectedValue) throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression(expression,context);
		Assert.assertEquals(expectedValue,expr.getValue(TestScenarioCreator.getTestEvaluationContext()));
	}

	private void checkTemplateParsingError(String expression,String expectedMessage) throws Exception {
		checkTemplateParsingError(expression, TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT,expectedMessage);
	}
	
	private void checkTemplateParsingError(String expression,ParserContext context, String expectedMessage) throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		try {
			parser.parseExpression(expression,context);
			Assert.fail("Should have failed");
		} catch (Exception e) {
			if (!e.getMessage().equals(expectedMessage)) {
				e.printStackTrace();
			}
			Assert.assertEquals(expectedMessage,e.getMessage());
		}
	}
	
	private static final ParserContext DOLLARSQUARE_TEMPLATE_PARSER_CONTEXT = new ParserContext() {
		public String getExpressionPrefix() {
			return "$[";
		}
		public String getExpressionSuffix() {
			return "]";
		}
		public boolean isTemplate() {
			return true;
		}
	};
	

}
