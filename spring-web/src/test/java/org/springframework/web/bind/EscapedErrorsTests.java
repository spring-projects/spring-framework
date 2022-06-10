/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.bind;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 02.05.2003
 */
public class EscapedErrorsTests {

	@Test
	public void testEscapedErrors() {
		TestBean tb = new TestBean();
		tb.setName("empty &");

		Errors errors = new EscapedErrors(new BindException(tb, "tb"));
		errors.rejectValue("name", "NAME_EMPTY &", null, "message: &");
		errors.rejectValue("age", "AGE_NOT_SET <tag>", null, "message: <tag>");
		errors.rejectValue("age", "AGE_NOT_32 <tag>", null, "message: <tag>");
		errors.reject("GENERAL_ERROR \" '", null, "message: \" '");

		assertThat(errors.hasErrors()).as("Correct errors flag").isTrue();
		assertThat(errors.getErrorCount() == 4).as("Correct number of errors").isTrue();
		assertThat("tb".equals(errors.getObjectName())).as("Correct object name").isTrue();

		assertThat(errors.hasGlobalErrors()).as("Correct global errors flag").isTrue();
		assertThat(errors.getGlobalErrorCount() == 1).as("Correct number of global errors").isTrue();
		ObjectError globalError = errors.getGlobalError();
		String defaultMessage = globalError.getDefaultMessage();
		assertThat("message: &quot; &#39;".equals(defaultMessage)).as("Global error message escaped").isTrue();
		assertThat("GENERAL_ERROR \" '".equals(globalError.getCode())).as("Global error code not escaped").isTrue();
		ObjectError globalErrorInList = errors.getGlobalErrors().get(0);
		assertThat(defaultMessage.equals(globalErrorInList.getDefaultMessage())).as("Same global error in list").isTrue();
		ObjectError globalErrorInAllList = errors.getAllErrors().get(3);
		assertThat(defaultMessage.equals(globalErrorInAllList.getDefaultMessage())).as("Same global error in list").isTrue();

		assertThat(errors.hasFieldErrors()).as("Correct field errors flag").isTrue();
		assertThat(errors.getFieldErrorCount() == 3).as("Correct number of field errors").isTrue();
		assertThat(errors.getFieldErrors().size() == 3).as("Correct number of field errors in list").isTrue();
		FieldError fieldError = errors.getFieldError();
		assertThat("NAME_EMPTY &".equals(fieldError.getCode())).as("Field error code not escaped").isTrue();
		assertThat("empty &amp;".equals(errors.getFieldValue("name"))).as("Field value escaped").isTrue();
		FieldError fieldErrorInList = errors.getFieldErrors().get(0);
		assertThat(fieldError.getDefaultMessage().equals(fieldErrorInList.getDefaultMessage())).as("Same field error in list").isTrue();

		assertThat(errors.hasFieldErrors("name")).as("Correct name errors flag").isTrue();
		assertThat(errors.getFieldErrorCount("name") == 1).as("Correct number of name errors").isTrue();
		assertThat(errors.getFieldErrors("name").size() == 1).as("Correct number of name errors in list").isTrue();
		FieldError nameError = errors.getFieldError("name");
		assertThat("message: &amp;".equals(nameError.getDefaultMessage())).as("Name error message escaped").isTrue();
		assertThat("NAME_EMPTY &".equals(nameError.getCode())).as("Name error code not escaped").isTrue();
		assertThat("empty &amp;".equals(errors.getFieldValue("name"))).as("Name value escaped").isTrue();
		FieldError nameErrorInList = errors.getFieldErrors("name").get(0);
		assertThat(nameError.getDefaultMessage().equals(nameErrorInList.getDefaultMessage())).as("Same name error in list").isTrue();

		assertThat(errors.hasFieldErrors("age")).as("Correct age errors flag").isTrue();
		assertThat(errors.getFieldErrorCount("age") == 2).as("Correct number of age errors").isTrue();
		assertThat(errors.getFieldErrors("age").size() == 2).as("Correct number of age errors in list").isTrue();
		FieldError ageError = errors.getFieldError("age");
		assertThat("message: &lt;tag&gt;".equals(ageError.getDefaultMessage())).as("Age error message escaped").isTrue();
		assertThat("AGE_NOT_SET <tag>".equals(ageError.getCode())).as("Age error code not escaped").isTrue();
		assertThat((Integer.valueOf(0)).equals(errors.getFieldValue("age"))).as("Age value not escaped").isTrue();
		FieldError ageErrorInList = errors.getFieldErrors("age").get(0);
		assertThat(ageError.getDefaultMessage().equals(ageErrorInList.getDefaultMessage())).as("Same name error in list").isTrue();
		FieldError ageError2 = errors.getFieldErrors("age").get(1);
		assertThat("message: &lt;tag&gt;".equals(ageError2.getDefaultMessage())).as("Age error 2 message escaped").isTrue();
		assertThat("AGE_NOT_32 <tag>".equals(ageError2.getCode())).as("Age error 2 code not escaped").isTrue();
	}

}
