/*
 * Copyright 2002-present the original author or authors.
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

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockPageContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Keith Donald
 */
class EvalTagTests extends AbstractTagTests {

	private EvalTag tag;

	private MockPageContext context;


	@BeforeEach
	void setup() {
		LocaleContextHolder.setDefaultLocale(Locale.UK);

		FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
		factory.afterPropertiesSet();
		context = createPageContext();
		context.getRequest().setAttribute("org.springframework.core.convert.ConversionService", factory.getObject());
		context.getRequest().setAttribute("bean", new Bean());

		tag = new EvalTag();
		tag.setPageContext(context);
	}

	@AfterEach
	void reset() {
		LocaleContextHolder.setDefaultLocale(null);
	}


	@Test
	void printScopedAttributeResult() throws Exception {
		tag.setExpression("bean.method()");
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(((MockHttpServletResponse) context.getResponse()).getContentAsString()).isEqualTo("foo");
	}

	@Test
	void printNullAsEmptyString() throws Exception {
		tag.setExpression("bean.null");
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(((MockHttpServletResponse) context.getResponse()).getContentAsString()).isEmpty();
	}

	@Test
	void printFormattedScopedAttributeResult() throws Exception {
		tag.setExpression("bean.formattable");
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(((MockHttpServletResponse) context.getResponse()).getContentAsString()).isEqualTo("25%");
	}

	@Test
	void printHtmlEscapedAttributeResult() throws Exception {
		tag.setExpression("bean.html()");
		tag.setHtmlEscape(true);
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(((MockHttpServletResponse) context.getResponse()).getContentAsString()).isEqualTo("&lt;p&gt;");
	}

	@Test
	void printJavaScriptEscapedAttributeResult() throws Exception {
		tag.setExpression("bean.js()");
		tag.setJavaScriptEscape(true);
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(((MockHttpServletResponse) context.getResponse()).getContentAsString()).isEqualTo("function foo() { alert(\\\"hi\\\") }");
	}

	@Test
	void setFormattedScopedAttributeResult() throws Exception {
		tag.setExpression("bean.formattable");
		tag.setVar("foo");
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(context.getAttribute("foo")).isEqualTo(new BigDecimal(".25"));
	}

	@Test  // SPR-6923
	public void nestedPropertyWithAttributeName() throws Exception {
		tag.setExpression("bean.bean");
		tag.setVar("foo");
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(context.getAttribute("foo")).isEqualTo("not the bean object");
	}

	@Test
	void accessUsingBeanSyntax() throws Exception {
		GenericApplicationContext wac = (GenericApplicationContext)
				context.getRequest().getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		wac.getDefaultListableBeanFactory().registerSingleton("bean2", context.getRequest().getAttribute("bean"));
		tag.setExpression("@bean2.bean");
		tag.setVar("foo");
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(context.getAttribute("foo")).isEqualTo("not the bean object");
	}

	@Test
	void environmentAccess() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("key.foo", "value.foo");
		GenericApplicationContext wac = (GenericApplicationContext)
		context.getRequest().getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		wac.getEnvironment().getPropertySources().addFirst(new MapPropertySource("mapSource", map));
		wac.getDefaultListableBeanFactory().registerSingleton("bean2", context.getRequest().getAttribute("bean"));
		tag.setExpression("@environment['key.foo']");
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(((MockHttpServletResponse) context.getResponse()).getContentAsString()).isEqualTo("value.foo");
	}

	@Test
	void mapAccess() throws Exception {
		tag.setExpression("bean.map.key");
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(((MockHttpServletResponse) context.getResponse()).getContentAsString()).isEqualTo("value");
	}

	@Test
	void resolveImplicitVariable() throws Exception {
		ELContext elContext = mock();
		ELResolver elResolver = mock();
		given(elContext.getELResolver()).willReturn(elResolver);
		given(elResolver.getValue(any(ELContext.class), isNull(), eq("pageContext"))).willReturn(context);
		((ExtendedMockPageContext) context).setELContext(elContext);

		tag.setExpression("pageContext.getClass().getSimpleName()");
		int action = tag.doStartTag();
		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		action = tag.doEndTag();
		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(((MockHttpServletResponse) context.getResponse()).getContentAsString()).isEqualTo("ExtendedMockPageContext");
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
			Map<String, Object> map = new HashMap<>();
			map.put("key", "value");
			return map;
		}
	}

}
