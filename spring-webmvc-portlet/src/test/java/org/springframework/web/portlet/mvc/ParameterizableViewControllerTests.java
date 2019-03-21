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

package org.springframework.web.portlet.mvc;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.junit.Test;

import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.StaticPortletApplicationContext;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Sam Brannen
 */
public class ParameterizableViewControllerTests {

	private final ParameterizableViewController controller = new ParameterizableViewController();

	private final RenderRequest request = new MockRenderRequest();

	private final RenderResponse response = new MockRenderResponse();


	@Test
	public void renderRequestWithViewNameSet() throws Exception {
		String viewName = "testView";
		controller.setViewName(viewName);
		ModelAndView mav = controller.handleRenderRequest(request, response);
		assertEquals(viewName, mav.getViewName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void initApplicationContextWithNoViewNameSet() throws Exception {
		controller.setApplicationContext(new StaticPortletApplicationContext());
	}

	@Test(expected = PortletException.class)
	public void actionRequestNotHandled() throws Exception {
		controller.handleActionRequest(new MockActionRequest(), new MockActionResponse());
	}

}
