/*
 * Copyright 2002-2023 the original author or authors.
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
		assertThat(errors.getErrorCount()).as("Correct number of errors").isEqualTo(4);
		assertThat(errors.getObjectName()).as("Correct object name").isEqualTo("tb");

		assertThat(errors.hasGlobalErrors()).as("Correct global errors flag").isTrue();
		assertThat(errors.getGlobalErrorCount()).as("Correct number of global errors").isOne();
		ObjectError globalError = errors.getGlobalError();
		String defaultMessage = globalError.getDefaultMessage();
		assertThat(defaultMessage).as("Global error message escaped").isEqualTo("message: &quot; &#39;");
		assertThat(globalError.getCode()).as("Global error code not escaped").isEqualTo("GENERAL_ERROR \" '");
		ObjectError globalErrorInList = errors.getGlobalErrors().get(0);
		assertThat(defaultMessage).as("Same global error in list").isEqualTo(globalErrorInList.getDefaultMessage());
		ObjectError globalErrorInAllList = errors.getAllErrors().get(3);
		assertThat(defaultMessage).as("Same global error in list").isEqualTo(globalErrorInAllList.getDefaultMessage());

		assertThat(errors.hasFieldErrors()).as("Correct field errors flag").isTrue();
		assertThat(errors.getFieldErrorCount()).as("Correct number of field errors").isEqualTo(3);
		assertThat(errors.getFieldErrors()).as("Correct number of field errors in list").hasSize(3);
		FieldError fieldError = errors.getFieldError();
		assertThat(fieldError.getCode()).as("Field error code not escaped").isEqualTo("NAME_EMPTY &");
		assertThat(errors.getFieldValue("name")).as("Field value escaped").isEqualTo("empty &amp;");
		FieldError fieldErrorInList = errors.getFieldErrors().get(0);
		assertThat(fieldError.getDefaultMessage()).as("Same field error in list")
				.isEqualTo(fieldErrorInList.getDefaultMessage());

		assertThat(errors.hasFieldErrors("name")).as("Correct name errors flag").isTrue();
		assertThat(errors.getFieldErrorCount("name")).as("Correct number of name errors").isOne();
		assertThat(errors.getFieldErrors("name")).as("Correct number of name errors in list").hasSize(1);
		FieldError nameError = errors.getFieldError("name");
		assertThat(nameError.getDefaultMessage()).as("Name error message escaped").isEqualTo("message: &amp;");
		assertThat(nameError.getCode()).as("Name error code not escaped").isEqualTo("NAME_EMPTY &");
		assertThat(errors.getFieldValue("name")).as("Name value escaped").isEqualTo("empty &amp;");
		FieldError nameErrorInList = errors.getFieldErrors("name").get(0);
		assertThat(nameError.getDefaultMessage()).as("Same name error in list")
				.isEqualTo(nameErrorInList.getDefaultMessage());

		assertThat(errors.hasFieldErrors("age")).as("Correct age errors flag").isTrue();
		assertThat(errors.getFieldErrorCount("age")).as("Correct number of age errors").isEqualTo(2);
		assertThat(errors.getFieldErrors("age")).as("Correct number of age errors in list").hasSize(2);
		FieldError ageError = errors.getFieldError("age");
		assertThat(ageError.getDefaultMessage()).as("Age error message escaped").isEqualTo("message: &lt;tag&gt;");
		assertThat(ageError.getCode()).as("Age error code not escaped").isEqualTo("AGE_NOT_SET <tag>");
		assertThat((Integer.valueOf(0))).as("Age value not escaped").isEqualTo(errors.getFieldValue("age"));
		FieldError ageErrorInList = errors.getFieldErrors("age").get(0);
		assertThat(ageError.getDefaultMessage()).as("Same name error in list")
				.isEqualTo(ageErrorInList.getDefaultMessage());
		FieldError ageError2 = errors.getFieldErrors("age").get(1);
		assertThat(ageError2.getDefaultMessage()).as("Age error 2 message escaped").isEqualTo("message: &lt;tag&gt;");
		assertThat(ageError2.getCode()).as("Age error 2 code not escaped").isEqualTo("AGE_NOT_32 <tag>");
	}

}
