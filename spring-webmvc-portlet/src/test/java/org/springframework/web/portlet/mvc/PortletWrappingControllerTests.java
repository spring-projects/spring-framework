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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.web.portlet.context.ConfigurablePortletApplicationContext;
import org.springframework.web.portlet.context.StaticPortletApplicationContext;

/**
 * Unit tests for the {@link PortletWrappingController} class.
 *
 * @author Mark Fisher
 * @author Rick Evans
 * @author Chris Beams
 */
public final class PortletWrappingControllerTests {

	private static final String RESULT_RENDER_PARAMETER_NAME = "result";
	private static final String PORTLET_WRAPPING_CONTROLLER_BEAN_NAME = "controller";
	private static final String RENDERED_RESPONSE_CONTENT = "myPortlet-view";
	private static final String PORTLET_NAME_ACTION_REQUEST_PARAMETER_NAME = "portletName";


	private PortletWrappingController controller;


	@Before
	public void setUp() {
		ConfigurablePortletApplicationContext applicationContext = new MyApplicationContext();
		MockPortletConfig config = new MockPortletConfig(new MockPortletContext(), "wrappedPortlet");
		applicationContext.setPortletConfig(config);
		applicationContext.refresh();
		controller = (PortletWrappingController) applicationContext.getBean(PORTLET_WRAPPING_CONTROLLER_BEAN_NAME);
	}


	@Test
	public void testActionRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setParameter("test", "test");
		controller.handleActionRequest(request, response);
		String result = response.getRenderParameter(RESULT_RENDER_PARAMETER_NAME);
		assertEquals("myPortlet-action", result);
	}

	@Test
	public void testRenderRequest() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		controller.handleRenderRequest(request, response);
		String result = response.getContentAsString();
		assertEquals(RENDERED_RESPONSE_CONTENT, result);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testActionRequestWithNoParameters() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		controller.handleActionRequest(request, response);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRejectsPortletClassThatDoesNotImplementPortletInterface() throws Exception {
		PortletWrappingController controller = new PortletWrappingController();
		controller.setPortletClass(String.class);
		controller.afterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRejectsIfPortletClassIsNotSupplied() throws Exception {
		PortletWrappingController controller = new PortletWrappingController();
		controller.setPortletClass(null);
		controller.afterPropertiesSet();
	}

	@Test(expected=IllegalStateException.class)
	public void testDestroyingTheControllerPropagatesDestroyToWrappedPortlet() throws Exception {
		final PortletWrappingController controller = new PortletWrappingController();
		controller.setPortletClass(MyPortlet.class);
		controller.afterPropertiesSet();
		// test for destroy() call being propagated via exception being thrown :(
		controller.destroy();
	}

	@Test
	public void testPortletName() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setParameter(PORTLET_NAME_ACTION_REQUEST_PARAMETER_NAME, "test");
		controller.handleActionRequest(request, response);
		String result = response.getRenderParameter(RESULT_RENDER_PARAMETER_NAME);
		assertEquals("wrappedPortlet", result);
	}

	@Test
	public void testDelegationToMockPortletConfigIfSoConfigured() throws Exception {

		final String BEAN_NAME = "Sixpence None The Richer";

		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();

		PortletWrappingController controller = new PortletWrappingController();
		controller.setPortletClass(MyPortlet.class);
		controller.setUseSharedPortletConfig(false);
		controller.setBeanName(BEAN_NAME);
		controller.afterPropertiesSet();

		request.setParameter(PORTLET_NAME_ACTION_REQUEST_PARAMETER_NAME, "true");
		controller.handleActionRequest(request, response);

		String result = response.getRenderParameter(RESULT_RENDER_PARAMETER_NAME);
		assertEquals(BEAN_NAME, result);
	}


	public static final class MyPortlet implements Portlet {

		private PortletConfig portletConfig;


		@Override
		public void init(PortletConfig portletConfig) {
			this.portletConfig = portletConfig;
		}

		@Override
		public void processAction(ActionRequest request, ActionResponse response) throws PortletException {
			if (request.getParameter("test") != null) {
				response.setRenderParameter(RESULT_RENDER_PARAMETER_NAME, "myPortlet-action");
			} else if (request.getParameter(PORTLET_NAME_ACTION_REQUEST_PARAMETER_NAME) != null) {
				response.setRenderParameter(RESULT_RENDER_PARAMETER_NAME, getPortletConfig().getPortletName());
			} else {
				throw new IllegalArgumentException("no request parameters");
			}
		}

		@Override
		public void render(RenderRequest request, RenderResponse response) throws IOException {
			response.getWriter().write(RENDERED_RESPONSE_CONTENT);
		}

		public PortletConfig getPortletConfig() {
			return this.portletConfig;
		}

		@Override
		public void destroy() {
			throw new IllegalStateException("Being destroyed...");
		}

	}

	private static final class MyApplicationContext extends StaticPortletApplicationContext {

		@Override
		public void refresh() throws BeansException {
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.add("portletClass", MyPortlet.class);
			registerSingleton(PORTLET_WRAPPING_CONTROLLER_BEAN_NAME, PortletWrappingController.class, pvs);
			super.refresh();
		}
	}

}
