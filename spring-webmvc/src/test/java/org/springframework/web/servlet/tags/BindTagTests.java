/*
 * Copyright 2002-2021 the original author or authors.
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

import java.beans.PropertyEditorSupport;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.testfixture.beans.IndexedTestBean;
import org.springframework.beans.testfixture.beans.NestedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.tags.form.FormTag;
import org.springframework.web.servlet.tags.form.TagWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Mark Fisher
 */
class BindTagTests extends AbstractTagTests {

	@Test
	void bindTagWithoutErrors() throws JspException {
		PageContext pc = createPageContext();
		Errors errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat(status.getExpression() == null).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		boolean condition = !status.isError();
		assertThat(condition).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 0).as("Correct errorCodes").isTrue();
		assertThat(status.getErrorMessages().length == 0).as("Correct errorMessages").isTrue();
		assertThat("".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
		assertThat("".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("".equals(status.getErrorMessagesAsString(","))).as("Correct errorMessagesAsString").isTrue();
	}

	@Test
	void bindTagWithGlobalErrors() throws JspException {
		PageContext pc = createPageContext();
		Errors errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		errors.reject("code1", "message1");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat(status.getExpression() == null).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 1).as("Correct errorCodes").isTrue();
		assertThat(status.getErrorMessages().length == 1).as("Correct errorMessages").isTrue();
		assertThat("code1".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
		assertThat("message1".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message1".equals(status.getErrorMessagesAsString(","))).as("Correct errorMessagesAsString").isTrue();

		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.*");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("*".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 1).as("Correct errorCodes").isTrue();
		assertThat(status.getErrorMessages().length == 1).as("Correct errorMessages").isTrue();
		assertThat("code1".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
		assertThat("message1".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message1".equals(status.getErrorMessagesAsString(","))).as("Correct errorMessagesAsString").isTrue();
	}

	@Test
	void bindTagWithGlobalErrorsAndNoDefaultMessage() throws JspException {
		PageContext pc = createPageContext();
		Errors errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		errors.reject("code1");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat(status.getExpression() == null).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 1).as("Correct errorCodes").isTrue();
		assertThat("code1".equals(status.getErrorCode())).as("Correct errorCode").isTrue();

		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.*");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("*".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 1).as("Correct errorCodes").isTrue();
		assertThat("code1".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
	}

	@Test
	void bindTagWithGlobalErrorsAndDefaultMessageOnly() throws JspException {
		PageContext pc = createPageContext();
		Errors errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		errors.reject(null, "message1");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat(status.getExpression() == null).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorMessages().length == 1).as("Correct errorMessages").isTrue();
		assertThat("message1".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message1".equals(status.getErrorMessagesAsString(","))).as("Correct errorMessagesAsString").isTrue();

		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.*");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("*".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorMessages().length == 1).as("Correct errorMessages").isTrue();
		assertThat("message1".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message1".equals(status.getErrorMessagesAsString(","))).as("Correct errorMessagesAsString").isTrue();
	}

	@Test
	void bindStatusGetErrorMessagesAsString() throws JspException {
		// one error (should not include delimiter)
		PageContext pc = createPageContext();
		Errors errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		errors.reject("code1", null, "message1");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		tag.doStartTag();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status.getErrorMessagesAsString(",")).as("Error messages String should be 'message1'").isEqualTo("message1");

		// two errors
		pc = createPageContext();
		errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		errors.reject("code1", null, "message1");
		errors.reject("code1", null, "message2");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);
		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		tag.doStartTag();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status.getErrorMessagesAsString(",")).as("Error messages String should be 'message1,message2'").isEqualTo("message1,message2");

		// no errors
		pc = createPageContext();
		errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);
		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		tag.doStartTag();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status.getErrorMessagesAsString(",")).as("Error messages String should be ''").isEqualTo("");
	}

