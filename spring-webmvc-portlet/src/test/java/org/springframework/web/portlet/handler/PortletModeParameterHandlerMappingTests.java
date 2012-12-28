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
public class PortletModeParameterHandlerMappingTests extends TestCase {

	public static final String CONF = "/org/springframework/web/portlet/handler/portletModeParameterMapping.xml";

	private ConfigurablePortletApplicationContext pac;

	@Override
	public void setUp() throws Exception {
		MockPortletContext portletContext = new MockPortletContext();
		pac = new XmlPortletApplicationContext();
		pac.setPortletContext(portletContext);
		pac.setConfigLocations(new String[] {CONF});
		pac.refresh();
	}

	public void testPortletModeViewWithParameter() throws Exception {
		HandlerMapping hm = (HandlerMapping)pac.getBean("handlerMapping");

		MockPortletRequest addRequest = new MockPortletRequest();
		addRequest.setPortletMode(PortletMode.VIEW);
		addRequest.setParameter("action", "add");

		MockPortletRequest removeRequest = new MockPortletRequest();
		removeRequest.setPortletMode(PortletMode.VIEW);
		removeRequest.setParameter("action", "remove");

		Object addHandler = hm.getHandler(addRequest).getHandler();
		Object removeHandler = hm.getHandler(removeRequest).getHandler();

		assertEquals(pac.getBean("addItemHandler"), addHandler);
		assertEquals(pac.getBean("removeItemHandler"), removeHandler);
	}

	public void testPortletModeEditWithParameter() throws Exception {
		HandlerMapping hm = (HandlerMapping)pac.getBean("handlerMapping");

		MockPortletRequest request = new MockPortletRequest();
		request.setPortletMode(PortletMode.EDIT);
		request.setParameter("action", "prefs");

		Object handler = hm.getHandler(request).getHandler();
		assertEquals(pac.getBean("preferencesHandler"), handler);
	}

	public void testDuplicateMappingInSamePortletMode() {
		PortletModeParameterHandlerMapping hm = (PortletModeParameterHandlerMapping)pac.getBean("handlerMapping");
		try {
			hm.registerHandler(PortletMode.VIEW, "remove", new Object());
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	public void testDuplicateMappingInDifferentPortletMode() {
		PortletModeParameterHandlerMapping hm = (PortletModeParameterHandlerMapping)pac.getBean("handlerMapping");
		try {
			hm.registerHandler(PortletMode.EDIT, "remove", new Object());
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	public void testAllowDuplicateMappingInDifferentPortletMode() throws Exception {
		PortletModeParameterHandlerMapping hm = (PortletModeParameterHandlerMapping)pac.getBean("handlerMapping");
		hm.setAllowDuplicateParameters(true);

		Object editRemoveHandler = new Object();
		hm.registerHandler(PortletMode.EDIT, "remove", editRemoveHandler);

		MockPortletRequest request = new MockPortletRequest();
		request.setPortletMode(PortletMode.EDIT);
		request.setParameter("action", "remove");

		Object handler = hm.getHandler(request).getHandler();
		assertEquals(editRemoveHandler, handler);
	}

}
