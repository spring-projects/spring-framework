/*
* Copyright 2002-2012 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.springframework.web.portlet;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletSecurityException;
import javax.portlet.PortletSession;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockEvent;
import org.springframework.mock.web.portlet.MockEventRequest;
import org.springframework.mock.web.portlet.MockEventResponse;
import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.mock.web.portlet.MockPortletSession;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.portlet.context.PortletApplicationContextUtils;
import org.springframework.web.portlet.context.PortletConfigAwareBean;
import org.springframework.web.portlet.context.PortletContextAwareBean;
import org.springframework.web.portlet.context.PortletRequestAttributes;
import org.springframework.web.portlet.handler.PortletSessionRequiredException;
import org.springframework.web.portlet.multipart.MultipartActionRequest;
import org.springframework.web.portlet.multipart.PortletMultipartResolver;
import org.springframework.web.servlet.ViewRendererServlet;
import org.springframework.web.servlet.view.InternalResourceView;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Dan McCallum
 */
public class DispatcherPortletTests extends TestCase {

	private MockPortletConfig simplePortletConfig;

	private MockPortletConfig complexPortletConfig;

	private DispatcherPortlet simpleDispatcherPortlet;

	private DispatcherPortlet complexDispatcherPortlet;


	protected void setUp() throws PortletException {
		simplePortletConfig = new MockPortletConfig(new MockPortletContext(), "simple");
		complexPortletConfig = new MockPortletConfig(simplePortletConfig.getPortletContext(), "complex");
		complexPortletConfig.addInitParameter("publishContext", "false");

		simpleDispatcherPortlet = new DispatcherPortlet();
		simpleDispatcherPortlet.setContextClass(SimplePortletApplicationContext.class);
		simpleDispatcherPortlet.init(simplePortletConfig);

		complexDispatcherPortlet = new DispatcherPortlet();
		complexDispatcherPortlet.setContextClass(ComplexPortletApplicationContext.class);
		complexDispatcherPortlet.setNamespace("test");
		complexDispatcherPortlet.addRequiredProperty("publishContext");
		complexDispatcherPortlet.init(complexPortletConfig);
	}

	private PortletContext getPortletContext() {
		return complexPortletConfig.getPortletContext();
	}


	public void testDispatcherPortletGetPortletNameDoesNotFailWithoutConfig() {
		DispatcherPortlet dp = new DispatcherPortlet();
		assertEquals(null, dp.getPortletConfig());
		assertEquals(null, dp.getPortletName());
		assertEquals(null, dp.getPortletContext());
	}

	public void testDispatcherPortlets() {
		assertTrue("Correct namespace",
				("simple" + FrameworkPortlet.DEFAULT_NAMESPACE_SUFFIX).equals(simpleDispatcherPortlet.getNamespace()));
		assertTrue("Correct attribute",
				(FrameworkPortlet.PORTLET_CONTEXT_PREFIX + "simple").equals(simpleDispatcherPortlet.getPortletContextAttributeName()));
		assertTrue("Context published",
				simpleDispatcherPortlet.getPortletApplicationContext() ==
						getPortletContext().getAttribute(FrameworkPortlet.PORTLET_CONTEXT_PREFIX + "simple"));

		assertTrue("Correct namespace", "test".equals(complexDispatcherPortlet.getNamespace()));
		assertTrue("Correct attribute",
				(FrameworkPortlet.PORTLET_CONTEXT_PREFIX + "complex").equals(complexDispatcherPortlet.getPortletContextAttributeName()));
		assertTrue("Context not published",
				getPortletContext().getAttribute(FrameworkPortlet.PORTLET_CONTEXT_PREFIX + "complex") == null);
	}