	@Test
	void bindTagWithFieldErrors() throws JspException {
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		tb.setName("name1");
		Errors errors = new ServletRequestDataBinder(tb, "tb").getBindingResult();
		errors.rejectValue("name", "code1", "message & 1");
		errors.rejectValue("name", "code2", "message2");
		errors.rejectValue("age", "code2", "message2");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.name");
		tag.setHtmlEscape(true);
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("name".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat("name1".equals(status.getValue())).as("Correct value").isTrue();
		assertThat("name1".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 2).as("Correct errorCodes").isTrue();
		assertThat(status.getErrorMessages().length == 2).as("Correct errorMessages").isTrue();
		assertThat("code1".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
		assertThat("code1".equals(status.getErrorCodes()[0])).as("Correct errorCode").isTrue();
		assertThat("code2".equals(status.getErrorCodes()[1])).as("Correct errorCode").isTrue();
		assertThat("message &amp; 1".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message &amp; 1".equals(status.getErrorMessages()[0])).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessages()[1])).as("Correct errorMessage").isTrue();
		assertThat("message &amp; 1 - message2".equals(status.getErrorMessagesAsString(" - "))).as("Correct errorMessagesAsString").isTrue();

		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.age");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("age".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat(Integer.valueOf(0).equals(status.getValue())).as("Correct value").isTrue();
		assertThat("0".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 1).as("Correct errorCodes").isTrue();
		assertThat(status.getErrorMessages().length == 1).as("Correct errorMessages").isTrue();
		assertThat("code2".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
		assertThat("message2".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessagesAsString(" - "))).as("Correct errorMessagesAsString").isTrue();

		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.*");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("*".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 3).as("Correct errorCodes").isTrue();
		assertThat(status.getErrorMessages().length == 3).as("Correct errorMessages").isTrue();
		assertThat("code1".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
		assertThat("code1".equals(status.getErrorCodes()[0])).as("Correct errorCode").isTrue();
		assertThat("code2".equals(status.getErrorCodes()[1])).as("Correct errorCode").isTrue();
		assertThat("code2".equals(status.getErrorCodes()[2])).as("Correct errorCode").isTrue();
		assertThat("message & 1".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message & 1".equals(status.getErrorMessages()[0])).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessages()[1])).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessages()[2])).as("Correct errorMessage").isTrue();
	}

	@Test
	void bindTagWithFieldErrorsAndNoDefaultMessage() throws JspException {
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		tb.setName("name1");
		Errors errors = new ServletRequestDataBinder(tb, "tb").getBindingResult();
		errors.rejectValue("name", "code1");
		errors.rejectValue("name", "code2");
		errors.rejectValue("age", "code2");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.name");
		tag.setHtmlEscape(true);
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("name".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat("name1".equals(status.getValue())).as("Correct value").isTrue();
		assertThat("name1".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 2).as("Correct errorCodes").isTrue();
		assertThat("code1".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
		assertThat("code1".equals(status.getErrorCodes()[0])).as("Correct errorCode").isTrue();
		assertThat("code2".equals(status.getErrorCodes()[1])).as("Correct errorCode").isTrue();

		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.age");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("age".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat(Integer.valueOf(0).equals(status.getValue())).as("Correct value").isTrue();
		assertThat("0".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 1).as("Correct errorCodes").isTrue();
		assertThat("code2".equals(status.getErrorCode())).as("Correct errorCode").isTrue();

		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.*");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("*".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 3).as("Correct errorCodes").isTrue();
		assertThat("code1".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
		assertThat("code1".equals(status.getErrorCodes()[0])).as("Correct errorCode").isTrue();
		assertThat("code2".equals(status.getErrorCodes()[1])).as("Correct errorCode").isTrue();
		assertThat("code2".equals(status.getErrorCodes()[2])).as("Correct errorCode").isTrue();
	}

	@Test
	void bindTagWithFieldErrorsAndDefaultMessageOnly() throws JspException {
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		tb.setName("name1");
		Errors errors = new ServletRequestDataBinder(tb, "tb").getBindingResult();
		errors.rejectValue("name", null, "message & 1");
		errors.rejectValue("name", null, "message2");
		errors.rejectValue("age", null, "message2");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.name");
		tag.setHtmlEscape(true);
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("name".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat("name1".equals(status.getValue())).as("Correct value").isTrue();
		assertThat("name1".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorMessages().length == 2).as("Correct errorMessages").isTrue();
		assertThat("message &amp; 1".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message &amp; 1".equals(status.getErrorMessages()[0])).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessages()[1])).as("Correct errorMessage").isTrue();
		assertThat("message &amp; 1 - message2".equals(status.getErrorMessagesAsString(" - "))).as("Correct errorMessagesAsString").isTrue();

		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.age");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("age".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat(Integer.valueOf(0).equals(status.getValue())).as("Correct value").isTrue();
		assertThat("0".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorMessages().length == 1).as("Correct errorMessages").isTrue();
		assertThat("message2".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessagesAsString(" - "))).as("Correct errorMessagesAsString").isTrue();

		tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.*");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("*".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat(status.getValue() == null).as("Correct value").isTrue();
		assertThat("".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorMessages().length == 3).as("Correct errorMessages").isTrue();
		assertThat("message & 1".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message & 1".equals(status.getErrorMessages()[0])).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessages()[1])).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessages()[2])).as("Correct errorMessage").isTrue();
	}

	@Test
	void bindTagWithNestedFieldErrors() throws JspException {
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		tb.setName("name1");
		TestBean spouse = new TestBean();
		spouse.setName("name2");
		tb.setSpouse(spouse);
		Errors errors = new ServletRequestDataBinder(tb, "tb").getBindingResult();
		errors.rejectValue("spouse.name", "code1", "message1");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.spouse.name");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("spouse.name".equals(status.getExpression())).as("Correct expression").isTrue();
		assertThat("name2".equals(status.getValue())).as("Correct value").isTrue();
		assertThat("name2".equals(status.getDisplayValue())).as("Correct displayValue").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 1).as("Correct errorCodes").isTrue();
		assertThat(status.getErrorMessages().length == 1).as("Correct errorMessages").isTrue();
		assertThat("code1".equals(status.getErrorCode())).as("Correct errorCode").isTrue();
		assertThat("message1".equals(status.getErrorMessage())).as("Correct errorMessage").isTrue();
		assertThat("message1".equals(status.getErrorMessagesAsString(" - "))).as("Correct errorMessagesAsString").isTrue();
	}

	@Test
	void propertyExposing() throws JspException {
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		tb.setName("name1");
		Errors errors = new BindException(tb, "tb");
		errors.rejectValue("name", "code1", null, "message & 1");
		errors.rejectValue("name", "code2", null, "message2");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		// test global property (should be null)
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		assertThat(tag.getProperty()).isNull();

		// test property set (tb.name)
		tag.release();
		tag.setPageContext(pc);
		tag.setPath("tb.name");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		assertThat(tag.getProperty()).isEqualTo("name");
	}

	@Test
	void bindTagWithIndexedProperties() throws JspException {
		PageContext pc = createPageContext();
		IndexedTestBean tb = new IndexedTestBean();
		Errors errors = new ServletRequestDataBinder(tb, "tb").getBindingResult();
		errors.rejectValue("array[0]", "code1", "message1");
		errors.rejectValue("array[0]", "code2", "message2");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.array[0]");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("array[0]".equals(status.getExpression())).as("Correct expression").isTrue();
		boolean condition = status.getValue() instanceof TestBean;
		assertThat(condition).as("Value is TestBean").isTrue();
		assertThat("name0".equals(((TestBean) status.getValue()).getName())).as("Correct value").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 2).as("Correct errorCodes").isTrue();
		assertThat(status.getErrorMessages().length == 2).as("Correct errorMessages").isTrue();
		assertThat("code1".equals(status.getErrorCodes()[0])).as("Correct errorCode").isTrue();
		assertThat("code2".equals(status.getErrorCodes()[1])).as("Correct errorCode").isTrue();
		assertThat("message1".equals(status.getErrorMessages()[0])).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessages()[1])).as("Correct errorMessage").isTrue();
	}

	@Test
	void bindTagWithMappedProperties() throws JspException {
		PageContext pc = createPageContext();
		IndexedTestBean tb = new IndexedTestBean();
		Errors errors = new ServletRequestDataBinder(tb, "tb").getBindingResult();
		errors.rejectValue("map[key1]", "code1", "message1");
		errors.rejectValue("map[key1]", "code2", "message2");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.map[key1]");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("map[key1]".equals(status.getExpression())).as("Correct expression").isTrue();
		boolean condition = status.getValue() instanceof TestBean;
		assertThat(condition).as("Value is TestBean").isTrue();
		assertThat("name4".equals(((TestBean) status.getValue()).getName())).as("Correct value").isTrue();
		assertThat(status.isError()).as("Correct isError").isTrue();
		assertThat(status.getErrorCodes().length == 2).as("Correct errorCodes").isTrue();
		assertThat(status.getErrorMessages().length == 2).as("Correct errorMessages").isTrue();
		assertThat("code1".equals(status.getErrorCodes()[0])).as("Correct errorCode").isTrue();
		assertThat("code2".equals(status.getErrorCodes()[1])).as("Correct errorCode").isTrue();
		assertThat("message1".equals(status.getErrorMessages()[0])).as("Correct errorMessage").isTrue();
		assertThat("message2".equals(status.getErrorMessages()[1])).as("Correct errorMessage").isTrue();
	}

	@Test
	void bindTagWithIndexedPropertiesAndCustomEditor() throws JspException {
		PageContext pc = createPageContext();
		IndexedTestBean tb = new IndexedTestBean();
		DataBinder binder = new ServletRequestDataBinder(tb, "tb");
		binder.registerCustomEditor(TestBean.class, null, new PropertyEditorSupport() {
			@Override
			public String getAsText() {
				return "something";
			}
		});
		Errors errors = binder.getBindingResult();
		errors.rejectValue("array[0]", "code1", "message1");
		errors.rejectValue("array[0]", "code2", "message2");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.array[0]");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat("array[0]".equals(status.getExpression())).as("Correct expression").isTrue();
		// because of the custom editor getValue() should return a String
		boolean condition = status.getValue() instanceof String;
		assertThat(condition).as("Value is TestBean").isTrue();
		assertThat("something".equals(status.getValue())).as("Correct value").isTrue();
	}

	@Test
	void bindTagWithToStringAndHtmlEscaping() throws JspException {
		PageContext pc = createPageContext();
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.doctor");
		tag.setHtmlEscape(true);
		TestBean tb = new TestBean("somebody", 99);
		NestedTestBean ntb = new NestedTestBean("juergen&eva");
		tb.setDoctor(ntb);
		pc.getRequest().setAttribute("tb", tb);
		tag.doStartTag();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status.getExpression()).isEqualTo("doctor");
		boolean condition = status.getValue() instanceof NestedTestBean;
		assertThat(condition).isTrue();
		assertThat(status.getDisplayValue().contains("juergen&amp;eva")).isTrue();
	}

	@Test
	void bindTagWithSetValueAndHtmlEscaping() throws JspException {
		PageContext pc = createPageContext();
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.someSet");
		tag.setHtmlEscape(true);
		pc.getRequest().setAttribute("tb", new TestBean("juergen&eva", 99));
		tag.doStartTag();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status.getExpression()).isEqualTo("someSet");
		boolean condition = status.getValue() instanceof Set;
		assertThat(condition).isTrue();
	}

	@Test
	void bindTagWithFieldButWithoutErrorsInstance() throws JspException {
		PageContext pc = createPageContext();
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.name");
		pc.getRequest().setAttribute("tb", new TestBean("juergen&eva", 99));
		tag.doStartTag();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status.getExpression()).isEqualTo("name");
		assertThat(status.getValue()).isEqualTo("juergen&eva");
	}

	@Test
	void bindTagWithFieldButWithoutErrorsInstanceAndHtmlEscaping() throws JspException {
		PageContext pc = createPageContext();
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb.name");
		tag.setHtmlEscape(true);
		pc.getRequest().setAttribute("tb", new TestBean("juergen&eva", 99));
		tag.doStartTag();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status.getExpression()).isEqualTo("name");
		assertThat(status.getValue()).isEqualTo("juergen&amp;eva");
	}

	@Test
	void bindTagWithBeanButWithoutErrorsInstance() throws JspException {
		PageContext pc = createPageContext();
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		pc.getRequest().setAttribute("tb", new TestBean("juergen", 99));
		tag.doStartTag();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status.getExpression()).isNull();
		assertThat(status.getValue()).isNull();
	}

	@Test
	void bindTagWithoutBean() throws JspException {
		PageContext pc = createPageContext();
		BindTag tag = new BindTag();
		tag.setPageContext(pc);
		tag.setPath("tb");
		assertThatExceptionOfType(JspException.class).isThrownBy(
				tag::doStartTag);
	}


	@Test
	void bindErrorsTagWithoutErrors() throws JspException {
		PageContext pc = createPageContext();
		Errors errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);
		BindErrorsTag tag = new BindErrorsTag();
		tag.setPageContext(pc);
		tag.setName("tb");
		assertThat(tag.doStartTag() == Tag.SKIP_BODY).as("Correct doStartTag return value").isTrue();
		assertThat(pc.getAttribute(BindErrorsTag.ERRORS_VARIABLE_NAME) == null).as("Doesn't have errors variable").isTrue();
	}

