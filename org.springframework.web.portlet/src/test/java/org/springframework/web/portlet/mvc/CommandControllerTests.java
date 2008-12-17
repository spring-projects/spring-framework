/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.portlet.mvc;

import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;

import junit.framework.TestCase;

import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.PortletRequestDataBinder;
import org.springframework.web.portlet.handler.PortletSessionRequiredException;

/**
 * @author Mark Fisher
 */
public class CommandControllerTests extends TestCase {
	
	private static final String ERRORS_KEY = "errors";

	public void testRenderRequestWithNoParams() throws Exception {
		TestController tc = new TestController();
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setContextPath("test");
		ModelAndView mav = tc.handleRenderRequest(request, response);
		assertEquals("test-view", mav.getViewName());
		assertNotNull(mav.getModel().get(tc.getCommandName()));
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertNotNull(errors);
		assertEquals("There should be no errors", 0, errors.getErrorCount());
	}

	public void testRenderRequestWithParams() throws Exception {
		TestController tc = new TestController();
		MockRenderRequest request = new MockRenderRequest();		
		MockRenderResponse response = new MockRenderResponse();
		String name = "test";
		int age = 30;
		request.addParameter("name", name);
		request.addParameter("age", "" + age);
		request.setContextPath("test");
		ModelAndView mav = tc.handleRenderRequest(request, response);
		assertEquals("test-view", mav.getViewName());
		TestBean command = (TestBean)mav.getModel().get(tc.getCommandName());
		assertEquals("Name should be bound", name, command.getName());
		assertEquals("Age should be bound", age, command.getAge());
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertNotNull(errors);
		assertEquals("There should be no errors", 0, errors.getErrorCount());
	}
	
	public void testRenderRequestWithMismatch() throws Exception {
		TestController tc = new TestController();
		MockRenderRequest request = new MockRenderRequest();		
		MockRenderResponse response = new MockRenderResponse();
		String name = "test";
		request.addParameter("name", name);
		request.addParameter("age", "zzz");
		request.setContextPath("test");
		ModelAndView mav = tc.handleRenderRequest(request, response);
		assertEquals("test-view", mav.getViewName());
		TestBean command = (TestBean)mav.getModel().get(tc.getCommandName());
		assertNotNull(command);
		assertEquals("Name should be bound", name, command.getName());
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be 1 error", 1, errors.getErrorCount());		
		assertNotNull(errors.getFieldError("age"));
		assertEquals("typeMismatch", errors.getFieldError("age").getCode());
	}
	
	public void testRenderWhenMinimizedReturnsNull() throws Exception {
		TestController tc = new TestController();
		assertFalse(tc.isRenderWhenMinimized());
		MockRenderRequest request = new MockRenderRequest();
		request.setWindowState(WindowState.MINIMIZED);
		MockRenderResponse response = new MockRenderResponse();
		ModelAndView mav = tc.handleRenderRequest(request, response);
		assertNull("ModelAndView should be null", mav);
	}

	public void testAllowRenderWhenMinimized() throws Exception {
		TestController tc = new TestController();
		tc.setRenderWhenMinimized(true);
		MockRenderRequest request = new MockRenderRequest();
		request.setWindowState(WindowState.MINIMIZED);
		request.setContextPath("test");
		MockRenderResponse response = new MockRenderResponse();
		ModelAndView mav = tc.handleRenderRequest(request, response);
		assertNotNull("ModelAndView should not be null", mav);		
		assertEquals("test-view", mav.getViewName());
		assertNotNull(mav.getModel().get(tc.getCommandName()));
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be no errors", 0, errors.getErrorCount());
	}
	
	public void testRequiresSessionWithoutSession() throws Exception {
		TestController tc = new TestController();
		tc.setRequireSession(true);
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		try {
			tc.handleRenderRequest(request, response);
			fail("Should have thrown PortletSessionRequiredException");
		}
		catch (PortletSessionRequiredException ex) {
			// expected
		}
	}

	public void testRequiresSessionWithSession() throws Exception {
		TestController tc = new TestController();
		tc.setRequireSession(true);
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		
		// create the session
		request.getPortletSession(true);
		try {
			tc.handleRenderRequest(request, response);
		}
		catch (PortletSessionRequiredException ex) {
			fail("Should not have thrown PortletSessionRequiredException");
		}
	}
	
	public void testRenderRequestWithoutCacheSetting() throws Exception {
		TestController tc = new TestController();
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		tc.handleRenderRequest(request, response);
		String cacheProperty = response.getProperty(RenderResponse.EXPIRATION_CACHE);
		assertNull("Expiration-cache should be null", cacheProperty);
	}

	public void testRenderRequestWithNegativeCacheSetting() throws Exception {
		TestController tc = new TestController();
		tc.setCacheSeconds(-99);
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		tc.handleRenderRequest(request, response);		
		String cacheProperty = response.getProperty(RenderResponse.EXPIRATION_CACHE);
		assertNull("Expiration-cache should be null", cacheProperty);
	}
	