	public void testSimpleValidActionRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setParameter("action", "form");
		request.setParameter("age", "29");
		simpleDispatcherPortlet.processAction(request, response);
		String exceptionParam = response.getRenderParameter(DispatcherPortlet.ACTION_EXCEPTION_RENDER_PARAMETER);
		assertNull(exceptionParam);
		SimplePortletApplicationContext ac = (SimplePortletApplicationContext)simpleDispatcherPortlet.getPortletApplicationContext();
		String commandAttribute = ac.getRenderCommandSessionAttributeName();
		TestBean testBean = (TestBean) request.getPortletSession().getAttribute(commandAttribute);
		assertEquals(39, testBean.getAge());
	}

	public void testSimpleInvalidActionRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setParameter("action", "invalid");
		simpleDispatcherPortlet.processAction(request, response);
		String exceptionParam = response.getRenderParameter(DispatcherPortlet.ACTION_EXCEPTION_RENDER_PARAMETER);
		assertNotNull(exceptionParam);
		assertTrue(exceptionParam.startsWith(NoHandlerFoundException.class.getName()));
	}


	public void testSimpleInvalidActionRequestWithoutHandling() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setParameter("action", "invalid");
		simpleDispatcherPortlet.setForwardActionException(false);
		try {
			simpleDispatcherPortlet.processAction(request, response);
			fail("Should have thrown a " + NoHandlerFoundException.class);
		}
		catch (NoHandlerFoundException ex) {
			// expected
		}
	}

	public void testSimpleValidEventRequest() throws Exception {
		MockEvent event = new MockEvent("test-event");
		MockEventRequest request = new MockEventRequest(event);
		MockEventResponse response = new MockEventResponse();
		request.setParameter("action", "form");
		simpleDispatcherPortlet.processEvent(request, response);
		assertEquals("test-event", response.getRenderParameter("event"));
	}

	public void testSimpleInvalidEventRequest() throws Exception {
		MockEvent event = new MockEvent("test-event");
		MockEventRequest request = new MockEventRequest(event);
		MockEventResponse response = new MockEventResponse();
		request.setParameter("action", "invalid");
		try {
			simpleDispatcherPortlet.processEvent(request, response);
			fail("Should have thrown a " + NoHandlerFoundException.class);
		}
		catch (NoHandlerFoundException ex) {
			// expected
		}
	}

	public void testSimpleInvalidEventRequestWithHandling() throws Exception {
		MockEvent event = new MockEvent("event");
		MockEventRequest request = new MockEventRequest(event);
		MockEventResponse response = new MockEventResponse();
		request.setParameter("action", "invalid");
		simpleDispatcherPortlet.setForwardEventException(true);
		simpleDispatcherPortlet.processEvent(request, response);
		String exceptionParam = response.getRenderParameter(DispatcherPortlet.ACTION_EXCEPTION_RENDER_PARAMETER);
		assertNotNull(exceptionParam);
		assertTrue(exceptionParam.startsWith(NoHandlerFoundException.class.getName()));
	}

	public void testSimpleFormViewNoBindOnNewForm() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("action", "form");
		request.setParameter("age", "29");
		simpleDispatcherPortlet.doDispatch(request, response);
		assertEquals("5", response.getContentAsString());
	}

	public void testSimpleFormViewBindOnNewForm() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("action", "form-bind");
		request.setParameter("age", "29");
		simpleDispatcherPortlet.doDispatch(request, response);
		assertEquals("34", response.getContentAsString());
	}

	public void testSimpleFormViewWithSessionAndBindOnNewForm() throws Exception {
		MockRenderRequest renderRequest = new MockRenderRequest();
		MockRenderResponse renderResponse = new MockRenderResponse();
		renderRequest.setParameter("action", "form-session-bind");
		renderRequest.setParameter("age", "30");
		TestBean testBean = new TestBean();
		testBean.setAge(40);
		SimplePortletApplicationContext ac =
				(SimplePortletApplicationContext)simpleDispatcherPortlet.getPortletApplicationContext();
		String formAttribute = ac.getFormSessionAttributeName();
		PortletSession session = new MockPortletSession();
		session.setAttribute(formAttribute, testBean);
		renderRequest.setSession(session);
		simpleDispatcherPortlet.doDispatch(renderRequest, renderResponse);
		assertEquals("35", renderResponse.getContentAsString());
	}

	public void testSimpleFormViewWithSessionNoBindOnNewForm() throws Exception {
		MockActionRequest actionRequest = new MockActionRequest();
		MockActionResponse actionResponse = new MockActionResponse();
		actionRequest.setSession(new MockPortletSession());
		actionRequest.setParameter("action", "form-session-nobind");
		actionRequest.setParameter("age", "27");
		simpleDispatcherPortlet.processAction(actionRequest, actionResponse);
		Map renderParameters = actionResponse.getRenderParameterMap();

		MockRenderRequest renderRequest = new MockRenderRequest();
		MockRenderResponse renderResponse = new MockRenderResponse();
		renderRequest.setParameters(renderParameters);
		renderRequest.setParameter("action", "form-session-nobind");
		renderRequest.setParameter("age", "30");
		renderRequest.setSession(actionRequest.getPortletSession());
		simpleDispatcherPortlet.doDispatch(renderRequest, renderResponse);
		assertEquals("finished42", renderResponse.getContentAsString());
	}

	public void testSimpleRequiredSessionFormWithoutSession() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("action", "form-session-bind");
		try {
			simpleDispatcherPortlet.doDispatch(request, response);
			fail("Should have thrown PortletSessionRequiredException");
		}
		catch (PortletSessionRequiredException ex) {
// expected
		}
	}

	public void testSimpleFormSubmission() throws Exception {
		MockActionRequest actionRequest = new MockActionRequest();
		MockActionResponse actionResponse = new MockActionResponse();
		actionRequest.setParameter("action", "form");
		actionRequest.setParameter("age", "29");
		simpleDispatcherPortlet.processAction(actionRequest, actionResponse);

		MockRenderRequest renderRequest = new MockRenderRequest();
		MockRenderResponse renderResponse = new MockRenderResponse();
		renderRequest.setSession(actionRequest.getPortletSession());
		renderRequest.setParameters(actionResponse.getRenderParameterMap());
		renderRequest.setParameter("action", "form");
		simpleDispatcherPortlet.doDispatch(renderRequest, renderResponse);
		assertEquals("finished44", renderResponse.getContentAsString());
	}

	public void testSimpleFormSubmissionWithValidationError() throws Exception {
		MockActionRequest actionRequest = new MockActionRequest();
		MockActionResponse actionResponse = new MockActionResponse();
		actionRequest.setParameter("action", "form");
		actionRequest.setParameter("age", "XX");
		simpleDispatcherPortlet.processAction(actionRequest, actionResponse);

		MockRenderRequest renderRequest = new MockRenderRequest();
		MockRenderResponse renderResponse = new MockRenderResponse();
		renderRequest.setSession(actionRequest.getPortletSession());
		renderRequest.setParameters(actionResponse.getRenderParameterMap());
		renderRequest.setParameter("action", "form");
		simpleDispatcherPortlet.doDispatch(renderRequest, renderResponse);
		assertEquals("5", renderResponse.getContentAsString());
	}

	public void testSimpleInvalidRenderRequest() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("action", "invalid");
		try {
			simpleDispatcherPortlet.doDispatch(request, response);
			fail("Should have thrown UnavailableException");
		}
		catch (NoHandlerFoundException ex) {
// expected
		}
	}

	public void testPortletModeParameterMappingHelp1() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.HELP);
		request.setParameter("action", "help1");
		complexDispatcherPortlet.processAction(request, response);
		String param = response.getRenderParameter("param");
		assertEquals("help1 was here", param);
	}

	public void testPortletModeParameterMappingHelp2() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.HELP);
		request.setParameter("action", "help2");
		complexDispatcherPortlet.processAction(request, response);
		String param = response.getRenderParameter("param");
		assertEquals("help2 was here", param);
	}

	public void testPortletModeParameterMappingInvalidHelpActionRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.HELP);
		request.setParameter("action", "help3");
		complexDispatcherPortlet.processAction(request, response);
		String exceptionParam = response.getRenderParameter(DispatcherPortlet.ACTION_EXCEPTION_RENDER_PARAMETER);
		assertNotNull(exceptionParam);
		assertTrue(exceptionParam.startsWith(NoHandlerFoundException.class.getName()));
	}

	public void testPortletModeParameterMappingInvalidHelpRenderRequest() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.HELP);
		request.setParameter("action", "help3");
		complexDispatcherPortlet.doDispatch(request, response);
		Map model = (Map) request.getAttribute(ViewRendererServlet.MODEL_ATTRIBUTE);
		assertTrue(model.get("exception").getClass().equals(NoHandlerFoundException.class));
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-unavailable", view.getBeanName());
	}

	public void testPortletModeMappingValidEditActionRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.EDIT);
		request.addUserRole("role1");
		request.setParameter("action", "not mapped");
		request.setParameter("myParam", "not mapped");
		complexDispatcherPortlet.processAction(request, response);
		assertEquals("edit was here", response.getRenderParameter("param"));
	}

	public void testPortletModeMappingEditActionRequestWithUnauthorizedUserRole() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.EDIT);
		request.addUserRole("role3");
		request.setParameter("action", "not mapped");
		request.setParameter("myParam", "not mapped");
		complexDispatcherPortlet.processAction(request, response);
		String exception = response.getRenderParameter(DispatcherPortlet.ACTION_EXCEPTION_RENDER_PARAMETER);
		assertNotNull(exception);
		String name = PortletSecurityException.class.getName();
		assertTrue(exception.startsWith(name));
	}

	public void testPortletModeMappingValidViewRenderRequest() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.VIEW);
		request.addUserRole("role2");
		request.setParameter("action", "not mapped");
		request.setParameter("myParam", "not mapped");
		complexDispatcherPortlet.doDispatch(request, response);
		Map model = (Map) request.getAttribute(ViewRendererServlet.MODEL_ATTRIBUTE);
		assertEquals("view was here", model.get("result"));
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("someViewName", view.getBeanName());
	}

	public void testPortletModeMappingViewRenderRequestWithUnauthorizedUserRole() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.VIEW);
		request.addUserRole("role3");
		request.setParameter("action", "not mapped");
		request.setParameter("myParam", "not mapped");
		complexDispatcherPortlet.doDispatch(request, response);
		Map model = (Map) request.getAttribute(ViewRendererServlet.MODEL_ATTRIBUTE);
		Exception exception = (Exception) model.get("exception");
		assertNotNull(exception);
		assertTrue(exception.getClass().equals(PortletSecurityException.class));
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-default-1", view.getBeanName());
	}

	public void testParameterMappingValidActionRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.EDIT);
		request.setParameter("action", "not mapped");
		request.setParameter("myParam", "test1");
		complexDispatcherPortlet.processAction(request, response);
		assertEquals("test1-action", response.getRenderParameter("result"));
	}

	public void testParameterMappingValidRenderRequest() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.VIEW);
		request.setParameter("action", "not mapped");
		request.setParameter("myParam", "test2");
		complexDispatcherPortlet.doDispatch(request, response);
		assertEquals("test2-view", response.getProperty("result"));
	}

	public void testUnknownHandlerActionRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setParameter("myParam", "unknown");
		complexDispatcherPortlet.processAction(request, response);
		String exceptionParam = response.getRenderParameter(DispatcherPortlet.ACTION_EXCEPTION_RENDER_PARAMETER);
		assertNotNull(exceptionParam);
		assertTrue(exceptionParam.startsWith(PortletException.class.getName()));
		assertTrue(exceptionParam.indexOf("No adapter for handler") != -1);
	}

	public void testUnknownHandlerRenderRequest() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("myParam", "unknown");
		complexDispatcherPortlet.doDispatch(request, response);
		Map model = (Map) request.getAttribute(ViewRendererServlet.MODEL_ATTRIBUTE);
		Exception exception = (Exception)model.get("exception");
		assertTrue(exception.getClass().equals(PortletException.class));
		assertTrue(exception.getMessage().indexOf("No adapter for handler") != -1);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-default-1", view.getBeanName());
	}

	public void testNoDetectAllHandlerMappingsWithPortletModeActionRequest() throws Exception {
		DispatcherPortlet complexDispatcherPortlet = new DispatcherPortlet();
		complexDispatcherPortlet.setContextClass(ComplexPortletApplicationContext.class);
		complexDispatcherPortlet.setNamespace("test");
		complexDispatcherPortlet.setDetectAllHandlerMappings(false);
		complexDispatcherPortlet.init(new MockPortletConfig(getPortletContext(), "complex"));
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.EDIT);
		complexDispatcherPortlet.processAction(request, response);
		String exceptionParam = response.getRenderParameter(DispatcherPortlet.ACTION_EXCEPTION_RENDER_PARAMETER);
		assertNotNull(exceptionParam);
		assertTrue(exceptionParam.startsWith(NoHandlerFoundException.class.getName()));
	}

	public void testNoDetectAllHandlerMappingsWithParameterRenderRequest() throws Exception {
		DispatcherPortlet complexDispatcherPortlet = new DispatcherPortlet();
		complexDispatcherPortlet.setContextClass(ComplexPortletApplicationContext.class);
		complexDispatcherPortlet.setNamespace("test");
		complexDispatcherPortlet.setDetectAllHandlerMappings(false);
		complexDispatcherPortlet.init(new MockPortletConfig(getPortletContext(), "complex"));
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("myParam", "test1");
		complexDispatcherPortlet.doDispatch(request, response);
		Map model = (Map) request.getAttribute(ViewRendererServlet.MODEL_ATTRIBUTE);
		Exception exception = (Exception) model.get("exception");
		assertTrue(exception.getClass().equals(NoHandlerFoundException.class));
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-unavailable", view.getBeanName());
	}

	public void testExistingMultipartRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.EDIT);
		ComplexPortletApplicationContext.MockMultipartResolver multipartResolver =
				(ComplexPortletApplicationContext.MockMultipartResolver)
						complexDispatcherPortlet.getPortletApplicationContext().getBean("portletMultipartResolver");
		MultipartActionRequest multipartRequest = multipartResolver.resolveMultipart(request);
		complexDispatcherPortlet.processAction(multipartRequest, response);
		multipartResolver.cleanupMultipart(multipartRequest);
		assertNotNull(request.getAttribute("cleanedUp"));
	}

	public void testMultipartResolutionFailed() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.EDIT);
		request.addUserRole("role1");
		request.setAttribute("fail", Boolean.TRUE);
		complexDispatcherPortlet.processAction(request, response);
		String exception = response.getRenderParameter(DispatcherPortlet.ACTION_EXCEPTION_RENDER_PARAMETER);
		assertTrue(exception.startsWith(MaxUploadSizeExceededException.class.getName()));
	}

	public void testActionRequestHandledEvent() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		complexDispatcherPortlet.processAction(request, response);
		ComplexPortletApplicationContext.TestApplicationListener listener =
				(ComplexPortletApplicationContext.TestApplicationListener)
						complexDispatcherPortlet.getPortletApplicationContext().getBean("testListener");
		assertEquals(1, listener.counter);
	}

	public void testRenderRequestHandledEvent() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		ComplexPortletApplicationContext.TestApplicationListener listener =
				(ComplexPortletApplicationContext.TestApplicationListener)
						complexDispatcherPortlet.getPortletApplicationContext().getBean("testListener");
		assertEquals(1, listener.counter);
	}

	public void testPublishEventsOff() throws Exception {
		complexDispatcherPortlet.setPublishEvents(false);
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setParameter("action", "checker");
		complexDispatcherPortlet.processAction(request, response);
		ComplexPortletApplicationContext.TestApplicationListener listener =
				(ComplexPortletApplicationContext.TestApplicationListener)
						complexDispatcherPortlet.getPortletApplicationContext().getBean("testListener");
		assertEquals(0, listener.counter);
	}

	public void testCorrectLocaleInRequest() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("myParam", "requestLocaleChecker");
		request.addPreferredLocale(Locale.CANADA);
		complexDispatcherPortlet.doDispatch(request, response);
		assertEquals("locale-ok", response.getContentAsString());
	}

	public void testIncorrectLocaleInRequest() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("myParam", "requestLocaleChecker");
		request.addPreferredLocale(Locale.ENGLISH);
		complexDispatcherPortlet.doDispatch(request, response);
		Map model = (Map) request.getAttribute(ViewRendererServlet.MODEL_ATTRIBUTE);
		Exception exception = (Exception) model.get("exception");
		assertTrue(exception.getClass().equals(PortletException.class));
		assertEquals("Incorrect Locale in RenderRequest", exception.getMessage());
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-default-1", view.getBeanName());
	}

	public void testCorrectLocaleInLocaleContextHolder() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("myParam", "contextLocaleChecker");
		request.addPreferredLocale(Locale.CANADA);
		complexDispatcherPortlet.doDispatch(request, response);
		assertEquals("locale-ok", response.getContentAsString());
	}

	public void testIncorrectLocaleInLocalContextHolder() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("myParam", "contextLocaleChecker");
		request.addPreferredLocale(Locale.ENGLISH);
		complexDispatcherPortlet.doDispatch(request, response);
		Map model = (Map) request.getAttribute(ViewRendererServlet.MODEL_ATTRIBUTE);
		Exception exception = (Exception) model.get("exception");
		assertTrue(exception.getClass().equals(PortletException.class));
		assertEquals("Incorrect Locale in LocaleContextHolder", exception.getMessage());
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-default-1", view.getBeanName());
	}

	public void testHandlerInterceptorNoAbort() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.VIEW);
		request.addUserRole("role1");
		request.addParameter("abort", "false");
		complexDispatcherPortlet.doDispatch(request, response);
		assertTrue(request.getAttribute("test1-remove-never") != null);
		assertTrue(request.getAttribute("test1-remove-post") == null);
		assertTrue(request.getAttribute("test1-remove-after") == null);
		assertTrue(request.getAttribute("test2-remove-never") != null);
		assertTrue(request.getAttribute("test2-remove-post") == null);
		assertTrue(request.getAttribute("test2-remove-after") == null);
	}

	public void testHandlerInterceptorAbort() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.VIEW);
		request.addUserRole("role1");
		request.addParameter("abort", "true");
		complexDispatcherPortlet.doDispatch(request, response);
		assertTrue(request.getAttribute("test1-remove-never") != null);
		assertTrue(request.getAttribute("test1-remove-post") != null);
		assertTrue(request.getAttribute("test1-remove-after") == null);
		assertTrue(request.getAttribute("test2-remove-never") == null);
		assertTrue(request.getAttribute("test2-remove-post") == null);
		assertTrue(request.getAttribute("test2-remove-after") == null);
	}

	public void testHandlerInterceptorNotClearingModelAndView() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.VIEW);
		request.addUserRole("role1");
		request.addParameter("noView", "false");
		complexDispatcherPortlet.doDispatch(request, response);
		Map model = (Map) request.getAttribute(ViewRendererServlet.MODEL_ATTRIBUTE);
		assertEquals("view was here", model.get("result"));
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("someViewName", view.getBeanName());
	}

	public void testHandlerInterceptorClearingModelAndView() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setPortletMode(PortletMode.VIEW);
		request.addUserRole("role1");
		request.addParameter("noView", "true");
		complexDispatcherPortlet.doDispatch(request, response);
		Map model = (Map) request.getAttribute(ViewRendererServlet.MODEL_ATTRIBUTE);
		assertNull(model);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertNull(view);
	}

	public void testParameterMappingInterceptorWithCorrectParam() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.VIEW);
		request.addUserRole("role1");
		request.addParameter("interceptingParam", "test1");
		complexDispatcherPortlet.processAction(request, response);
		assertEquals("test1", response.getRenderParameter("interceptingParam"));
	}

	public void testParameterMappingInterceptorWithIncorrectParam() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setPortletMode(PortletMode.VIEW);
		request.addUserRole("role1");
		request.addParameter("incorrect", "test1");
		complexDispatcherPortlet.processAction(request, response);
		assertNull(response.getRenderParameter("incorrect"));
		assertNull(response.getRenderParameter("interceptingParam"));
	}

	public void testPortletHandlerAdapterActionRequest() throws Exception {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.setParameter("myParam", "myPortlet");
		complexDispatcherPortlet.processAction(request, response);
		assertEquals("myPortlet action called", response.getRenderParameter("result"));
		ComplexPortletApplicationContext.MyPortlet myPortlet =
				(ComplexPortletApplicationContext.MyPortlet) complexDispatcherPortlet.getPortletApplicationContext().getBean("myPortlet");
		assertEquals("complex", myPortlet.getPortletConfig().getPortletName());
		assertEquals(getPortletContext(), myPortlet.getPortletConfig().getPortletContext());
		assertEquals(complexDispatcherPortlet.getPortletContext(), myPortlet.getPortletConfig().getPortletContext());
		complexDispatcherPortlet.destroy();
		assertNull(myPortlet.getPortletConfig());
	}

	public void testPortletHandlerAdapterRenderRequest() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.setParameter("myParam", "myPortlet");
		complexDispatcherPortlet.doDispatch(request, response);
		assertEquals("myPortlet was here", response.getContentAsString());
		ComplexPortletApplicationContext.MyPortlet myPortlet =
				(ComplexPortletApplicationContext.MyPortlet)
						complexDispatcherPortlet.getPortletApplicationContext().getBean("myPortlet");
		assertEquals("complex", myPortlet.getPortletConfig().getPortletName());
		assertEquals(getPortletContext(), myPortlet.getPortletConfig().getPortletContext());
		assertEquals(complexDispatcherPortlet.getPortletContext(),
				myPortlet.getPortletConfig().getPortletContext());
		complexDispatcherPortlet.destroy();
		assertNull(myPortlet.getPortletConfig());
	}

	public void testModelAndViewDefiningExceptionInMappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception1");
		request.addParameter("fail", "yes");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-modelandview", view.getBeanName());
	}

	public void testModelAndViewDefiningExceptionInUnmappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception2");
		request.addParameter("fail", "yes");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-modelandview", view.getBeanName());
	}

	public void testIllegalAccessExceptionInMappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception1");
		request.addParameter("access", "illegal");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-exception", view.getBeanName());
	}

	public void testIllegalAccessExceptionInUnmappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception2");
		request.addParameter("access", "illegal");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-illegalaccess", view.getBeanName());
	}

	public void testPortletRequestBindingExceptionInMappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception1");
		request.addParameter("binding", "should fail");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-exception", view.getBeanName());
	}

	public void testPortletRequestBindingExceptionInUnmappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception2");
		request.addParameter("binding", "should fail");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-binding", view.getBeanName());
	}

	public void testIllegalArgumentExceptionInMappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception1");
		request.addParameter("unknown", "");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-runtime", view.getBeanName());
	}

	public void testIllegalArgumentExceptionInUnmappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception2");
		request.addParameter("unknown", "");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-default-1", view.getBeanName());
	}

	public void testExceptionInMappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception1");
		request.addParameter("generic", "123");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-exception", view.getBeanName());
	}

	public void testExceptionInUnmappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception2");
		request.addParameter("generic", "123");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-default-1", view.getBeanName());
	}

	public void testRuntimeExceptionInMappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception1");
		request.addParameter("runtime", "true");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-runtime", view.getBeanName());
	}

	public void testRuntimeExceptionInUnmappedHandler() throws Exception {
		MockRenderRequest request = new MockRenderRequest();
		request.setPortletMode(PortletMode.HELP);
		request.addParameter("myParam", "exception2");
		request.addParameter("runtime", "true");
		MockRenderResponse response = new MockRenderResponse();
		complexDispatcherPortlet.doDispatch(request, response);
		InternalResourceView view = (InternalResourceView) request.getAttribute(ViewRendererServlet.VIEW_ATTRIBUTE);
		assertEquals("failed-default-1", view.getBeanName());
	}

	public void testGetMessage() {
		String message = complexDispatcherPortlet.getPortletApplicationContext().getMessage("test", null, Locale.ENGLISH);
		assertEquals("test message", message);
	}

	public void testGetMessageOtherLocale() {
		String message = complexDispatcherPortlet.getPortletApplicationContext().getMessage("test", null, Locale.CANADA);
		assertEquals("Canadian & test message", message);
	}

	public void testGetMessageWithArgs() {
		Object[] args = new String[] {"this", "that"};
		String message = complexDispatcherPortlet.getPortletApplicationContext().getMessage("test.args", args, Locale.ENGLISH);
		assertEquals("test this and that", message);
	}

	public void testPortletApplicationContextLookup() {
		MockPortletContext portletContext = new MockPortletContext();
		ApplicationContext ac = PortletApplicationContextUtils.getWebApplicationContext(portletContext);
		assertNull(ac);
		try {
			ac = PortletApplicationContextUtils.getRequiredWebApplicationContext(portletContext);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
// expected
		}
		portletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				new StaticWebApplicationContext());
		try {
			ac = PortletApplicationContextUtils.getRequiredWebApplicationContext(portletContext);
			assertNotNull(ac);
		}
		catch (IllegalStateException ex) {
			fail("Should not have thrown IllegalStateException: " + ex.getMessage());
		}
	}

	public void testValidActionRequestWithExistingThreadLocalRequestContext() throws IOException, PortletException {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.addPreferredLocale(Locale.GERMAN);
		request.setParameter("action", "form");
		request.setParameter("age", "29");

// see RequestContextListener.requestInitialized()
		try {
			LocaleContextHolder.setLocale(request.getLocale());
			RequestContextHolder.setRequestAttributes(new PortletRequestAttributes(request));

			LocaleContext servletLocaleContext = LocaleContextHolder.getLocaleContext();
			RequestAttributes servletRequestAttrs = RequestContextHolder.getRequestAttributes();

			simpleDispatcherPortlet.processAction(request, response);

			assertSame(servletLocaleContext, LocaleContextHolder.getLocaleContext());
			assertSame(servletRequestAttrs, RequestContextHolder.getRequestAttributes());
		}
		finally {
			RequestContextHolder.resetRequestAttributes();
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testValidRenderRequestWithExistingThreadLocalRequestContext() throws IOException, PortletException {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.addPreferredLocale(Locale.GERMAN);

// see RequestContextListener.requestInitialized()
		try {
			LocaleContextHolder.setLocale(request.getLocale());
			RequestContextHolder.setRequestAttributes(new PortletRequestAttributes(request));

			LocaleContext servletLocaleContext = LocaleContextHolder.getLocaleContext();
			RequestAttributes servletRequestAttrs = RequestContextHolder.getRequestAttributes();

			request.setParameter("action", "form");
			request.setParameter("age", "29");
			simpleDispatcherPortlet.doDispatch(request, response);

			assertSame(servletLocaleContext, LocaleContextHolder.getLocaleContext());
			assertSame(servletRequestAttrs, RequestContextHolder.getRequestAttributes());
		}
		finally {
			RequestContextHolder.resetRequestAttributes();
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testInvalidActionRequestWithExistingThreadLocalRequestContext() throws IOException, PortletException {
		MockActionRequest request = new MockActionRequest();
		MockActionResponse response = new MockActionResponse();
		request.addPreferredLocale(Locale.GERMAN);

// see RequestContextListener.requestInitialized()
		try {
			LocaleContextHolder.setLocale(request.getLocale());
			RequestContextHolder.setRequestAttributes(new PortletRequestAttributes(request));

			LocaleContext servletLocaleContext = LocaleContextHolder.getLocaleContext();
			RequestAttributes servletRequestAttrs = RequestContextHolder.getRequestAttributes();

			request.setParameter("action", "invalid");
			simpleDispatcherPortlet.processAction(request, response);
			String exceptionParam = response.getRenderParameter(DispatcherPortlet.ACTION_EXCEPTION_RENDER_PARAMETER);
			assertNotNull(exceptionParam); // ensure that an exceptional condition occured

			assertSame(servletLocaleContext, LocaleContextHolder.getLocaleContext());
			assertSame(servletRequestAttrs, RequestContextHolder.getRequestAttributes());
		}
		finally {
			RequestContextHolder.resetRequestAttributes();
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testInvalidRenderRequestWithExistingThreadLocalRequestContext() throws IOException, PortletException {
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		request.addPreferredLocale(Locale.GERMAN);

// see RequestContextListener.requestInitialized()
		try {
			LocaleContextHolder.setLocale(request.getLocale());
			RequestContextHolder.setRequestAttributes(new PortletRequestAttributes(request));

			LocaleContext servletLocaleContext = LocaleContextHolder.getLocaleContext();
			RequestAttributes servletRequestAttrs = RequestContextHolder.getRequestAttributes();

			try {
				simpleDispatcherPortlet.doDispatch(request, response);
				fail("should have failed to find a handler and raised an NoHandlerFoundExceptionException");
			}
			catch (NoHandlerFoundException ex) {
// expected
			}

			assertSame(servletLocaleContext, LocaleContextHolder.getLocaleContext());
			assertSame(servletRequestAttrs, RequestContextHolder.getRequestAttributes());
		}
		finally {
			RequestContextHolder.resetRequestAttributes();
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testDispatcherPortletRefresh() throws PortletException {
		MockPortletContext portletContext = new MockPortletContext("org/springframework/web/portlet/context");
		DispatcherPortlet portlet = new DispatcherPortlet();

		portlet.init(new MockPortletConfig(portletContext, "empty"));
		PortletContextAwareBean contextBean = (PortletContextAwareBean)
				portlet.getPortletApplicationContext().getBean("portletContextAwareBean");
		PortletConfigAwareBean configBean = (PortletConfigAwareBean)
				portlet.getPortletApplicationContext().getBean("portletConfigAwareBean");
		assertSame(portletContext, contextBean.getPortletContext());
		assertSame(portlet.getPortletConfig(), configBean.getPortletConfig());
		PortletMultipartResolver multipartResolver = portlet.getMultipartResolver();
		assertNotNull(multipartResolver);

		portlet.refresh();

		PortletContextAwareBean contextBean2 = (PortletContextAwareBean)
				portlet.getPortletApplicationContext().getBean("portletContextAwareBean");
		PortletConfigAwareBean configBean2 = (PortletConfigAwareBean)
				portlet.getPortletApplicationContext().getBean("portletConfigAwareBean");
		assertSame(portletContext, contextBean.getPortletContext());
		assertSame(portlet.getPortletConfig(), configBean.getPortletConfig());
		assertTrue(contextBean != contextBean2);
		assertTrue(configBean != configBean2);
		PortletMultipartResolver multipartResolver2 = portlet.getMultipartResolver();
		assertTrue(multipartResolver != multipartResolver2);

		portlet.destroy();
	}

	public void testDispatcherPortletContextRefresh() throws PortletException {
		MockPortletContext portletContext = new MockPortletContext("org/springframework/web/portlet/context");
		DispatcherPortlet portlet = new DispatcherPortlet();

		portlet.init(new MockPortletConfig(portletContext, "empty"));
		PortletContextAwareBean contextBean = (PortletContextAwareBean)
				portlet.getPortletApplicationContext().getBean("portletContextAwareBean");
		PortletConfigAwareBean configBean = (PortletConfigAwareBean)
				portlet.getPortletApplicationContext().getBean("portletConfigAwareBean");
		assertSame(portletContext, contextBean.getPortletContext());
		assertSame(portlet.getPortletConfig(), configBean.getPortletConfig());
		PortletMultipartResolver multipartResolver = portlet.getMultipartResolver();
		assertNotNull(multipartResolver);

		((ConfigurableApplicationContext) portlet.getPortletApplicationContext()).refresh();

		PortletContextAwareBean contextBean2 = (PortletContextAwareBean)
				portlet.getPortletApplicationContext().getBean("portletContextAwareBean");
		PortletConfigAwareBean configBean2 = (PortletConfigAwareBean)
				portlet.getPortletApplicationContext().getBean("portletConfigAwareBean");
		assertSame(portletContext, contextBean.getPortletContext());
		assertSame(portlet.getPortletConfig(), configBean.getPortletConfig());
		assertTrue(contextBean != contextBean2);
		assertTrue(configBean != configBean2);
		PortletMultipartResolver multipartResolver2 = portlet.getMultipartResolver();
		assertTrue(multipartResolver != multipartResolver2);

		portlet.destroy();
	}

}