	@Test
	void bindErrorsTagWithErrors() throws JspException {
		PageContext pc = createPageContext();
		Errors errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		errors.reject("test", null, "test");
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);
		BindErrorsTag tag = new BindErrorsTag();
		tag.setPageContext(pc);
		tag.setName("tb");
		assertThat(tag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		assertThat(pc.getAttribute(BindErrorsTag.ERRORS_VARIABLE_NAME, PageContext.REQUEST_SCOPE) == errors).as("Has errors variable").isTrue();
	}

	@Test
	void bindErrorsTagWithoutBean() throws JspException {
		PageContext pc = createPageContext();
		BindErrorsTag tag = new BindErrorsTag();
		tag.setPageContext(pc);
		tag.setName("tb");
		assertThat(tag.doStartTag() == Tag.SKIP_BODY).as("Correct doStartTag return value").isTrue();
	}


	@Test
	void nestedPathDoEndTag() throws JspException {
		PageContext pc = createPageContext();
		NestedPathTag tag = new NestedPathTag();
		tag.setPath("foo");
		tag.setPageContext(pc);
		tag.doStartTag();
		int returnValue = tag.doEndTag();
		assertThat(returnValue).isEqualTo(Tag.EVAL_PAGE);
		assertThat(pc.getAttribute(NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE)).isNull();
	}

