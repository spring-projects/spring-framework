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

package org.springframework.web.servlet.tags.form;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import org.junit.Test;

import org.springframework.mock.web.test.MockBodyContent;
import org.springframework.mock.web.test.MockPageContext;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.tags.RequestContextAwareTag;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Jeremy Grelle
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ErrorsTagTests extends AbstractFormTagTests {

	private static final String COMMAND_NAME = "testBean";

	private ErrorsTag tag;


	@Override
	@SuppressWarnings("serial")
	protected void onSetUp() {
		this.tag = new ErrorsTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPath("name");
		this.tag.setParent(getFormTag());
		this.tag.setPageContext(getPageContext());
	}

	@Override
	protected TestBean createTestBean() {
		return new TestBean();
	}


	@Test
	public void withExplicitNonWhitespaceBodyContent() throws Exception {
		String mockContent = "This is some explicit body content";
		this.tag.setBodyContent(new MockBodyContent(mockContent, getWriter()));

		// construct an errors instance of the tag
		TestBean target = new TestBean();
		target.setName("Rob Harrop");
		Errors errors = new BeanPropertyBindingResult(target, COMMAND_NAME);
		errors.rejectValue("name", "some.code", "Default Message");

		exposeBindingResult(errors);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);
		assertEquals(mockContent, getOutput());
	}

	@Test
	public void withExplicitWhitespaceBodyContent() throws Exception {
		this.tag.setBodyContent(new MockBodyContent("\t\n   ", getWriter()));

		// construct an errors instance of the tag
		TestBean target = new TestBean();
		target.setName("Rob Harrop");
		Errors errors = new BeanPropertyBindingResult(target, COMMAND_NAME);
		errors.rejectValue("name", "some.code", "Default Message");

		exposeBindingResult(errors);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertElementTagOpened(output);
		assertElementTagClosed(output);

		assertContainsAttribute(output, "id", "name.errors");
		assertBlockTagContains(output, "Default Message");
	}

	@Test
	public void withExplicitEmptyWhitespaceBodyContent() throws Exception {
		this.tag.setBodyContent(new MockBodyContent("", getWriter()));

		// construct an errors instance of the tag
		TestBean target = new TestBean();
		target.setName("Rob Harrop");
		Errors errors = new BeanPropertyBindingResult(target, COMMAND_NAME);
		errors.rejectValue("name", "some.code", "Default Message");

		exposeBindingResult(errors);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertElementTagOpened(output);
		assertElementTagClosed(output);

		assertContainsAttribute(output, "id", "name.errors");
		assertBlockTagContains(output, "Default Message");
	}

	@Test
	public void withErrors() throws Exception {
		// construct an errors instance of the tag
		TestBean target = new TestBean();
		target.setName("Rob Harrop");
		Errors errors = new BeanPropertyBindingResult(target, COMMAND_NAME);
		errors.rejectValue("name", "some.code", "Default Message");
		errors.rejectValue("name", "too.short", "Too Short");

		exposeBindingResult(errors);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertElementTagOpened(output);
		assertElementTagClosed(output);

		assertContainsAttribute(output, "id", "name.errors");
		assertBlockTagContains(output, "<br/>");
		assertBlockTagContains(output, "Default Message");
		assertBlockTagContains(output, "Too Short");
	}

	@Test
	public void withErrorsAndDynamicAttributes() throws Exception {
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		// construct an errors instance of the tag
		TestBean target = new TestBean();
		target.setName("Rob Harrop");
		Errors errors = new BeanPropertyBindingResult(target, COMMAND_NAME);
		errors.rejectValue("name", "some.code", "Default Message");
		errors.rejectValue("name", "too.short", "Too Short");

		exposeBindingResult(errors);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertElementTagOpened(output);
		assertElementTagClosed(output);

		assertContainsAttribute(output, "id", "name.errors");
		assertContainsAttribute(output, dynamicAttribute1, dynamicAttribute1);
		assertContainsAttribute(output, dynamicAttribute2, dynamicAttribute2);
		assertBlockTagContains(output, "<br/>");
		assertBlockTagContains(output, "Default Message");
		assertBlockTagContains(output, "Too Short");
	}

	@Test
	public void withEscapedErrors() throws Exception {
		// construct an errors instance of the tag
		TestBean target = new TestBean();
		target.setName("Rob Harrop");
		Errors errors = new BeanPropertyBindingResult(target, COMMAND_NAME);
		errors.rejectValue("name", "some.code", "Default <> Message");
		errors.rejectValue("name", "too.short", "Too & Short");

		exposeBindingResult(errors);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertElementTagOpened(output);
		assertElementTagClosed(output);

		assertContainsAttribute(output, "id", "name.errors");
		assertBlockTagContains(output, "<br/>");
		assertBlockTagContains(output, "Default &lt;&gt; Message");
		assertBlockTagContains(output, "Too &amp; Short");
	}

	@Test
	public void withNonEscapedErrors() throws Exception {
		this.tag.setHtmlEscape(false);

		// construct an errors instance of the tag
		TestBean target = new TestBean();
		target.setName("Rob Harrop");
		Errors errors = new BeanPropertyBindingResult(target, COMMAND_NAME);
		errors.rejectValue("name", "some.code", "Default <> Message");
		errors.rejectValue("name", "too.short", "Too & Short");

		exposeBindingResult(errors);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertElementTagOpened(output);
		assertElementTagClosed(output);

		assertContainsAttribute(output, "id", "name.errors");
		assertBlockTagContains(output, "<br/>");
		assertBlockTagContains(output, "Default <> Message");
		assertBlockTagContains(output, "Too & Short");
	}

	@Test
	public void withErrorsAndCustomElement() throws Exception {
		// construct an errors instance of the tag
		TestBean target = new TestBean();
		target.setName("Rob Harrop");
		Errors errors = new BeanPropertyBindingResult(target, COMMAND_NAME);
		errors.rejectValue("name", "some.code", "Default Message");
		errors.rejectValue("name", "too.short", "Too Short");

		exposeBindingResult(errors);

		this.tag.setElement("div");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertElementTagOpened(output);
		assertElementTagClosed(output);

		assertContainsAttribute(output, "id", "name.errors");
		assertBlockTagContains(output, "<br/>");
		assertBlockTagContains(output, "Default Message");
		assertBlockTagContains(output, "Too Short");
	}

	@Test
	public void withoutErrors() throws Exception {
		Errors errors = new BeanPropertyBindingResult(new TestBean(), "COMMAND_NAME");
		exposeBindingResult(errors);
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertEquals(0, output.length());
	}

	@Test
	public void withoutErrorsInstance() throws Exception {
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertEquals(0, output.length());
	}

	@Test
	public void asBodyTag() throws Exception {
		Errors errors = new BeanPropertyBindingResult(new TestBean(), "COMMAND_NAME");
		errors.rejectValue("name", "some.code", "Default Message");
		errors.rejectValue("name", "too.short", "Too Short");
		exposeBindingResult(errors);
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		assertNotNull(getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE));
		String bodyContent = "Foo";
		this.tag.setBodyContent(new MockBodyContent(bodyContent, getWriter()));
		this.tag.doEndTag();
		this.tag.doFinally();
		assertEquals(bodyContent, getOutput());
		assertNull(getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE));
	}

	@Test
	public void asBodyTagWithExistingMessagesAttribute() throws Exception {
		String existingAttribute = "something";
		getPageContext().setAttribute(ErrorsTag.MESSAGES_ATTRIBUTE, existingAttribute);
		Errors errors = new BeanPropertyBindingResult(new TestBean(), "COMMAND_NAME");
		errors.rejectValue("name", "some.code", "Default Message");
		errors.rejectValue("name", "too.short", "Too Short");
		exposeBindingResult(errors);
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		assertNotNull(getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE));
		assertTrue(getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE) instanceof List);
		String bodyContent = "Foo";
		this.tag.setBodyContent(new MockBodyContent(bodyContent, getWriter()));
		this.tag.doEndTag();
		this.tag.doFinally();
		assertEquals(bodyContent, getOutput());
		assertEquals(existingAttribute, getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE));
	}

	/**
	 * https://jira.spring.io/browse/SPR-2788
	 */
	@Test
	public void asBodyTagWithErrorsAndExistingMessagesAttributeInNonPageScopeAreNotClobbered() throws Exception {
		String existingAttribute = "something";
		getPageContext().setAttribute(ErrorsTag.MESSAGES_ATTRIBUTE, existingAttribute, PageContext.APPLICATION_SCOPE);
		Errors errors = new BeanPropertyBindingResult(new TestBean(), "COMMAND_NAME");
		errors.rejectValue("name", "some.code", "Default Message");
		errors.rejectValue("name", "too.short", "Too Short");
		exposeBindingResult(errors);
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		assertNotNull(getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE));
		assertTrue(getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE) instanceof List);
		String bodyContent = "Foo";
		this.tag.setBodyContent(new MockBodyContent(bodyContent, getWriter()));
		this.tag.doEndTag();
		this.tag.doFinally();
		assertEquals(bodyContent, getOutput());
		assertEquals(existingAttribute,
				getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE, PageContext.APPLICATION_SCOPE));
	}

	/**
	 * https://jira.spring.io/browse/SPR-2788
	 */
	@Test
	public void asBodyTagWithNoErrorsAndExistingMessagesAttributeInApplicationScopeAreNotClobbered() throws Exception {
		assertWhenNoErrorsExistingMessagesInScopeAreNotClobbered(PageContext.APPLICATION_SCOPE);
	}

	/**
	 * https://jira.spring.io/browse/SPR-2788
	 */
	@Test
	public void asBodyTagWithNoErrorsAndExistingMessagesAttributeInSessionScopeAreNotClobbered() throws Exception {
		assertWhenNoErrorsExistingMessagesInScopeAreNotClobbered(PageContext.SESSION_SCOPE);
	}

	/**
	 * https://jira.spring.io/browse/SPR-2788
	 */
	@Test
	public void asBodyTagWithNoErrorsAndExistingMessagesAttributeInPageScopeAreNotClobbered() throws Exception {
		assertWhenNoErrorsExistingMessagesInScopeAreNotClobbered(PageContext.PAGE_SCOPE);
	}

	/**
	 * https://jira.spring.io/browse/SPR-2788
	 */
	@Test
	public void asBodyTagWithNoErrorsAndExistingMessagesAttributeInRequestScopeAreNotClobbered() throws Exception {
		assertWhenNoErrorsExistingMessagesInScopeAreNotClobbered(PageContext.REQUEST_SCOPE);
	}

	/**
	 * https://jira.spring.io/browse/SPR-4005
	 */
	@Test
	public void omittedPathMatchesObjectErrorsOnly() throws Exception {
		this.tag.setPath(null);
		Errors errors = new BeanPropertyBindingResult(new TestBean(), "COMMAND_NAME");
		errors.reject("some.code", "object error");
		errors.rejectValue("name", "some.code", "field error");
		exposeBindingResult(errors);
		this.tag.doStartTag();
		assertNotNull(getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE));
		this.tag.doEndTag();
		String output = getOutput();
		assertTrue(output.contains("id=\"testBean.errors\""));
		assertTrue(output.contains("object error"));
		assertFalse(output.contains("field error"));
	}

	@Test
	public void specificPathMatchesSpecificFieldOnly() throws Exception {
		this.tag.setPath("name");
		Errors errors = new BeanPropertyBindingResult(new TestBean(), "COMMAND_NAME");
		errors.reject("some.code", "object error");
		errors.rejectValue("name", "some.code", "field error");
		exposeBindingResult(errors);
		this.tag.doStartTag();
		assertNotNull(getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE));
		this.tag.doEndTag();
		String output = getOutput();
		assertTrue(output.contains("id=\"name.errors\""));
		assertFalse(output.contains("object error"));
		assertTrue(output.contains("field error"));
	}

	@Test
	public void starMatchesAllErrors() throws Exception {
		this.tag.setPath("*");
		Errors errors = new BeanPropertyBindingResult(new TestBean(), "COMMAND_NAME");
		errors.reject("some.code", "object error");
		errors.rejectValue("name", "some.code", "field error");
		exposeBindingResult(errors);
		this.tag.doStartTag();
		assertNotNull(getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE));
		this.tag.doEndTag();
		String output = getOutput();
		assertTrue(output.contains("id=\"testBean.errors\""));
		assertTrue(output.contains("object error"));
		assertTrue(output.contains("field error"));
	}

	@Override
	protected void exposeBindingResult(Errors errors) {
		// wrap errors in a Model
		Map model = new HashMap();
		model.put(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, errors);

		// replace the request context with one containing the errors
		MockPageContext pageContext = getPageContext();
		RequestContext context = new RequestContext((HttpServletRequest) pageContext.getRequest(), model);
		pageContext.setAttribute(RequestContextAwareTag.REQUEST_CONTEXT_PAGE_ATTRIBUTE, context);
	}

	private void assertElementTagOpened(String output) {
		assertTrue(output.startsWith("<" + this.tag.getElement() + " "));
	}

	private void assertElementTagClosed(String output) {
		assertTrue(output.endsWith("</" + this.tag.getElement() + ">"));
	}

	private void assertWhenNoErrorsExistingMessagesInScopeAreNotClobbered(int scope) throws JspException {
		String existingAttribute = "something";
		getPageContext().setAttribute(ErrorsTag.MESSAGES_ATTRIBUTE, existingAttribute, scope);

		Errors errors = new BeanPropertyBindingResult(new TestBean(), "COMMAND_NAME");
		exposeBindingResult(errors);
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertEquals(0, output.length());

		assertEquals(existingAttribute, getPageContext().getAttribute(ErrorsTag.MESSAGES_ATTRIBUTE, scope));
	}

}
