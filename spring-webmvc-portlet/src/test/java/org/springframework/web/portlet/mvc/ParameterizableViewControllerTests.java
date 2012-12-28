/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.StaticPortletApplicationContext;

/**
 * @author Mark Fisher
 */
public class ParameterizableViewControllerTests extends TestCase {

	public void testRenderRequestWithViewNameSet() throws Exception {
		ParameterizableViewController controller = new ParameterizableViewController();
		String viewName = "testView";
		controller.setViewName(viewName);
		RenderRequest request = new MockRenderRequest();
		RenderResponse response = new MockRenderResponse();
		ModelAndView mav = controller.handleRenderRequest(request, response);
		assertEquals(viewName, mav.getViewName());
	}

	public void testInitApplicationContextWithNoViewNameSet() throws Exception {
		ParameterizableViewController controller = new ParameterizableViewController();
		try {
			controller.setApplicationContext(new StaticPortletApplicationContext());
			fail("should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testActionRequestNotHandled() throws Exception {
		ParameterizableViewController controller = new ParameterizableViewController();
		ActionRequest request = new MockActionRequest();
		ActionResponse response = new MockActionResponse();
		try {
			controller.handleActionRequest(request, response);
			fail("should have thrown PortletException");
		}
		catch (PortletException ex) {
			// expected
		}
	}

}
