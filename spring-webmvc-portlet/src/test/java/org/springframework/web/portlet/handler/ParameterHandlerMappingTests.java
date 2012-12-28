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

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.web.portlet.HandlerMapping;
import org.springframework.web.portlet.context.ConfigurablePortletApplicationContext;
import org.springframework.web.portlet.context.XmlPortletApplicationContext;

/**
 * @author Mark Fisher
 */
public class ParameterHandlerMappingTests extends TestCase {

	public static final String CONF = "/org/springframework/web/portlet/handler/parameterMapping.xml";

	private ConfigurablePortletApplicationContext pac;

	@Override
	public void setUp() throws Exception {
		MockPortletContext portletContext = new MockPortletContext();
		pac = new XmlPortletApplicationContext();
		pac.setPortletContext(portletContext);
		pac.setConfigLocations(new String[] {CONF});
		pac.refresh();
	}

	public void testParameterMapping() throws Exception {
		HandlerMapping hm = (HandlerMapping)pac.getBean("handlerMapping");

		MockPortletRequest addRequest = new MockPortletRequest();
		addRequest.addParameter("action", "add");

		MockPortletRequest removeRequest = new MockPortletRequest();
		removeRequest.addParameter("action", "remove");

		Object addHandler = hm.getHandler(addRequest).getHandler();
		Object removeHandler = hm.getHandler(removeRequest).getHandler();

		assertEquals(pac.getBean("addItemHandler"), addHandler);
		assertEquals(pac.getBean("removeItemHandler"), removeHandler);
	}

	public void testUnregisteredHandlerWithNoDefault() throws Exception {
		HandlerMapping hm = (HandlerMapping)pac.getBean("handlerMapping");

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("action", "modify");

		assertNull(hm.getHandler(request));
	}

	public void testUnregisteredHandlerWithDefault() throws Exception {
		ParameterHandlerMapping hm = (ParameterHandlerMapping)pac.getBean("handlerMapping");
		Object defaultHandler = new Object();
		hm.setDefaultHandler(defaultHandler);

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("action", "modify");

		assertNotNull(hm.getHandler(request));
		assertEquals(defaultHandler, hm.getHandler(request).getHandler());
	}

	public void testConfiguredParameterName() throws Exception {
		ParameterHandlerMapping hm = (ParameterHandlerMapping)pac.getBean("handlerMapping");
		hm.setParameterName("someParam");

		MockPortletRequest request = new MockPortletRequest();
		request.addParameter("someParam", "add");

		Object handler = hm.getHandler(request).getHandler();
		assertEquals(pac.getBean("addItemHandler"), handler);
	}

	public void testDuplicateMappingAttempt() {
		ParameterHandlerMapping hm = (ParameterHandlerMapping)pac.getBean("handlerMapping");
		try {
			hm.registerHandler("add", new Object());
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

}