	@Test
	void nestedPathDoEndTagWithNesting() throws JspException {
		PageContext pc = createPageContext();
		NestedPathTag tag = new NestedPathTag();
		tag.setPath("foo");
		tag.setPageContext(pc);
		tag.doStartTag();

		NestedPathTag anotherTag = new NestedPathTag();
		anotherTag.setPageContext(pc);
		anotherTag.setPath("bar");
		anotherTag.doStartTag();
		anotherTag.doEndTag();

		assertThat(pc.getAttribute(NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE)).isEqualTo("foo.");

		tag.doEndTag();
		assertThat(pc.getAttribute(NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE)).isNull();
	}

	@Test
	void nestedPathDoStartTagInternal() throws JspException {
		PageContext pc = createPageContext();
		NestedPathTag tag = new NestedPathTag();
		tag.setPath("foo");
		tag.setPageContext(pc);
		int returnValue = tag.doStartTag();

		assertThat(returnValue).isEqualTo(Tag.EVAL_BODY_INCLUDE);
		assertThat(pc.getAttribute(NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE)).isEqualTo("foo.");
	}

	@Test
	void nestedPathDoStartTagInternalWithNesting() throws JspException {
		PageContext pc = createPageContext();
		NestedPathTag tag = new NestedPathTag();
		tag.setPath("foo");
		tag.setPageContext(pc);
		tag.doStartTag();
		assertThat(pc.getAttribute(NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE)).isEqualTo("foo.");

		NestedPathTag anotherTag = new NestedPathTag();
		anotherTag.setPageContext(pc);
		anotherTag.setPath("bar");
		anotherTag.doStartTag();

		assertThat(pc.getAttribute(NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE)).isEqualTo("foo.bar.");

		NestedPathTag yetAnotherTag = new NestedPathTag();
		yetAnotherTag.setPageContext(pc);
		yetAnotherTag.setPath("boo");
		yetAnotherTag.doStartTag();

		assertThat(pc.getAttribute(NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE)).isEqualTo("foo.bar.boo.");

		yetAnotherTag.doEndTag();

		NestedPathTag andAnotherTag = new NestedPathTag();
		andAnotherTag.setPageContext(pc);
		andAnotherTag.setPath("boo2");
		andAnotherTag.doStartTag();

		assertThat(pc.getAttribute(NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE)).isEqualTo("foo.bar.boo2.");
	}

