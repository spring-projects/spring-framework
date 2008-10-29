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

import junit.framework.TestCase;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.mock.web.portlet.*;
import org.springframework.test.AssertThrows;
import org.springframework.web.portlet.context.ConfigurablePortletApplicationContext;
import org.springframework.web.portlet.context.StaticPortletApplicationContext;

import javax.portlet.*;
import java.io.IOException;

/**
 * Unit tests for the {@link PortletWrappingController} class.
 *
 * @author Mark Fisher
 * @author Rick Evans
 */
public final class PortletWrappingControllerTests extends TestCase {

    private static final String RESULT_RENDER_PARAMETER_NAME = "result";
    private static final String PORTLET_WRAPPING_CONTROLLER_BEAN_NAME = "controller";
    private static final String RENDERED_RESPONSE_CONTENT = "myPortlet-view";
    private static final String PORTLET_NAME_ACTION_REQUEST_PARAMETER_NAME = "portletName";


    private PortletWrappingController controller;


    public void setUp() {
        ConfigurablePortletApplicationContext applicationContext = new MyApplicationContext();
        MockPortletConfig config = new MockPortletConfig(new MockPortletContext(), "wrappedPortlet");
        applicationContext.setPortletConfig(config);
        applicationContext.refresh();
        controller = (PortletWrappingController) applicationContext.getBean(PORTLET_WRAPPING_CONTROLLER_BEAN_NAME);
    }


    public void testActionRequest() throws Exception {
        MockActionRequest request = new MockActionRequest();
        MockActionResponse response = new MockActionResponse();
        request.setParameter("test", "test");
        controller.handleActionRequest(request, response);
        String result = response.getRenderParameter(RESULT_RENDER_PARAMETER_NAME);
        assertEquals("myPortlet-action", result);
    }

    public void testRenderRequest() throws Exception {
        MockRenderRequest request = new MockRenderRequest();
        MockRenderResponse response = new MockRenderResponse();
        controller.handleRenderRequest(request, response);
        String result = response.getContentAsString();
        assertEquals(RENDERED_RESPONSE_CONTENT, result);
    }

    public void testActionRequestWithNoParameters() throws Exception {
        final MockActionRequest request = new MockActionRequest();
        final MockActionResponse response = new MockActionResponse();
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                controller.handleActionRequest(request, response);
            }
        }.runTest();
    }

    public void testRejectsPortletClassThatDoesNotImplementPortletInterface() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                PortletWrappingController controller = new PortletWrappingController();
                controller.setPortletClass(String.class);
                controller.afterPropertiesSet();
            }
        }.runTest();
    }

    public void testRejectsIfPortletClassIsNotSupplied() throws Exception {
        new AssertThrows(IllegalArgumentException.class) {
            public void test() throws Exception {
                PortletWrappingController controller = new PortletWrappingController();
                controller.setPortletClass(null);
                controller.afterPropertiesSet();
            }
        }.runTest();
    }

    public void testDestroyingTheControllerPropagatesDestroyToWrappedPortlet() throws Exception {
        final PortletWrappingController controller = new PortletWrappingController();
        controller.setPortletClass(MyPortlet.class);
        controller.afterPropertiesSet();
        // test for destroy() call being propagated via exception being thrown :(
        new AssertThrows(IllegalStateException.class) {
            public void test() throws Exception {
                controller.destroy();
            }
        }.runTest();
    }

    public void testPortletName() throws Exception {
        MockActionRequest request = new MockActionRequest();
        MockActionResponse response = new MockActionResponse();
        request.setParameter(PORTLET_NAME_ACTION_REQUEST_PARAMETER_NAME, "test");
        controller.handleActionRequest(request, response);
        String result = response.getRenderParameter(RESULT_RENDER_PARAMETER_NAME);
        assertEquals("wrappedPortlet", result);
    }

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


        public void init(PortletConfig portletConfig) {
            this.portletConfig = portletConfig;
        }

        public void processAction(ActionRequest request, ActionResponse response) throws PortletException {
            if (request.getParameter("test") != null) {
                response.setRenderParameter(RESULT_RENDER_PARAMETER_NAME, "myPortlet-action");
            } else if (request.getParameter(PORTLET_NAME_ACTION_REQUEST_PARAMETER_NAME) != null) {
                response.setRenderParameter(RESULT_RENDER_PARAMETER_NAME, getPortletConfig().getPortletName());
            } else {
                throw new IllegalArgumentException("no request parameters");
            }
        }

        public void render(RenderRequest request, RenderResponse response) throws IOException {
            response.getWriter().write(RENDERED_RESPONSE_CONTENT);
        }

        public PortletConfig getPortletConfig() {
            return this.portletConfig;
        }

        public void destroy() {
            throw new IllegalStateException("Being destroyed...");
        }

    }

    private static final class MyApplicationContext extends StaticPortletApplicationContext {

        public void refresh() throws BeansException {
            MutablePropertyValues pvs = new MutablePropertyValues();
            pvs.addPropertyValue("portletClass", MyPortlet.class);
            registerSingleton(PORTLET_WRAPPING_CONTROLLER_BEAN_NAME, PortletWrappingController.class, pvs);
            super.refresh();
        }
    }

}
