/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.junit.Test;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;

/**
 * Tests for {@link MessageTag}.
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Nicholas Williams
 */
@SuppressWarnings("serial")
public class MessageTagTests extends AbstractTagTests {

	@Test
	public void messageTagWithMessageSourceResolvable() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setMessage(new DefaultMessageSourceResolvable("test"));
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", message.toString());
	}

	@Test
	public void messageTagWithCode() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("test");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", message.toString());
	}

	@Test
	public void messageTagWithCodeAndArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments("arg1");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1 message {1}", message.toString());
	}

	@Test
	public void messageTagWithCodeAndArguments() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments("arg1,arg2");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1 message arg2", message.toString());
	}

	@Test
	public void messageTagWithCodeAndStringArgumentWithCustomSeparator() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
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
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1,1 message arg2,2", message.toString());
	}

	@Test
	public void messageTagWithCodeAndArrayArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments(new Object[] {"arg1", 5});
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1 message 5", message.toString());
	}

	@Test
	public void messageTagWithCodeAndObjectArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments(5);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test 5 message {1}", message.toString());
	}

	@Test
	public void messageTagWithCodeAndArgumentAndNestedArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		tag.setArguments(5);
		tag.addArgument(7);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test 5 message 7", message.toString());
	}

	@Test
	public void messageTagWithCodeAndNestedArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		tag.addArgument(7);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test 7 message {1}", message.toString());
	}

	@Test
	public void messageTagWithCodeAndNestedArguments() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		tag.addArgument("arg1");
		tag.addArgument(6);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1 message 6", message.toString());
	}

	@Test
	public void messageTagWithCodeAndText() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setText("testtext");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", (message.toString()));
	}

	@Test
	public void messageTagWithText() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("test & text é");
		tag.setHtmlEscape(true);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertTrue("Correct message", message.toString().startsWith("test &amp; text &"));
	}

	@Test
	public void messageTagWithTextEncodingEscaped() throws JspException {
		PageContext pc = createPageContext();
		pc.getServletContext().setInitParameter(WebUtils.RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM, "true");
		pc.getResponse().setCharacterEncoding("UTF-8");
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("test <&> é");
		tag.setHtmlEscape(true);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test &lt;&amp;&gt; é", message.toString());
	}

	@Test
	public void messageTagWithTextAndJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("' test & text \\");
		tag.setJavaScriptEscape(true);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "\\' test & text \\\\", message.toString());
	}

	@Test
	public void messageTagWithTextAndHtmlEscapeAndJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
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
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "&#39; test &amp; text \\\\", message.toString());
	}

	@Test
	public void messageWithVarAndScope() throws JspException {
		PageContext pc = createPageContext();
		MessageTag tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setText("text & text");
		tag.setVar("testvar");
		tag.setScope("page");
		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("text & text", pc.getAttribute("testvar"));
		tag.release();

		tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setVar("testvar2");
		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", pc.getAttribute("testvar2"));
		tag.release();
	}

	@Test
	public void messageWithVar() throws JspException {
		PageContext pc = createPageContext();
		MessageTag tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setText("text & text");
		tag.setVar("testvar");
		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("text & text", pc.getAttribute("testvar"));
		tag.release();

		// try to reuse
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setVar("testvar");

		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", pc.getAttribute("testvar"));
	}

	@Test
	public void nullMessageSource() throws JspException {
		PageContext pc = createPageContext();
		ConfigurableWebApplicationContext ctx = (ConfigurableWebApplicationContext)
				RequestContextUtils.findWebApplicationContext((HttpServletRequest) pc.getRequest(), pc.getServletContext());
		ctx.close();

		MessageTag tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setVar("testvar2");
		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void requestContext() throws ServletException {
		PageContext pc = createPageContext();
		RequestContext rc = new RequestContext((HttpServletRequest) pc.getRequest(), pc.getServletContext());
		assertEquals("test message", rc.getMessage("test"));
		assertEquals("test message", rc.getMessage("test", (Object[]) null));
		assertEquals("test message", rc.getMessage("test", "default"));
		assertEquals("test message", rc.getMessage("test", (Object[]) null, "default"));
		assertEquals("test arg1 message arg2",
				rc.getMessage("testArgs", new String[] {"arg1", "arg2"}, "default"));
		assertEquals("test arg1 message arg2",
				rc.getMessage("testArgs", Arrays.asList(new String[] {"arg1", "arg2"}), "default"));
		assertEquals("default", rc.getMessage("testa", "default"));
		assertEquals("default", rc.getMessage("testa", (List) null, "default"));
		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(new String[] {"test"});
		assertEquals("test message", rc.getMessage(resolvable));
	}

}
