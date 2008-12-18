/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class CancellableFormControllerTests extends TestCase {

	public void testFormViewRequest() throws Exception {
		String formView = "theFormView";

		TestController ctl = new TestController();
		ctl.setFormView(formView);
		ctl.setBindOnNewForm(true);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		String name = "Rob Harrop";
		int age = 23;

		request.setMethod("GET");
		request.addParameter("name", name);
		request.addParameter("age", "" + age);

		ModelAndView mv = ctl.handleRequest(request, response);

		assertEquals("Incorrect view name", formView, mv.getViewName());

		TestBean command = (TestBean) mv.getModel().get(ctl.getCommandName());

		testCommandIsBound(command, name, age);
	}

	public void testFormSubmissionRequestWithoutCancel() throws Exception {
		String successView = "successView";

		TestController ctl = new TestController();
		ctl.setSuccessView(successView);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		String name = "Rob Harrop";
		int age = 23;

		request.setMethod("POST");
		request.addParameter("name", name);
		request.addParameter("age", "" + age);

		ModelAndView mv = ctl.handleRequest(request, response);

		assertEquals("Incorrect view name", successView, mv.getViewName());

		TestBean command = (TestBean) mv.getModel().get(ctl.getCommandName());

		testCommandIsBound(command, name, age);
	}

	public void testFormSubmissionWithErrors() throws Exception {
		String successView = "successView";
		String formView = "formView";

		TestController ctl = new TestController();
		ctl.setSuccessView(successView);
		ctl.setFormView(formView);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("POST");
		request.addParameter("name", "Rob Harrop");
		request.addParameter("age", "xxx23");

		ModelAndView mv = ctl.handleRequest(request, response);
		assertEquals("Incorrect view name", formView, mv.getViewName());

		Errors errors = (Errors) mv.getModel().get(BindException.MODEL_KEY_PREFIX + ctl.getCommandName());
		assertNotNull("No errors", errors);
		assertEquals(1, errors.getErrorCount());
	}

	public void testFormSubmissionWithValidationError() throws Exception {
		String successView = "successView";
		String formView = "formView";

		TestController ctl = new TestController();
		ctl.setSuccessView(successView);
		ctl.setFormView(formView);
		TestValidator val = new TestValidator();
		ctl.setValidator(val);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("POST");
		request.addParameter("name", "Rob Harrop");
		request.addParameter("age", "23");

		ModelAndView mv = ctl.handleRequest(request, response);
		assertEquals("Incorrect view name", formView, mv.getViewName());

		Errors errors = (Errors) mv.getModel().get(BindException.MODEL_KEY_PREFIX + ctl.getCommandName());
		assertNotNull("No errors", errors);
		assertEquals(1, errors.getErrorCount());
		assertTrue(val.invoked);
	}

	public void testCancelSubmission() throws Exception {
		String cancelView = "cancelView";
	String cancelParameterKey = "cancelRequest";

		TestController ctl = new TestController();
		ctl.setCancelParamKey(cancelParameterKey);
		ctl.setCancelView(cancelView);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("POST");
		request.addParameter("cancelRequest", "true");

		ModelAndView mv = ctl.handleRequest(request, response);
		assertEquals("Incorrect view name", cancelView, mv.getViewName());
	}

	public void testCancelSubmissionWithValidationError() throws Exception {
		String cancelView = "cancelView";
	String cancelParameterKey = "cancelRequest";

		TestController ctl = new TestController();
		ctl.setCancelParamKey(cancelParameterKey);
		ctl.setCancelView(cancelView);
		TestValidator val = new TestValidator();
		ctl.setValidator(val);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("POST");
		request.addParameter("name", "Rob Harrop");
		request.addParameter("age", "23");
		request.addParameter("cancelRequest", "true");

		ModelAndView mv = ctl.handleRequest(request, response);
		assertEquals("Incorrect view name", cancelView, mv.getViewName());

		assertFalse(val.invoked);
	}

	public void testCancelSubmissionWithCustomModelParams() throws Exception {
		String cancelView = "cancelView";
	String cancelParameterKey = "cancelRequest";
	final String reason = "Because I wanted to";

		TestController ctl = new TestController() {
			protected ModelAndView onCancel(HttpServletRequest request, HttpServletResponse response, Object command) {
				return new ModelAndView(getCancelView(), "reason", reason);
			}
		};

		ctl.setCancelParamKey(cancelParameterKey);
		ctl.setCancelView(cancelView);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("POST");
		request.addParameter("cancelRequest", "true");

		ModelAndView mv = ctl.handleRequest(request, response);
		assertEquals("Incorrect view name", cancelView, mv.getViewName());
		assertEquals("Model parameter reason not correct", reason, mv.getModel().get("reason"));
	}

	private void testCommandIsBound(TestBean command, String name, int age) {
		assertNotNull("Command bean should not be null", command);
		assertEquals("Name not bound", name, command.getName());
		assertEquals("Age not bound", age, command.getAge());
	}


	private static class TestController extends CancellableFormController {

		public TestController() {
			setCommandClass(TestBean.class);
		}
	}


	private static class TestValidator implements Validator {

		private boolean invoked = false;

		public boolean supports(Class clazz) {
			return TestBean.class.isAssignableFrom(clazz);
		}
		public void validate(Object target, Errors errors) {
			this.invoked = true;
			TestBean tb = (TestBean) target;
			if (tb.getAge() < 25) {
				errors.rejectValue("age", "TOO_YOUNG");
			}
		}
	}

}