	@Test
	void nestedPathWithBindTag() throws JspException {
		PageContext pc = createPageContext();
		Errors errors = new ServletRequestDataBinder(new TestBean(), "tb").getBindingResult();
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", errors);

		NestedPathTag nestedPathTag = new NestedPathTag();
		nestedPathTag.setPath("tb");
		nestedPathTag.setPageContext(pc);
		nestedPathTag.doStartTag();

		BindTag bindTag = new BindTag();
		bindTag.setPageContext(pc);
		bindTag.setPath("name");

		assertThat(bindTag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat(status.getPath()).isEqualTo("tb.name");
		assertThat(status.getDisplayValue()).as("Correct field value").isEqualTo("");

		BindTag bindTag2 = new BindTag();
		bindTag2.setPageContext(pc);
		bindTag2.setPath("age");

		assertThat(bindTag2.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status2 = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status2 != null).as("Has status variable").isTrue();
		assertThat(status2.getPath()).isEqualTo("tb.age");
		assertThat(status2.getDisplayValue()).as("Correct field value").isEqualTo("0");

		bindTag2.doEndTag();

		BindStatus status3 = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status3).as("Status matches previous status").isSameAs(status);
		assertThat(status.getPath()).isEqualTo("tb.name");
		assertThat(status.getDisplayValue()).as("Correct field value").isEqualTo("");

		bindTag.doEndTag();
		nestedPathTag.doEndTag();
	}

