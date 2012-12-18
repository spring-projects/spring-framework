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

import javax.portlet.PortletException;
import javax.portlet.PortletMode;

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.web.portlet.ModelAndView;

/**
 * @author Mark Fisher
 */
public class PortletModeNameViewControllerTests extends TestCase {

	private PortletModeNameViewController controller;

	public void setUp() {
		controller = new PortletModeNameViewController();
	}

	public void testEditPortletMode() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.EDIT);
		ModelAndView mav = controller.handleRenderRequest(request, response);
		assertEquals("edit", mav.getViewName());
	}

	public void testHelpPortletMode() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.HELP);
		ModelAndView mav = controller.handleRenderRequest(request, response);
		assertEquals("help", mav.getViewName());
	}

	public void testViewPortletMode() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.VIEW);
		ModelAndView mav = controller.handleRenderRequest(request, response);
		assertEquals("view", mav.getViewName());
	}

	public void testActionRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		try {
			controller.handleActionRequest(request, response);
			fail("Should have thrown PortletException");
		}
		catch(PortletException ex) {
			// expected
		}
	}
}
