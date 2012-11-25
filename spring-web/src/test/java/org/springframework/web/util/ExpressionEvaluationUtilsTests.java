/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.util;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.Expression;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.FunctionMapper;
import javax.servlet.jsp.el.VariableResolver;

import org.junit.Test;

import org.springframework.mock.web.MockExpressionEvaluator;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;

import static org.junit.Assert.*;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @since 16.09.2003
 */
public class ExpressionEvaluationUtilsTests {

	@Test
	public void testIsSpringJspExpressionSupportActive() {
		MockServletContext sc = new MockServletContext();
		PageContext pc = new MockPageContext(sc);
		assertFalse(ExpressionEvaluationUtils.isSpringJspExpressionSupportActive(pc));
		sc.addInitParameter("springJspExpressionSupport", "true");
		assertTrue(ExpressionEvaluationUtils.isSpringJspExpressionSupportActive(pc));
	}

	@Test
	public void testIsSpringJspExpressionSupportActiveOnServlet30() {
		MockServletContext sc = new MockServletContext();
		sc.setMajorVersion(3);
		sc.setMinorVersion(0);
		PageContext pc = new MockPageContext(sc);
		assertFalse(ExpressionEvaluationUtils.isSpringJspExpressionSupportActive(pc));
		sc.addInitParameter("springJspExpressionSupport", "true");
		assertTrue(ExpressionEvaluationUtils.isSpringJspExpressionSupportActive(pc));
	}

	@Test
	public void testIsSpringJspExpressionSupportActiveOnServlet30WithServlet23Application() {
		MockServletContext sc = new MockServletContext();
		sc.setMajorVersion(3);
		sc.setMinorVersion(0);
		sc.setEffectiveMajorVersion(2);
		sc.setEffectiveMinorVersion(3);
		PageContext pc = new MockPageContext(sc);
		assertTrue(ExpressionEvaluationUtils.isSpringJspExpressionSupportActive(pc));
		sc.addInitParameter("springJspExpressionSupport", "false");
		assertFalse(ExpressionEvaluationUtils.isSpringJspExpressionSupportActive(pc));
	}

	@Test
	public void testIsExpressionLanguage() {
		assertTrue(ExpressionEvaluationUtils.isExpressionLanguage("${bla}"));
		assertTrue(ExpressionEvaluationUtils.isExpressionLanguage("bla${bla}"));
		assertFalse(ExpressionEvaluationUtils.isExpressionLanguage("bla{bla"));
		assertFalse(ExpressionEvaluationUtils.isExpressionLanguage("bla$b{"));
	}

	@Test
	public void testEvaluate() throws Exception {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("springJspExpressionSupport", "true");
		MockPageContext ctx = new MockPageContext(sc);
		ctx.setAttribute("bla", "blie");

		assertEquals("blie", ExpressionEvaluationUtils.evaluate("test", "${bla}", String.class, ctx));
		assertEquals("test", ExpressionEvaluationUtils.evaluate("test", "test", String.class, ctx));

		try {
			ExpressionEvaluationUtils.evaluate("test", "test", Float.class, ctx);
			fail("Should have thrown JspException");
		}
		catch (JspException ex) {
			// expected
		}
	}

	@Test
	public void testEvaluateWithConcatenation() throws Exception {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("springJspExpressionSupport", "true");
		MockPageContext ctx = new MockPageContext(sc);
		ctx.setAttribute("bla", "blie");

		String expr = "text${bla}text${bla}text";
		Object o = ExpressionEvaluationUtils.evaluate("test", expr, String.class, ctx);
		assertEquals("textblietextblietext", o);

		expr = "${bla}text${bla}text";
		o = ExpressionEvaluationUtils.evaluate("test", expr, String.class, ctx);
		assertEquals("blietextblietext", o);

		expr = "${bla}text${bla}";
		o = ExpressionEvaluationUtils.evaluate("test", expr, String.class, ctx);
		assertEquals("blietextblie", o);

		expr = "${bla}text${bla}";
		o = ExpressionEvaluationUtils.evaluate("test", expr, Object.class, ctx);
		assertEquals("blietextblie", o);

		try {
			expr = "${bla}text${bla";
			ExpressionEvaluationUtils.evaluate("test", expr, Object.class, ctx);
			fail("Should have thrown JspException");
		}
		catch (JspException ex) {
			// expected
		}

		try {
			expr = "${bla}text${bla}";
			ExpressionEvaluationUtils.evaluate("test", expr, Float.class, ctx);
			fail("Should have thrown JspException");
		}
		catch (JspException ex) {
			// expected
		}
	}

	@Test
	public void testEvaluateString() throws Exception {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("springJspExpressionSupport", "true");
		MockPageContext ctx = new MockPageContext(sc);
		ctx.setAttribute("bla", "blie");

		assertEquals("blie", ExpressionEvaluationUtils.evaluateString("test", "${bla}", ctx));
		assertEquals("blie", ExpressionEvaluationUtils.evaluateString("test", "blie", ctx));
	}