	@Test
	void nestedPathWithBindTagWithIgnoreNestedPath() throws JspException {
		PageContext pc = createPageContext();
		Errors errors = new ServletRequestDataBinder(new TestBean(), "tb2").getBindingResult();
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb2", errors);

		NestedPathTag tag = new NestedPathTag();
		tag.setPath("tb");
		tag.setPageContext(pc);
		tag.doStartTag();

		BindTag bindTag = new BindTag();
		bindTag.setPageContext(pc);
		bindTag.setIgnoreNestedPath(true);
		bindTag.setPath("tb2.name");

		assertThat(bindTag.doStartTag() == Tag.EVAL_BODY_INCLUDE).as("Correct doStartTag return value").isTrue();
		BindStatus status = (BindStatus) pc.getAttribute(BindTag.STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		assertThat(status != null).as("Has status variable").isTrue();
		assertThat(status.getPath()).isEqualTo("tb2.name");
	}

	@Test
	void transformTagCorrectBehavior() throws JspException {
		// first set up the pagecontext and the bean
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "tb");
		CustomDateEditor l = new CustomDateEditor(df, true);
		binder.registerCustomEditor(Date.class, l);
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", binder.getBindingResult());

		// execute the bind tag using the date property
		BindTag bind = new BindTag();
		bind.setPageContext(pc);
		bind.setPath("tb.date");
		bind.doStartTag();

		// transform stuff
		TransformTag transform = new TransformTag();
		transform.setPageContext(pc);
		transform.setParent(bind);
		transform.setValue(tb.getDate());
		transform.setVar("theDate");
		transform.doStartTag();

		assertThat(pc.getAttribute("theDate")).isNotNull();
		assertThat(df.format(tb.getDate())).isEqualTo(pc.getAttribute("theDate"));

		// try another time, this time using Strings
		bind = new BindTag();
		bind.setPageContext(pc);
		bind.setPath("tb.name");
		bind.doStartTag();

		transform = new TransformTag();
		transform.setPageContext(pc);
		transform.setValue("name");
		transform.setParent(bind);
		transform.setVar("theString");
		transform.doStartTag();

		assertThat(pc.getAttribute("theString")).isNotNull();
		assertThat(pc.getAttribute("theString")).isEqualTo("name");
	}

	@Test
	void transformTagWithHtmlEscape() throws JspException {
		// first set up the PageContext and the bean
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "tb");
		CustomDateEditor l = new CustomDateEditor(df, true);
		binder.registerCustomEditor(Date.class, l);
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", binder.getBindingResult());

		// try another time, this time using Strings
		BindTag bind = new BindTag();
		bind.setPageContext(pc);
		bind.setPath("tb.name");
		bind.doStartTag();

		TransformTag transform = new TransformTag();
		transform.setPageContext(pc);
		transform.setValue("na<me");
		transform.setParent(bind);
		transform.setVar("theString");
		transform.setHtmlEscape(true);
		transform.doStartTag();

