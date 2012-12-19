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

package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @author Alef Arendsen
 */
public class HtmlEscapeTagTests extends AbstractTagTests {

	@SuppressWarnings("serial")
	public void testHtmlEscapeTag() throws JspException {
		PageContext pc = createPageContext();
		HtmlEscapeTag tag = new HtmlEscapeTag();
		tag.setPageContext(pc);
		tag.doStartTag();
		HtmlEscapingAwareTag testTag = new HtmlEscapingAwareTag() {
			public int doStartTagInternal() throws Exception {
				return EVAL_BODY_INCLUDE;
			}
		};
		testTag.setPageContext(pc);
		testTag.doStartTag();

		assertTrue("Correct default", !tag.getRequestContext().isDefaultHtmlEscape());
		assertTrue("Correctly applied", !testTag.isHtmlEscape());
		tag.setDefaultHtmlEscape("true");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertTrue("Correctly enabled", tag.getRequestContext().isDefaultHtmlEscape());
		assertTrue("Correctly applied", testTag.isHtmlEscape());
		tag.setDefaultHtmlEscape("false");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertTrue("Correctly disabled", !tag.getRequestContext().isDefaultHtmlEscape());
		assertTrue("Correctly applied", !testTag.isHtmlEscape());

		tag.setDefaultHtmlEscape("true");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		testTag.setHtmlEscape("true");
		assertTrue("Correctly enabled", tag.getRequestContext().isDefaultHtmlEscape());
		assertTrue("Correctly applied", testTag.isHtmlEscape());
		testTag.setHtmlEscape("false");
		assertTrue("Correctly enabled", tag.getRequestContext().isDefaultHtmlEscape());
		assertTrue("Correctly applied", !testTag.isHtmlEscape());
		tag.setDefaultHtmlEscape("false");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		testTag.setHtmlEscape("true");
		assertTrue("Correctly disabled", !tag.getRequestContext().isDefaultHtmlEscape());
		assertTrue("Correctly applied", testTag.isHtmlEscape());
		testTag.setHtmlEscape("false");
		assertTrue("Correctly disabled", !tag.getRequestContext().isDefaultHtmlEscape());
		assertTrue("Correctly applied", !testTag.isHtmlEscape());
	}

	public void testHtmlEscapeTagWithContextParamTrue() throws JspException {
		PageContext pc = createPageContext();
		MockServletContext sc = (MockServletContext) pc.getServletContext();
		sc.addInitParameter(WebUtils.HTML_ESCAPE_CONTEXT_PARAM, "true");
		HtmlEscapeTag tag = new HtmlEscapeTag();
		tag.setDefaultHtmlEscape("false");
		tag.setPageContext(pc);
		tag.doStartTag();

		assertTrue("Correct default", !tag.getRequestContext().isDefaultHtmlEscape());
		tag.setDefaultHtmlEscape("true");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertTrue("Correctly enabled", tag.getRequestContext().isDefaultHtmlEscape());
		tag.setDefaultHtmlEscape("false");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertTrue("Correctly disabled", !tag.getRequestContext().isDefaultHtmlEscape());
	}

	public void testHtmlEscapeTagWithContextParamFalse() throws JspException {
		PageContext pc = createPageContext();
		MockServletContext sc = (MockServletContext) pc.getServletContext();
		HtmlEscapeTag tag = new HtmlEscapeTag();
		tag.setPageContext(pc);
		tag.doStartTag();

		sc.addInitParameter(WebUtils.HTML_ESCAPE_CONTEXT_PARAM, "false");
		assertTrue("Correct default", !tag.getRequestContext().isDefaultHtmlEscape());
		tag.setDefaultHtmlEscape("true");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertTrue("Correctly enabled", tag.getRequestContext().isDefaultHtmlEscape());
		tag.setDefaultHtmlEscape("false");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertTrue("Correctly disabled", !tag.getRequestContext().isDefaultHtmlEscape());
	}

	@SuppressWarnings("serial")
	public void testEscapeBody() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer result = new StringBuffer();
		EscapeBodyTag tag = new EscapeBodyTag() {
			protected String readBodyContent() {
				return "test text";
			}
			protected void writeBodyContent(String content) {
				result.append(content);
			}
		};
		tag.setPageContext(pc);
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, tag.doStartTag());
		assertEquals(Tag.SKIP_BODY, tag.doAfterBody());
		assertEquals("test text", result.toString());
	}

	@SuppressWarnings("serial")
	public void testEscapeBodyWithHtmlEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer result = new StringBuffer();
		EscapeBodyTag tag = new EscapeBodyTag() {
			protected String readBodyContent() {
				return "test & text";
			}
			protected void writeBodyContent(String content) {
				result.append(content);
			}
		};
		tag.setPageContext(pc);
		tag.setHtmlEscape("true");
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, tag.doStartTag());
		assertEquals(Tag.SKIP_BODY, tag.doAfterBody());
		assertEquals("test &amp; text", result.toString());
	}

	@SuppressWarnings("serial")
	public void testEscapeBodyWithJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer result = new StringBuffer();
		EscapeBodyTag tag = new EscapeBodyTag() {
			protected String readBodyContent() {
				return "' test & text \\";
			}
			protected void writeBodyContent(String content) {
				result.append(content);
			}
		};
		tag.setPageContext(pc);
		tag.setJavaScriptEscape("true");
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, tag.doStartTag());
		assertEquals(Tag.SKIP_BODY, tag.doAfterBody());
		assertEquals("Correct content", "\\' test & text \\\\", result.toString());
	}

	@SuppressWarnings("serial")
	public void testEscapeBodyWithHtmlEscapeAndJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer result = new StringBuffer();
		EscapeBodyTag tag = new EscapeBodyTag() {
			protected String readBodyContent() {
				return "' test & text \\";
			}
			protected void writeBodyContent(String content) {
				result.append(content);
			}
		};
		tag.setPageContext(pc);
		tag.setHtmlEscape("true");
		tag.setJavaScriptEscape("true");
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, tag.doStartTag());
		assertEquals(Tag.SKIP_BODY, tag.doAfterBody());
		assertEquals("Correct content", "&#39; test &amp; text \\\\", result.toString());
	}

}
