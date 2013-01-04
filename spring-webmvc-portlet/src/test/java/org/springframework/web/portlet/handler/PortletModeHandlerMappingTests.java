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

package org.springframework.web.portlet.handler;

import javax.portlet.PortletMode;

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.web.portlet.HandlerMapping;
import org.springframework.web.portlet.context.ConfigurablePortletApplicationContext;
import org.springframework.web.portlet.context.XmlPortletApplicationContext;

/**
 * @author Mark Fisher
 */
public class PortletModeHandlerMappingTests extends TestCase {

	public static final String CONF = "/org/springframework/web/portlet/handler/portletModeMapping.xml";

	private ConfigurablePortletApplicationContext pac;

	@Override
	public void setUp() throws Exception {
		MockPortletContext portletContext = new MockPortletContext();
		pac = new XmlPortletApplicationContext();
		pac.setPortletContext(portletContext);
		pac.setConfigLocations(new String[] {CONF});
		pac.refresh();
	}

	public void testPortletModeView() throws Exception {
		HandlerMapping hm = (HandlerMapping)pac.getBean("handlerMapping");

		MockPortletRequest request = new MockPortletRequest();
		request.setPortletMode(PortletMode.VIEW);

		Object handler = hm.getHandler(request).getHandler();
		assertEquals(pac.getBean("viewHandler"), handler);
	}

	public void testPortletModeEdit() throws Exception {
		HandlerMapping hm = (HandlerMapping)pac.getBean("handlerMapping");

		MockPortletRequest request = new MockPortletRequest();
		request.setPortletMode(PortletMode.EDIT);

		Object handler = hm.getHandler(request).getHandler();
		assertEquals(pac.getBean("editHandler"), handler);
	}

	public void testPortletModeHelp() throws Exception {
		HandlerMapping hm = (HandlerMapping)pac.getBean("handlerMapping");

		MockPortletRequest request = new MockPortletRequest();
		request.setPortletMode(PortletMode.HELP);

		Object handler = hm.getHandler(request).getHandler();
		assertEquals(pac.getBean("helpHandler"), handler);
	}

	public void testDuplicateMappingAttempt() {
		PortletModeHandlerMapping hm = (PortletModeHandlerMapping)pac.getBean("handlerMapping");
		try {
			hm.registerHandler(PortletMode.VIEW, new Object());
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

}
