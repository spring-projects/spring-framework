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

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.BodyTag;
import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Alef Arendsen
 */
@SuppressWarnings("serial")
class HtmlEscapeTagTests extends AbstractTagTests {

	@Test
	void htmlEscapeTag() throws JspException {
		PageContext pc = createPageContext();
		HtmlEscapeTag tag = new HtmlEscapeTag();
		tag.setPageContext(pc);
		tag.doStartTag();
		HtmlEscapingAwareTag testTag = new HtmlEscapingAwareTag() {
			@Override
			protected int doStartTagInternal() {
				return EVAL_BODY_INCLUDE;
			}
		};
		testTag.setPageContext(pc);
		testTag.doStartTag();

		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correct default").isFalse();
		assertThat(testTag.isHtmlEscape()).as("Correctly applied").isFalse();
		tag.setDefaultHtmlEscape(true);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly enabled").isTrue();
		assertThat(testTag.isHtmlEscape()).as("Correctly applied").isTrue();
		tag.setDefaultHtmlEscape(false);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly disabled").isFalse();
		assertThat(testTag.isHtmlEscape()).as("Correctly applied").isFalse();

		tag.setDefaultHtmlEscape(true);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		testTag.setHtmlEscape(true);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly enabled").isTrue();
		assertThat(testTag.isHtmlEscape()).as("Correctly applied").isTrue();
		testTag.setHtmlEscape(false);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly enabled").isTrue();
		assertThat(testTag.isHtmlEscape()).as("Correctly applied").isFalse();
		tag.setDefaultHtmlEscape(false);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		testTag.setHtmlEscape(true);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly disabled").isFalse();
		assertThat(testTag.isHtmlEscape()).as("Correctly applied").isTrue();
		testTag.setHtmlEscape(false);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly disabled").isFalse();
		assertThat(testTag.isHtmlEscape()).as("Correctly applied").isFalse();
	}

	@Test
	void htmlEscapeTagWithContextParamTrue() throws JspException {
		PageContext pc = createPageContext();
		MockServletContext sc = (MockServletContext) pc.getServletContext();
		sc.addInitParameter(WebUtils.HTML_ESCAPE_CONTEXT_PARAM, "true");
		HtmlEscapeTag tag = new HtmlEscapeTag();
		tag.setDefaultHtmlEscape(false);
		tag.setPageContext(pc);
		tag.doStartTag();

		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correct default").isFalse();
		tag.setDefaultHtmlEscape(true);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly enabled").isTrue();
		tag.setDefaultHtmlEscape(false);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly disabled").isFalse();
	}

	@Test
	void htmlEscapeTagWithContextParamFalse() throws JspException {
		PageContext pc = createPageContext();
		MockServletContext sc = (MockServletContext) pc.getServletContext();
		HtmlEscapeTag tag = new HtmlEscapeTag();
		tag.setPageContext(pc);
		tag.doStartTag();

		sc.addInitParameter(WebUtils.HTML_ESCAPE_CONTEXT_PARAM, "false");
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correct default").isFalse();
		tag.setDefaultHtmlEscape(true);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly enabled").isTrue();
		tag.setDefaultHtmlEscape(false);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.getRequestContext().isDefaultHtmlEscape()).as("Correctly disabled").isFalse();
	}

	@Test
	void escapeBody() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder result = new StringBuilder();
		EscapeBodyTag tag = new EscapeBodyTag() {
			@Override
			protected String readBodyContent() {
				return "test text";
			}
			@Override
			protected void writeBodyContent(String content) {
				result.append(content);
			}
		};
		tag.setPageContext(pc);
		assertThat(tag.doStartTag()).isEqualTo(BodyTag.EVAL_BODY_BUFFERED);
		assertThat(tag.doAfterBody()).isEqualTo(Tag.SKIP_BODY);
		assertThat(result.toString()).isEqualTo("test text");
	}

	@Test
	void escapeBodyWithHtmlEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder result = new StringBuilder();
		EscapeBodyTag tag = new EscapeBodyTag() {
			@Override
			protected String readBodyContent() {
				return "test & text";
			}
			@Override
			protected void writeBodyContent(String content) {
				result.append(content);
			}
		};
		tag.setPageContext(pc);
		tag.setHtmlEscape(true);
		assertThat(tag.doStartTag()).isEqualTo(BodyTag.EVAL_BODY_BUFFERED);
		assertThat(tag.doAfterBody()).isEqualTo(Tag.SKIP_BODY);
		assertThat(result.toString()).isEqualTo("test &amp; text");
	}

	@Test
	void escapeBodyWithJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder result = new StringBuilder();
		EscapeBodyTag tag = new EscapeBodyTag() {
			@Override
			protected String readBodyContent() {
				return "' test & text \\";
			}
			@Override
			protected void writeBodyContent(String content) {
				result.append(content);
			}
		};
		tag.setPageContext(pc);
		tag.setJavaScriptEscape(true);
		assertThat(tag.doStartTag()).isEqualTo(BodyTag.EVAL_BODY_BUFFERED);
		assertThat(tag.doAfterBody()).isEqualTo(Tag.SKIP_BODY);
		assertThat(result.toString()).as("Correct content").isEqualTo("\\' test & text \\\\");
	}

	@Test
	void escapeBodyWithHtmlEscapeAndJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder result = new StringBuilder();
		EscapeBodyTag tag = new EscapeBodyTag() {
			@Override
			protected String readBodyContent() {
				return "' test & text \\";
			}
			@Override
			protected void writeBodyContent(String content) {
				result.append(content);
			}
		};
		tag.setPageContext(pc);
		tag.setHtmlEscape(true);
		tag.setJavaScriptEscape(true);
		assertThat(tag.doStartTag()).isEqualTo(BodyTag.EVAL_BODY_BUFFERED);
		assertThat(tag.doAfterBody()).isEqualTo(Tag.SKIP_BODY);
		assertThat(result.toString()).as("Correct content").isEqualTo("&#39; test &amp; text \\\\");
	}

}