	public void testRenderRequestWithZeroCacheSetting() throws Exception {
		TestController tc = new TestController();
		tc.setCacheSeconds(0);
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		tc.handleRenderRequest(request, response);		
		String cacheProperty = response.getProperty(RenderResponse.EXPIRATION_CACHE);
		assertEquals("Expiration-cache should be set to 0 seconds", "0", cacheProperty);
	}

	public void testRenderRequestWithPositiveCacheSetting() throws Exception {
		TestController tc = new TestController();
		tc.setCacheSeconds(30);
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		tc.handleRenderRequest(request, response);		
		String cacheProperty = response.getProperty(RenderResponse.EXPIRATION_CACHE);
		assertEquals("Expiration-cache should be set to 30 seconds", "30", cacheProperty);
	}
	
	public void testActionRequest() throws Exception {
		TestController tc = new TestController();
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		tc.handleActionRequest(request, response);
		TestBean command = (TestBean)request.getPortletSession().getAttribute(tc.getRenderCommandSessionAttributeName());
		assertTrue(command.isJedi());
	}
	
	public void testSuppressBinding() throws Exception {
		TestController tc = new TestController() {
			protected boolean suppressBinding(PortletRequest request) {
				return true;
			}
		};
		MockRenderRequest request = new MockRenderRequest();		
		MockRenderResponse response = new MockRenderResponse();
		String name = "test";
		int age = 30;
		request.addParameter("name", name);
		request.addParameter("age", "" + age);
		request.setContextPath("test");
		ModelAndView mav = tc.handleRenderRequest(request, response);
		assertEquals("test-view", mav.getViewName());
		TestBean command = (TestBean)mav.getModel().get(tc.getCommandName());
		assertNotNull(command);
		assertTrue("Name should not have been bound", name != command.getName());
		assertTrue("Age should not have been bound", age != command.getAge());
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be no errors", 0, errors.getErrorCount());
	}
	
	public void testWithCustomDateEditor() throws Exception {
		final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		TestController tc = new TestController() {
			protected void initBinder(PortletRequest request, PortletRequestDataBinder binder) {
				binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
			}
		};
		MockRenderRequest request = new MockRenderRequest();		
		MockRenderResponse response = new MockRenderResponse();
		String name = "test";
		int age = 30;
		request.addParameter("name", name);
		request.addParameter("age", "" + age);
		String dateString = "07-03-2006";
		Date expectedDate = dateFormat.parse(dateString);
		request.addParameter("date", dateString);
		ModelAndView mav = tc.handleRenderRequest(request, response);
		TestBean command = (TestBean)mav.getModel().get(tc.getCommandName());
		assertEquals(name, command.getName());
		assertEquals(age, command.getAge());
		assertEquals(expectedDate, command.getDate());
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be no errors", 0, errors.getErrorCount());
	}

	public void testWithCustomDateEditorEmptyNotAllowed() throws Exception {
		final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		TestController tc = new TestController() {
			protected void initBinder(PortletRequest request, PortletRequestDataBinder binder) {
				binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
			}
		};
		MockRenderRequest request = new MockRenderRequest();		
		MockRenderResponse response = new MockRenderResponse();
		String name = "test";
		int age = 30;
		request.addParameter("name", name);
		request.addParameter("age", "" + age);
		String emptyString = "";
		request.addParameter("date", emptyString);
		ModelAndView mav = tc.handleRenderRequest(request, response);
		TestBean command = (TestBean)mav.getModel().get(tc.getCommandName());
		assertEquals(name, command.getName());
		assertEquals(age, command.getAge());
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be 1 error", 1, errors.getErrorCount());		
		assertNotNull(errors.getFieldError("date"));
		assertEquals("typeMismatch", errors.getFieldError("date").getCode());
		assertEquals(emptyString, errors.getFieldError("date").getRejectedValue());
	}

	public void testWithCustomDateEditorEmptyAllowed() throws Exception {
		final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		TestController tc = new TestController() {
			protected void initBinder(PortletRequest request, PortletRequestDataBinder binder) {
				binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
			}
		};
		MockRenderRequest request = new MockRenderRequest();		
		MockRenderResponse response = new MockRenderResponse();
		String name = "test";
		int age = 30;
		request.addParameter("name", name);
		request.addParameter("age", "" + age);
		String dateString = "";
		request.addParameter("date", dateString);
		ModelAndView mav = tc.handleRenderRequest(request, response);
		TestBean command = (TestBean)mav.getModel().get(tc.getCommandName());
		assertEquals(name, command.getName());
		assertEquals(age, command.getAge());
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be 0 errors", 0, errors.getErrorCount());		
		assertNull("date should be null", command.getDate());
	}
	
