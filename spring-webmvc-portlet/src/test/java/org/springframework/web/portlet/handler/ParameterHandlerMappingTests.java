/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.web.portlet.context.ConfigurablePortletApplicationContext;
import org.springframework.web.portlet.context.XmlPortletApplicationContext;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Sam Brannen
 */
public class ParameterHandlerMappingTests {

	public static final String CONF = "/org/springframework/web/portlet/handler/parameterMapping.xml";

	private final ConfigurablePortletApplicationContext pac = new XmlPortletApplicationContext();

	private final MockPortletContext portletContext = new MockPortletContext();

	private final MockPortletRequest request = new MockPortletRequest();

	private ParameterHandlerMapping hm;

	@Before
	public void setUp() throws Exception {
		pac.setPortletContext(portletContext);
		pac.setConfigLocations(new String[] {CONF});
		pac.refresh();

		hm = pac.getBean(ParameterHandlerMapping.class);
	}

	@Test
	public void parameterMapping() throws Exception {
		MockPortletRequest addRequest = request;
		addRequest.addParameter("action", "add");
		Object addHandler = hm.getHandler(addRequest).getHandler();
		assertEquals(pac.getBean("addItemHandler"), addHandler);

		MockPortletRequest removeRequest = new MockPortletRequest();
		removeRequest.addParameter("action", "remove");
		Object removeHandler = hm.getHandler(removeRequest).getHandler();
		assertEquals(pac.getBean("removeItemHandler"), removeHandler);
	}

	@Test
	public void unregisteredHandlerWithNoDefault() throws Exception {
		request.addParameter("action", "modify");

		assertNull(hm.getHandler(request));
	}

	@Test
	public void unregisteredHandlerWithDefault() throws Exception {
		Object defaultHandler = new Object();
		hm.setDefaultHandler(defaultHandler);
		request.addParameter("action", "modify");

		assertNotNull(hm.getHandler(request));
		assertEquals(defaultHandler, hm.getHandler(request).getHandler());
	}

	@Test
	public void configuredParameterName() throws Exception {
		hm.setParameterName("someParam");
		request.addParameter("someParam", "add");

		Object handler = hm.getHandler(request).getHandler();
		assertEquals(pac.getBean("addItemHandler"), handler);
	}

	@Test(expected = IllegalStateException.class)
	public void duplicateMappingAttempt() {
		hm.registerHandler("add", new Object());
	}

}