		assertThat(pc.getAttribute("theString")).isNotNull();
		assertThat(pc.getAttribute("theString")).isEqualTo("na&lt;me");
	}

	@Test
	void transformTagOutsideBindTag() throws JspException {
		// first set up the pagecontext and the bean
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "tb");
		CustomDateEditor l = new CustomDateEditor(df, true);
		binder.registerCustomEditor(Date.class, l);
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", binder.getBindingResult());

		// now try to execute the tag outside a bindtag
		TransformTag transform = new TransformTag();
		transform.setPageContext(pc);
		transform.setVar("var");
		transform.setValue("bla");
		assertThatExceptionOfType(JspException.class).as("executed outside BindTag").isThrownBy(
				transform::doStartTag);

		// now try to execute the tag outside a bindtag, but inside a messageTag
		MessageTag message = new MessageTag();
		message.setPageContext(pc);
		transform = new TransformTag();
		transform.setPageContext(pc);
		transform.setVar("var");
		transform.setValue("bla");
		transform.setParent(message);
		assertThatExceptionOfType(JspException.class).as("executed outside BindTag and inside messagetag").isThrownBy(
				transform::doStartTag);
	}

	@Test
	void transformTagNonExistingValue() throws JspException {
		// first set up the pagecontext and the bean
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "tb");
		CustomDateEditor l = new CustomDateEditor(df, true);
		binder.registerCustomEditor(Date.class, l);
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", binder.getBindingResult());

		// try with non-existing value
		BindTag bind = new BindTag();
		bind.setPageContext(pc);
		bind.setPath("tb.name");
		bind.doStartTag();

		TransformTag transform = new TransformTag();
		transform.setPageContext(pc);
		transform.setValue(null);
		transform.setParent(bind);
		transform.setVar("theString2");
		transform.doStartTag();

		assertThat(pc.getAttribute("theString2")).isNull();
	}

	@Test
	void transformTagWithSettingOfScope() throws JspException {
		// first set up the pagecontext and the bean
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "tb");
		CustomDateEditor l = new CustomDateEditor(df, true);
		binder.registerCustomEditor(Date.class, l);
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", binder.getBindingResult());

		// execute the bind tag using the date property
		BindTag bind = new BindTag();
		bind.setPageContext(pc);
		bind.setPath("tb.date");
		bind.doStartTag();

		// transform stuff
		TransformTag transform = new TransformTag();
		transform.setPageContext(pc);
		transform.setParent(bind);
		transform.setValue(tb.getDate());
		transform.setVar("theDate");
		transform.setScope("page");
		transform.doStartTag();

		transform.release();

		assertThat(pc.getAttribute("theDate")).isNotNull();
		assertThat(pc.getAttribute("theDate")).isEqualTo(df.format(tb.getDate()));

		// try another time, this time using Strings
		bind = new BindTag();
		bind.setPageContext(pc);
		bind.setPath("tb.name");
		bind.doStartTag();

		transform = new TransformTag();
		transform.setPageContext(pc);
		transform.setValue("name");
		transform.setParent(bind);
		transform.setVar("theString");
		transform.setScope("page");
		transform.doStartTag();

		transform.release();

		assertThat(pc.getAttribute("theString")).isNotNull();
		assertThat(pc.getAttribute("theString")).isEqualTo("name");
	}

	/**
	 * SPR-4022
	 */
	@SuppressWarnings("serial")
	@Test
	void nestingInFormTag() throws JspException {
		PageContext pc = createPageContext();
		TestBean tb = new TestBean();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		ServletRequestDataBinder binder = new ServletRequestDataBinder(tb, "tb");
		CustomDateEditor l = new CustomDateEditor(df, true);
		binder.registerCustomEditor(Date.class, l);
		pc.getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "tb", binder.getBindingResult());

		FormTag formTag = new FormTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(new StringWriter());
			}
		};

		String action = "/form.html";
		String commandName = "tb";
		String name = "formName";
		String enctype = "my/enctype";
		String method = "POST";
		String onsubmit = "onsubmit";
		String onreset = "onreset";
		String cssClass = "myClass";
		String cssStyle = "myStyle";
		String acceptCharset = "iso-8859-1";

		formTag.setName(name);
		formTag.setCssClass(cssClass);
		formTag.setCssStyle(cssStyle);
		formTag.setAction(action);
		formTag.setModelAttribute(commandName);
		formTag.setEnctype(enctype);
		formTag.setMethod(method);
		formTag.setOnsubmit(onsubmit);
		formTag.setOnreset(onreset);
		formTag.setAcceptCharset(acceptCharset);

		formTag.setPageContext(pc);
		formTag.doStartTag();

		BindTag bindTag1 = new BindTag();
		bindTag1.setPageContext(pc);
		bindTag1.setPath("date");
		bindTag1.doStartTag();
		bindTag1.doEndTag();

		BindTag bindTag2 = new BindTag();
		bindTag2.setPageContext(pc);
		bindTag2.setPath("tb.date");
		bindTag2.doStartTag();
		bindTag2.doEndTag();

		BindTag bindTag3 = new BindTag();
		bindTag3.setPageContext(pc);
		bindTag3.setPath("tb");
		bindTag3.doStartTag();
		bindTag3.doEndTag();

		formTag.doEndTag();
	}

}