	public void testNestedBindingWithPropertyEditor() throws Exception {
		TestController tc = new TestController() {
			protected void initBinder(PortletRequest request, PortletRequestDataBinder binder) {
				binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
					public void setAsText(String text) throws IllegalArgumentException {
						setValue(new TestBean(text));
					}
				});
			}
		};
		MockRenderRequest request = new MockRenderRequest();		
		MockRenderResponse response = new MockRenderResponse();
		String name = "test";
		String spouseName = "testSpouse";
		int age = 30;
		int spouseAge = 31;
		request.addParameter("name", name);
		request.addParameter("age", "" + age);
		request.addParameter("spouse", spouseName);
		request.addParameter("spouse.age", "" + spouseAge);
		ModelAndView mav = tc.handleRenderRequest(request, response);
		TestBean command = (TestBean)mav.getModel().get(tc.getCommandName());
		assertEquals(name, command.getName());
		assertEquals(age, command.getAge());
		assertNotNull(command.getSpouse());
		assertEquals(spouseName, command.getSpouse().getName());
		assertEquals(spouseAge, command.getSpouse().getAge());
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be no errors", 0, errors.getErrorCount());
	}
	
	public void testWithValidatorNotSupportingCommandClass() throws Exception {
		Validator v = new Validator() {
			public boolean supports(Class c) {
				return false;
			}
			public void validate(Object o, Errors e) {}
		};
		TestController tc = new TestController();
		tc.setValidator(v);
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		try {
			tc.handleRenderRequest(request, response);
			fail("Should have thrown IllegalArgumentException");
		}
		catch(IllegalArgumentException e) {
			// expected
		}
	}

	public void testWithValidatorAddingGlobalError() throws Exception {
		final String errorCode = "someCode";
		final String defaultMessage = "validation error!";
		TestController tc = new TestController();
		tc.setValidator(new Validator() {
			public boolean supports(Class c) {
				return TestBean.class.isAssignableFrom(c);
			}
			public void validate(Object o, Errors e) {
				e.reject(errorCode, defaultMessage);
			}
		});
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		ModelAndView mav = tc.handleRenderRequest(request, response);
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be 1 error", 1, errors.getErrorCount());
		ObjectError error = errors.getGlobalError();
		assertEquals(error.getCode(), errorCode);
		assertEquals(error.getDefaultMessage(), defaultMessage);
	}

	public void testWithValidatorAndNullFieldError() throws Exception {
		final String errorCode = "someCode";
		final String defaultMessage = "validation error!";
		TestController tc = new TestController();
		tc.setValidator(new Validator() {
			public boolean supports(Class c) {
				return TestBean.class.isAssignableFrom(c);
			}
			public void validate(Object o, Errors e) {
				ValidationUtils.rejectIfEmpty(e, "name", errorCode, defaultMessage);
			}
		});
		MockRenderRequest request = new MockRenderRequest();
		int age = 32;
		request.setParameter("age", "" + age);
		MockRenderResponse response = new MockRenderResponse();
		ModelAndView mav = tc.handleRenderRequest(request, response);
		TestBean command = (TestBean)mav.getModel().get(tc.getCommandName());
		assertNull("name should be null", command.getName());
		assertEquals(age, command.getAge());
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be 1 error", 1, errors.getErrorCount());
		FieldError error = errors.getFieldError("name");
		assertEquals(error.getCode(), errorCode);
		assertEquals(error.getDefaultMessage(), defaultMessage);
	}

	public void testWithValidatorAndWhitespaceFieldError() throws Exception {
		final String errorCode = "someCode";
		final String defaultMessage = "validation error!";
		TestController tc = new TestController();
		tc.setValidator(new Validator() {
			public boolean supports(Class c) {
				return TestBean.class.isAssignableFrom(c);
			}
			public void validate(Object o, Errors e) {
				ValidationUtils.rejectIfEmptyOrWhitespace(e, "name", errorCode, defaultMessage);
			}
		});
		MockRenderRequest request = new MockRenderRequest();
		int age = 32;
		String whitespace = "  \t  ";
		request.setParameter("age", "" + age);
		request.setParameter("name", whitespace);
		MockRenderResponse response = new MockRenderResponse();
		ModelAndView mav = tc.handleRenderRequest(request, response);
		TestBean command = (TestBean)mav.getModel().get(tc.getCommandName());
		assertTrue(command.getName().equals(whitespace));
		assertEquals(age, command.getAge());
		BindException errors = (BindException)mav.getModel().get(ERRORS_KEY);
		assertEquals("There should be 1 error", 1, errors.getErrorCount());
		FieldError error = errors.getFieldError("name");
		assertEquals("rejected value should contain whitespace", whitespace, error.getRejectedValue());
		assertEquals(error.getCode(), errorCode);
		assertEquals(error.getDefaultMessage(), defaultMessage);
	}

	private static class TestController extends AbstractCommandController {

		private TestController() {
			super(TestBean.class, "testBean");
		}
		
		protected void handleAction(ActionRequest request, ActionResponse response, Object command, BindException errors) {
			((TestBean)command).setJedi(true);
		}

		protected ModelAndView handleRender(RenderRequest request, RenderResponse response, Object command, BindException errors) {
			assertNotNull(command);
			assertNotNull(errors);
			Map model = new HashMap();
			model.put(getCommandName(), command);
			model.put(ERRORS_KEY, errors);
			return new ModelAndView(request.getContextPath() + "-view", model);
		}
	}

}