	@Test
	public void testEvaluateStringWithConcatenation() throws Exception {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("springJspExpressionSupport", "true");
		MockPageContext ctx = new MockPageContext(sc);
		ctx.setAttribute("bla", "blie");

		String expr = "text${bla}text${bla}text";
		String s = ExpressionEvaluationUtils.evaluateString("test", expr, ctx);
		assertEquals("textblietextblietext", s);

		expr = "${bla}text${bla}text";
		s = ExpressionEvaluationUtils.evaluateString("test", expr, ctx);
		assertEquals("blietextblietext", s);

		expr = "${bla}text${bla}";
		s = ExpressionEvaluationUtils.evaluateString("test", expr, ctx);
		assertEquals("blietextblie", s);

		expr = "${bla}text${bla}";
		s = ExpressionEvaluationUtils.evaluateString("test", expr, ctx);
		assertEquals("blietextblie", s);

		try {
			expr = "${bla}text${bla";
			ExpressionEvaluationUtils.evaluateString("test", expr, ctx);
			fail("Should have thrown JspException");
		}
		catch (JspException ex) {
			// expected
		}

	}

	@Test
	public void testEvaluateInteger() throws Exception {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("springJspExpressionSupport", "true");
		MockPageContext ctx = new MockPageContext(sc);
		ctx.setAttribute("bla", new Integer(1));

		assertEquals(1, ExpressionEvaluationUtils.evaluateInteger("test", "${bla}", ctx));
		assertEquals(21, ExpressionEvaluationUtils.evaluateInteger("test", "21", ctx));
	}

	@Test
	public void testEvaluateBoolean() throws Exception {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("springJspExpressionSupport", "true");
		MockPageContext ctx = new MockPageContext(sc);
		ctx.setAttribute("bla", new Boolean(true));

		assertTrue(ExpressionEvaluationUtils.evaluateBoolean("test", "${bla}", ctx));
		assertTrue(ExpressionEvaluationUtils.evaluateBoolean("test", "true", ctx));
	}

	@Test
	public void testRepeatedEvaluate() throws Exception {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("springJspExpressionSupport", "true");
		PageContext ctx = new CountingMockPageContext(sc);
		CountingMockExpressionEvaluator eval = (CountingMockExpressionEvaluator) ctx.getExpressionEvaluator();
		ctx.setAttribute("bla", "blie");
		ctx.setAttribute("blo", "blue");

		assertEquals("blie", ExpressionEvaluationUtils.evaluate("test", "${bla}", String.class, ctx));
		assertEquals(1, eval.evaluateCount);

		assertEquals("blue", ExpressionEvaluationUtils.evaluate("test", "${blo}", String.class, ctx));
		assertEquals(2, eval.evaluateCount);

		assertEquals("blie", ExpressionEvaluationUtils.evaluate("test", "${bla}", String.class, ctx));
		assertEquals(3, eval.evaluateCount);

		assertEquals("blue", ExpressionEvaluationUtils.evaluate("test", "${blo}", String.class, ctx));
		assertEquals(4, eval.evaluateCount);
	}

	@Test
	public void testEvaluateWithComplexConcatenation() throws Exception {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("springJspExpressionSupport", "true");
		PageContext ctx = new CountingMockPageContext(sc);
		CountingMockExpressionEvaluator eval = (CountingMockExpressionEvaluator) ctx.getExpressionEvaluator();
		ctx.setAttribute("bla", "blie");
		ctx.setAttribute("blo", "blue");

		String expr = "text${bla}text${blo}text";
		Object o = ExpressionEvaluationUtils.evaluate("test", expr, String.class, ctx);
		assertEquals("textblietextbluetext", o);
		assertEquals(2, eval.evaluateCount);

		expr = "${bla}text${blo}text";
		o = ExpressionEvaluationUtils.evaluate("test", expr, String.class, ctx);
		assertEquals("blietextbluetext", o);
		assertEquals(4, eval.evaluateCount);

		expr = "${bla}text${blo}";
		o = ExpressionEvaluationUtils.evaluate("test", expr, String.class, ctx);
		assertEquals("blietextblue", o);
		assertEquals(6, eval.evaluateCount);

		expr = "${bla}text${blo}";
		o = ExpressionEvaluationUtils.evaluate("test", expr, Object.class, ctx);
		assertEquals("blietextblue", o);
		assertEquals(8, eval.evaluateCount);
	}


	private static class CountingMockPageContext extends MockPageContext {

		public CountingMockPageContext(ServletContext servletContext) {
			super(servletContext);
		}

		private ExpressionEvaluator eval = new CountingMockExpressionEvaluator(this);

		public ExpressionEvaluator getExpressionEvaluator() {
			return eval;
		}
	}


	private static class CountingMockExpressionEvaluator extends MockExpressionEvaluator {

		public int parseExpressionCount = 0;

		public int evaluateCount = 0;

		public CountingMockExpressionEvaluator(PageContext pageContext) {
			super(pageContext);
		}

		public Expression parseExpression(String expression, Class expectedType, FunctionMapper functionMapper) throws ELException {
			this.parseExpressionCount++;
			return super.parseExpression(expression, expectedType, functionMapper);
		}

		public Object evaluate(String expression, Class expectedType, VariableResolver variableResolver, FunctionMapper functionMapper) throws ELException {
			this.evaluateCount++;
			return super.evaluate(expression, expectedType, variableResolver, functionMapper);
		}
	}

}
