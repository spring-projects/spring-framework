/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.tags;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.jsp.tagext.Tag;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.format.number.PercentStyleFormatter;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockPageContext;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.Assert.*;

/**
 * @author Keith Donald
 */
public class EvalTagTests extends AbstractTagTests {

	private EvalTag tag;

	private MockPageContext context;

	@Before
	public void setUp() throws Exception {
		context = createPageContext();
		FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
		factory.afterPropertiesSet();
		context.getRequest().setAttribute("org.springframework.core.convert.ConversionService", factory.getObject());
		context.getRequest().setAttribute("bean", new Bean());
		tag = new EvalTag();
		tag.setPageContext(context);
	}

	@Test
	public void printScopedAttributeResult() throws Exception {
		tag.setExpression("bean.method()");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("foo", ((MockHttpServletResponse) context.getResponse()).getContentAsString());
	}

	@Test
	public void printNullAsEmptyString() throws Exception {
		tag.setExpression("bean.null");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("", ((MockHttpServletResponse) context.getResponse()).getContentAsString());
	}

	@Test
	public void printFormattedScopedAttributeResult() throws Exception {
		PercentStyleFormatter formatter = new PercentStyleFormatter();
		tag.setExpression("bean.formattable");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals(formatter.print(new BigDecimal(".25"), Locale.getDefault()),
				((MockHttpServletResponse) context.getResponse()).getContentAsString());
	}

	@Test
	public void printHtmlEscapedAttributeResult() throws Exception {
		tag.setExpression("bean.html()");
		tag.setHtmlEscape(true);
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("&lt;p&gt;", ((MockHttpServletResponse) context.getResponse()).getContentAsString());
	}

	@Test
	public void printJavaScriptEscapedAttributeResult() throws Exception {
		tag.setExpression("bean.js()");
		tag.setJavaScriptEscape(true);
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("function foo() { alert(\\\"hi\\\") }",
				((MockHttpServletResponse)context.getResponse()).getContentAsString());
	}

	@Test
	public void setFormattedScopedAttributeResult() throws Exception {
		tag.setExpression("bean.formattable");
		tag.setVar("foo");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals(new BigDecimal(".25"), context.getAttribute("foo"));
	}

	// SPR-6923
	@Test
	public void nestedPropertyWithAttributeName() throws Exception {
		tag.setExpression("bean.bean");
		tag.setVar("foo");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("not the bean object", context.getAttribute("foo"));
	}

	@Test
	public void accessUsingBeanSyntax() throws Exception {
		GenericApplicationContext wac = (GenericApplicationContext)
				context.getRequest().getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		wac.getDefaultListableBeanFactory().registerSingleton("bean2", context.getRequest().getAttribute("bean"));
		tag.setExpression("@bean2.bean");
		tag.setVar("foo");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("not the bean object", context.getAttribute("foo"));
	}

	@Test
	public void environmentAccess() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("key.foo", "value.foo");
		GenericApplicationContext wac = (GenericApplicationContext)
		context.getRequest().getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		wac.getEnvironment().getPropertySources().addFirst(new MapPropertySource("mapSource", map));
		wac.getDefaultListableBeanFactory().registerSingleton("bean2", context.getRequest().getAttribute("bean"));
		tag.setExpression("@environment['key.foo']");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("value.foo", ((MockHttpServletResponse) context.getResponse()).getContentAsString());
	}

	@Test
	public void mapAccess() throws Exception {
		tag.setExpression("bean.map.key");
		int action = tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, action);
		action = tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("value", ((MockHttpServletResponse) context.getResponse()).getContentAsString());
	}



	public static class Bean {

		public String method() {
			return "foo";
		}

		@NumberFormat(style=Style.PERCENT)
		public BigDecimal getFormattable() {
			return new BigDecimal(".25");
		}

		public String html() {
			return "<p>";
		}

		public String getBean() {
			return "not the bean object";
		}

		public Object getNull() {
			return null;
		}

		public String js() {
			return "function foo() { alert(\"hi\") }";
		}

		public Map<String, Object> getMap() {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("key", "value");
			return map;
		}
	}

}
