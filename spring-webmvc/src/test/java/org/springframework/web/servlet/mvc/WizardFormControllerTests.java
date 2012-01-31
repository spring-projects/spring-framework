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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Juergen Hoeller
 * @since 29.04.2003
 */
public class WizardFormControllerTests extends TestCase {

	public void testNoDirtyPageChange() throws Exception {
		AbstractWizardFormController wizard = new TestWizardController();
		wizard.setAllowDirtyBack(false);
		wizard.setAllowDirtyForward(false);
		wizard.setPageAttribute("currentPage");

		assertTrue(wizard.getFormSessionAttributeName() != wizard.getPageSessionAttributeName());
		HttpSession session = performRequest(wizard, null, null, 0, null, 0, "currentPage");

		Properties params = new Properties();
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, null, 0, null, 0, "currentPage");
		// not allowed to go to 1

		params.clear();
		params.setProperty("name", "myname");
		params.setProperty(AbstractWizardFormController.PARAM_PAGE, "0");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, params, 1, "myname", 0, "currentPage");
		// name set -> now allowed to go to 1

		params.clear();
		params.setProperty("name", "myname");
		performRequest(wizard, session, params, 1, "myname", 0, "currentPage");
		// name set -> now allowed to go to 1

		params.clear();
		params.setProperty("name", "myname");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1.x", "value");
		performRequest(wizard, session, params, 1, "myname", 0, "currentPage");
		// name set -> now allowed to go to 1

		params.clear();
		params.setProperty("name", "myname");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1.y", "value");
		performRequest(wizard, session, params, 1, "myname", 0, "currentPage");
		// name set -> now allowed to go to 1

		params.clear();
		params.setProperty("date", "not a date");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1.y", "value");
		performRequest(wizard, session, params, 1, "myname", 0, "currentPage");
		// name set -> now allowed to go to 1

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "0", "value");
		performRequest(wizard, session, params, 1, "myname", 0, "currentPage");
		// not allowed to go to 0

		params.clear();
		params.setProperty("age", "32");
		params.setProperty(AbstractWizardFormController.PARAM_PAGE, "1");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "0", "value");
		performRequest(wizard, session, params, 0, "myname", 32, "currentPage");
		// age set -> now allowed to go to 0

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_FINISH, "value");
		performRequest(wizard, session, params, -1, "myname", 32, null);
	}

	public void testCustomSessionAttributes() throws Exception {
		AbstractWizardFormController wizard = new TestWizardController() {
			protected String getFormSessionAttributeName() {
				return "myFormAttr";
			}
			protected String getPageSessionAttributeName() {
				return "myPageAttr";
			}
		};
		wizard.setAllowDirtyBack(false);
		wizard.setAllowDirtyForward(false);
		wizard.setPageAttribute("currentPage");

		HttpSession session = performRequest(wizard, null, null, 0, null, 0, "currentPage");
		assertTrue(session.getAttribute("myFormAttr") instanceof TestBean);
		assertEquals(new Integer(0), session.getAttribute("myPageAttr"));

		Properties params = new Properties();
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, null, 0, null, 0, "currentPage");
		// not allowed to go to 1

		params.clear();
		params.setProperty("name", "myname");
		params.setProperty(AbstractWizardFormController.PARAM_PAGE, "0");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, params, 1, "myname", 0, "currentPage");
		// name set -> now allowed to go to 1

		params.clear();
		params.setProperty("age", "32");
		params.setProperty(AbstractWizardFormController.PARAM_FINISH, "value");
		performRequest(wizard, session, params, -1, "myname", 32, "currentPage");
	}

	public void testCustomRequestDependentSessionAttributes() throws Exception {
		AbstractWizardFormController wizard = new TestWizardController() {
			protected String getFormSessionAttributeName(HttpServletRequest request) {
				return "myFormAttr" + request.getParameter("formAttr");
			}
			protected String getPageSessionAttributeName(HttpServletRequest request) {
				return "myPageAttr" + request.getParameter("pageAttr");
			}
		};
		wizard.setAllowDirtyBack(false);
		wizard.setAllowDirtyForward(false);
		wizard.setPageAttribute("currentPage");

		HttpSession session = performRequest(wizard, null, null, 0, null, 0, "currentPage");
		assertTrue(session.getAttribute("myFormAttr1") instanceof TestBean);
		assertEquals(new Integer(0), session.getAttribute("myPageAttr2"));

		Properties params = new Properties();
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, null, 0, null, 0, "currentPage");
		// not allowed to go to 1

		params.clear();
		params.setProperty("name", "myname");
		params.setProperty(AbstractWizardFormController.PARAM_PAGE, "0");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, params, 1, "myname", 0, "currentPage");
		// name set -> now allowed to go to 1

		params.clear();
		params.setProperty("age", "32");
		params.setProperty(AbstractWizardFormController.PARAM_FINISH, "value");
		performRequest(wizard, session, params, -1, "myname", 32, "currentPage");
	}

	public void testDirtyBack() throws Exception {
		AbstractWizardFormController wizard = new TestWizardController();
		wizard.setAllowDirtyBack(true);
		wizard.setAllowDirtyForward(false);
		HttpSession session = performRequest(wizard, null, null, 0, null, 0, null);

		Properties params = new Properties();
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, params, 0, null, 0, null);
		// not allowed to go to 1

		params.clear();
		params.setProperty("name", "myname");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, params, 1, "myname", 0, null);
		// name set -> now allowed to go to 1

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "0", "value");
		performRequest(wizard, session, params, 0, "myname", 0, null);
		// dirty back -> allowed to go to 0

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_FINISH, "value");
		performRequest(wizard, session, params, 1, "myname", 0, null);
		// finish while dirty -> show dirty page (1)

		params.clear();
		params.setProperty("age", "32");
		params.setProperty(AbstractWizardFormController.PARAM_FINISH, "value");
		performRequest(wizard, session, params, -1, "myname", 32, null);
		// age set -> now allowed to finish
	}

	public void testDirtyForward() throws Exception {
		AbstractWizardFormController wizard = new TestWizardController();
		wizard.setAllowDirtyBack(false);
		wizard.setAllowDirtyForward(true);
		HttpSession session = performRequest(wizard, null, null, 0, null, 0, null);

		Properties params = new Properties();
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, params, 1, null, 0, null);
		// dirty forward -> allowed to go to 1

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "0", "value");
		performRequest(wizard, session, params, 1, null, 0, null);
		// not allowed to go to 0

		params.clear();
		params.setProperty("age", "32");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "0", "value");
		performRequest(wizard, session, params, 0, null, 32, null);
		// age set -> now allowed to go to 0

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_FINISH, "value");
		performRequest(wizard, session, params, 0, null, 32, null);
		// finish while dirty -> show dirty page (0)

		params.clear();
		params.setProperty("name", "myname");
		params.setProperty(AbstractWizardFormController.PARAM_FINISH + ".x", "value");
		performRequest(wizard, session, params, -1, "myname", 32, null);
		// name set -> now allowed to finish
	}

	public void testSubmitWithoutValidation() throws Exception {
		AbstractWizardFormController wizard = new TestWizardController();
		wizard.setAllowDirtyBack(false);
		wizard.setAllowDirtyForward(false);
		HttpSession session = performRequest(wizard, null, null, 0, null, 0, null);

		Properties params = new Properties();
		params.setProperty("formChange", "true");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, params, 1, null, 0, null);
		// no validation -> allowed to go to 1

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "0", "value");
		performRequest(wizard, session, params, 1, null, 0, null);
		// not allowed to go to 0

		params.clear();
		params.setProperty("age", "32");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "0", "value");
		performRequest(wizard, session, params, 0, null, 32, null);
		// age set -> now allowed to go to 0

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_FINISH, "value");
		performRequest(wizard, session, params, 0, null, 32, null);
		// finish while dirty -> show dirty page (0)

		params.clear();
		params.setProperty("name", "myname");
		params.setProperty(AbstractWizardFormController.PARAM_FINISH + ".x", "value");
		performRequest(wizard, session, params, -1, "myname", 32, null);
		// name set -> now allowed to finish
	}

	public void testCancel() throws Exception {
		AbstractWizardFormController wizard = new TestWizardController();
		HttpSession session = performRequest(wizard, null, null, 0, null, 0, null);
		Properties params = new Properties();
		params.setProperty(AbstractWizardFormController.PARAM_CANCEL, "value");
		performRequest(wizard, session, params, -2, null, 0, null);

		assertTrue(session.getAttribute(wizard.getFormSessionAttributeName()) == null);
		assertTrue(session.getAttribute(wizard.getPageSessionAttributeName()) == null);

		session = performRequest(wizard, null, null, 0, null, 0, null);
		params = new Properties();
		params.setProperty(AbstractWizardFormController.PARAM_CANCEL + ".y", "value");
		performRequest(wizard, session, params, -2, null, 0, null);
	}

	public void testInvalidSubmit() throws Exception {
		AbstractWizardFormController wizard = new TestWizardController();
		wizard.setAllowDirtyBack(false);
		wizard.setAllowDirtyForward(false);
		wizard.setPageAttribute("currentPage");
		HttpSession session = performRequest(wizard, null, null, 0, null, 0, "currentPage");

		Properties params = new Properties();
		params.setProperty("name", "myname");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "1", "value");
		performRequest(wizard, session, params, 1, "myname", 0, "currentPage");

		params.clear();
		params.setProperty("age", "32");
		params.setProperty(AbstractWizardFormController.PARAM_TARGET + "0", "value");
		performRequest(wizard, session, params, 0, "myname", 32, "currentPage");

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_FINISH, "value");
		performRequest(wizard, session, params, -1, "myname", 32, null);

		params.clear();
		params.setProperty(AbstractWizardFormController.PARAM_FINISH, "value");
		performRequest(wizard, session, params, 0, null, 0, "currentPage");
		// returned to initial page of new wizard form
	}

	private HttpSession performRequest(
			AbstractWizardFormController wizard, HttpSession session, Properties params,
			int target, String name, int age, String pageAttr) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest((params != null ? "POST" : "GET"), "/wizard");
		request.addParameter("formAttr", "1");
		request.addParameter("pageAttr", "2");
		if (params != null) {
			for (Iterator it = params.keySet().iterator(); it.hasNext();) {
				String param = (String) it.next();
				request.addParameter(param, params.getProperty(param));
			}
		}
		request.setSession(session);
		request.setAttribute("target", new Integer(target));
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = wizard.handleRequest(request, response);
		if (target >= 0) {
			assertTrue("Page " + target + " returned", ("page" + target).equals(mv.getViewName()));
			if (pageAttr != null) {
				assertTrue("Page attribute set", (new Integer(target)).equals(mv.getModel().get(pageAttr)));
				assertTrue("Correct model size", mv.getModel().size() == 3);
			}
			else {
				assertTrue("Correct model size", mv.getModel().size() == 2);
			}
			assertTrue(
					request.getSession().getAttribute(wizard.getFormSessionAttributeName(request)) instanceof TestBean);
			assertEquals(new Integer(target),
					request.getSession().getAttribute(wizard.getPageSessionAttributeName(request)));
		}
		else if (target == -1) {
			assertTrue("Success target returned", "success".equals(mv.getViewName()));
			assertTrue("Correct model size", mv.getModel().size() == 1);
			assertTrue(request.getSession().getAttribute(wizard.getFormSessionAttributeName(request)) == null);
			assertTrue(request.getSession().getAttribute(wizard.getPageSessionAttributeName(request)) == null);
		}
		else if (target == -2) {
			assertTrue("Cancel view returned", "cancel".equals(mv.getViewName()));
			assertTrue("Correct model size", mv.getModel().size() == 1);
			assertTrue(request.getSession().getAttribute(wizard.getFormSessionAttributeName(request)) == null);
			assertTrue(request.getSession().getAttribute(wizard.getPageSessionAttributeName(request)) == null);
		}
		TestBean tb = (TestBean) mv.getModel().get("tb");
		assertTrue("Has model", tb != null);
		assertTrue("Name is " + name, ObjectUtils.nullSafeEquals(name, tb.getName()));
		assertTrue("Age is " + age, tb.getAge() == age);
		Errors errors = (Errors) mv.getModel().get(BindException.MODEL_KEY_PREFIX + "tb");
		if (params != null && params.containsKey("formChange")) {
			assertNotNull(errors);
			assertFalse(errors.hasErrors());
		}
		return request.getSession(false);
	}


	private static class TestWizardController extends AbstractWizardFormController {

		public TestWizardController() {
			setCommandClass(TestBean.class);
			setCommandName("tb");
			setPages(new String[] {"page0", "page1"});
		}

		protected Map referenceData(HttpServletRequest request, int page) throws Exception {
			assertEquals(new Integer(page), request.getAttribute("target"));
			return super.referenceData(request, page);
		}

		protected boolean suppressValidation(HttpServletRequest request, Object command) {
			return (request.getParameter("formChange") != null);
		}

		protected void validatePage(Object command, Errors errors, int page) {
			TestBean tb = (TestBean) command;
			switch (page) {
				case 0:
					if (tb.getName() == null) {
						errors.rejectValue("name", "NAME_REQUIRED", null, "Name is required");
					}
					break;
				case 1:
					if (tb.getAge() == 0) {
						errors.rejectValue("age", "AGE_REQUIRED", null, "Age is required");
					}
					break;
			  default:
					throw new IllegalArgumentException("Invalid page number");
			}
		}

		protected ModelAndView processFinish(
				HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		    throws ServletException, IOException {
			assertTrue(getCurrentPage(request) == 0 || getCurrentPage(request) == 1);
			return new ModelAndView("success", getCommandName(), command);
		}

		protected ModelAndView processCancel(
				HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		    throws ServletException, IOException {
			assertTrue(getCurrentPage(request) == 0 || getCurrentPage(request) == 1);
			return new ModelAndView("cancel", getCommandName(), command);
		}
	}

}
