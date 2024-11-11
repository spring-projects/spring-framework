/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageTag}.
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Nicholas Williams
 */
class MessageTagTests extends AbstractTagTests {

	@Test
	void messageTagWithMessageSourceResolvable() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setMessage(new DefaultMessageSourceResolvable("test"));
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test message");
	}

	@Test
	void messageTagWithCode() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("test");
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test message");
	}

	@Test
	void messageTagWithCodeAndArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments("arg1");
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test arg1 message {1}");
	}

	@Test
	void messageTagWithCodeAndArguments() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments("arg1,arg2");
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test arg1 message arg2");
	}

	@Test
	void messageTagWithCodeAndStringArgumentWithCustomSeparator() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments("arg1,1;arg2,2");
		tag.setArgumentSeparator(";");
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test arg1,1 message arg2,2");
	}

	@Test
	void messageTagWithCodeAndArrayArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments(new Object[] {"arg1", 5});
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test arg1 message 5");
	}

	@Test
	void messageTagWithCodeAndObjectArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments(5);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test 5 message {1}");
	}

	@Test
	void messageTagWithCodeAndArgumentAndNestedArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		tag.setArguments(5);
		tag.addArgument(7);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test 5 message 7");
	}

	@Test
	void messageTagWithCodeAndNestedArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		tag.addArgument(7);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test 7 message {1}");
	}

	@Test
	void messageTagWithCodeAndNestedArguments() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		tag.addArgument("arg1");
		tag.addArgument(6);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test arg1 message 6");
	}

	@Test
	void messageTagWithCodeAndText() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setText("testtext");
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat((message.toString())).as("Correct message").isEqualTo("test message");
	}

	@Test
	void messageTagWithText() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("test & text é");
		tag.setHtmlEscape(true);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").startsWith("test &amp; text &");
	}

	@Test
	void messageTagWithTextEncodingEscaped() throws JspException {
		PageContext pc = createPageContext();
		pc.getServletContext().setInitParameter(WebUtils.RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM, "true");
		pc.getResponse().setCharacterEncoding("UTF-8");
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("test <&> é");
		tag.setHtmlEscape(true);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("test &lt;&amp;&gt; é");
	}

	@Test
	void messageTagWithTextAndJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("' test & text \\");
		tag.setJavaScriptEscape(true);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("\\' test & text \\\\");
	}

	@Test
	void messageTagWithTextAndHtmlEscapeAndJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuilder message = new StringBuilder();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("' test & text \\");
		tag.setHtmlEscape(true);
		tag.setJavaScriptEscape(true);
		assertThat(tag.doStartTag()).as("Correct doStartTag return value").isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(message.toString()).as("Correct message").isEqualTo("&#39; test &amp; text \\\\");
	}

	@Test
	void messageWithVarAndScope() throws JspException {
		PageContext pc = createPageContext();
		MessageTag tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setText("text & text");
		tag.setVar("testvar");
		tag.setScope("page");
		tag.doStartTag();
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(pc.getAttribute("testvar")).isEqualTo("text & text");
		tag.release();

		tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setVar("testvar2");
		tag.doStartTag();
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(pc.getAttribute("testvar2")).as("Correct message").isEqualTo("test message");
		tag.release();
	}

	@Test
	void messageWithVar() throws JspException {
		PageContext pc = createPageContext();
		MessageTag tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setText("text & text");
		tag.setVar("testvar");
		tag.doStartTag();
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(pc.getAttribute("testvar")).isEqualTo("text & text");
		tag.release();

		// try to reuse
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setVar("testvar");

		tag.doStartTag();
		assertThat(tag.doEndTag()).as("Correct doEndTag return value").isEqualTo(Tag.EVAL_PAGE);
		assertThat(pc.getAttribute("testvar")).as("Correct message").isEqualTo("test message");
	}

	@Test
	void requestContext() {
		PageContext pc = createPageContext();
		RequestContext rc = new RequestContext((HttpServletRequest) pc.getRequest(), pc.getServletContext());
		assertThat(rc.getMessage("test")).isEqualTo("test message");
		assertThat(rc.getMessage("test", (Object[]) null)).isEqualTo("test message");
		assertThat(rc.getMessage("test", "default")).isEqualTo("test message");
		assertThat(rc.getMessage("test", (Object[]) null, "default")).isEqualTo("test message");
		assertThat(rc.getMessage("testArgs", new String[]{"arg1", "arg2"}, "default")).isEqualTo("test arg1 message arg2");
		assertThat(rc.getMessage("testArgs", Arrays.asList("arg1", "arg2"), "default")).isEqualTo("test arg1 message arg2");
		assertThat(rc.getMessage("testa", "default")).isEqualTo("default");
		assertThat(rc.getMessage("testa", Collections.emptyList(), "default")).isEqualTo("default");
		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(new String[] {"test"});
		assertThat(rc.getMessage(resolvable)).isEqualTo("test message");
	}

}
