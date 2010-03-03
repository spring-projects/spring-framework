/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.servlet.view.velocity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.easymock.MockControl;
import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.context.ApplicationContextException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.view.AbstractView;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Dave Syer
 */
public class VelocityViewTests {

	@Test
	public void testNoVelocityConfig() throws Exception {
		VelocityView vv = new VelocityView();
		MockControl wmc = MockControl.createControl(WebApplicationContext.class);
		WebApplicationContext wac = (WebApplicationContext) wmc.getMock();
		wac.getBeansOfType(VelocityConfig.class, true, false);
		wmc.setReturnValue(new HashMap());
		wac.getParentBeanFactory();
		wmc.setReturnValue(null);
		wmc.replay();

		vv.setUrl("anythingButNull");
		try {
			vv.setApplicationContext(wac);
			fail();
		}
		catch (ApplicationContextException ex) {
			// Check there's a helpful error message
			assertTrue(ex.getMessage().contains("VelocityConfig"));
		}

		wmc.verify();
	}

	@Test
	public void testNoTemplateName() throws Exception {
		VelocityView vv = new VelocityView();
		try {
			vv.afterPropertiesSet();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// Check there's a helpful error message
			assertTrue(ex.getMessage().indexOf("url") != -1);
		}
	}

	@Test
	public void testMergeTemplateSucceeds() throws Exception {
		testValidTemplateName(null);
	}
	
	@Test
	public void testMergeTemplateFailureWithIOException() throws Exception {
		testValidTemplateName(new IOException());
	}
	
	@Test
	public void testMergeTemplateFailureWithParseErrorException() throws Exception {
		testValidTemplateName(new ParseErrorException(""));
	}
		
	@Test
	public void testMergeTemplateFailureWithUnspecifiedException() throws Exception {
		testValidTemplateName(new Exception(""));
	}

	@Test
	public void testMergeTemplateFailureWithMethodInvocationException() throws Exception {
		testValidTemplateName(new MethodInvocationException("Bad template", null, "none", "foo.vm", 1, 100));
	}

	/**
	 * @param mergeTemplateFailureException may be null in which case mergeTemplate override will succeed.
	 * If it's non null it will be checked
	 */
	private void testValidTemplateName(final Exception mergeTemplateFailureException) throws Exception {
		Map model = new HashMap();
		model.put("foo", "bar");

		final String templateName = "test.vm";

		MockControl wmc = MockControl.createControl(WebApplicationContext.class);
		WebApplicationContext wac = (WebApplicationContext) wmc.getMock();
		MockServletContext sc = new MockServletContext();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		final Template expectedTemplate = new Template();
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine(templateName, expectedTemplate);
			}
		};
		wac.getBeansOfType(VelocityConfig.class, true, false);
		Map configurers = new HashMap();
		configurers.put("velocityConfigurer", vc);
		wmc.setReturnValue(configurers);
		wac.getParentBeanFactory();
		wmc.setReturnValue(null);
		wac.getServletContext();
		wmc.setReturnValue(sc, 3);
		wmc.replay();

		HttpServletRequest request = new MockHttpServletRequest();
		final HttpServletResponse expectedResponse = new MockHttpServletResponse();

		VelocityView vv = new VelocityView() {
			protected void mergeTemplate(Template template, Context context, HttpServletResponse response) throws Exception {
				assertTrue(template == expectedTemplate);
				assertTrue(context.getKeys().length >= 1);
				assertTrue(context.get("foo").equals("bar"));
				assertTrue(response == expectedResponse);
				if (mergeTemplateFailureException != null) {
					throw mergeTemplateFailureException;
				}
			}
		};
		vv.setUrl(templateName);
		vv.setApplicationContext(wac);

		try {
			vv.render(model, request, expectedResponse);
			if (mergeTemplateFailureException != null) {
				fail();
			}
		}
		catch (Exception ex) {
			assertNotNull(mergeTemplateFailureException);
			assertEquals(ex, mergeTemplateFailureException);
		}

		wmc.verify();
	}

	@Test
	public void testKeepExistingContentType() throws Exception {
		final String templateName = "test.vm";

		MockControl wmc = MockControl.createControl(WebApplicationContext.class);
		WebApplicationContext wac = (WebApplicationContext) wmc.getMock();
		MockServletContext sc = new MockServletContext();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		final Template expectedTemplate = new Template();
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine(templateName, expectedTemplate);
			}
		};
		wac.getBeansOfType(VelocityConfig.class, true, false);
		Map configurers = new HashMap();
		configurers.put("velocityConfigurer", vc);
		wmc.setReturnValue(configurers);
		wac.getParentBeanFactory();
		wmc.setReturnValue(null);
		wac.getServletContext();
		wmc.setReturnValue(sc, 3);
		wmc.replay();

		HttpServletRequest request = new MockHttpServletRequest();
		final HttpServletResponse expectedResponse = new MockHttpServletResponse();
		expectedResponse.setContentType("myContentType");

		VelocityView vv = new VelocityView() {
			protected void mergeTemplate(Template template, Context context, HttpServletResponse response) {
				assertTrue(template == expectedTemplate);
				assertTrue(response == expectedResponse);
			}
			protected void exposeHelpers(Map model, HttpServletRequest request) throws Exception {
				model.put("myHelper", "myValue");
			}
		};

		vv.setUrl(templateName);
		vv.setApplicationContext(wac);
		vv.render(new HashMap(), request, expectedResponse);

		wmc.verify();
		assertEquals("myContentType", expectedResponse.getContentType());
	}

	@Test
	public void testExposeHelpers() throws Exception {
		final String templateName = "test.vm";

		MockControl wmc = MockControl.createControl(WebApplicationContext.class);
		WebApplicationContext wac = (WebApplicationContext) wmc.getMock();
		wac.getParentBeanFactory();
		wmc.setReturnValue(null);
		wac.getServletContext();
		wmc.setReturnValue(new MockServletContext());
		final Template expectedTemplate = new Template();
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine(templateName, expectedTemplate);
			}
		};
		wac.getBeansOfType(VelocityConfig.class, true, false);
		Map configurers = new HashMap();
		configurers.put("velocityConfigurer", vc);
		wmc.setReturnValue(configurers);
		wmc.replay();

		// let it ask for locale
		MockControl reqControl = MockControl.createControl(HttpServletRequest.class);
		HttpServletRequest req = (HttpServletRequest) reqControl.getMock();
		req.getAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE);
		reqControl.setReturnValue(new AcceptHeaderLocaleResolver());
		req.getLocale();
		reqControl.setReturnValue(Locale.CANADA);
		reqControl.replay();

		final HttpServletResponse expectedResponse = new MockHttpServletResponse();

		VelocityView vv = new VelocityView() {
			protected void mergeTemplate(Template template, Context context, HttpServletResponse response) throws Exception {
				assertTrue(template == expectedTemplate);
				assertTrue(response == expectedResponse);

				assertEquals("myValue", context.get("myHelper"));
				assertTrue(context.get("math") instanceof MathTool);

				assertTrue(context.get("dateTool") instanceof DateTool);
				DateTool dateTool = (DateTool) context.get("dateTool");
				assertTrue(dateTool.getLocale().equals(Locale.CANADA));

				assertTrue(context.get("numberTool") instanceof NumberTool);
				NumberTool numberTool = (NumberTool) context.get("numberTool");
				assertTrue(numberTool.getLocale().equals(Locale.CANADA));
			}

			protected void exposeHelpers(Map model, HttpServletRequest request) throws Exception {
				model.put("myHelper", "myValue");
			}
		};

		vv.setUrl(templateName);
		vv.setApplicationContext(wac);
		Map<String, Class> toolAttributes = new HashMap<String, Class>();
		toolAttributes.put("math", MathTool.class);
		vv.setToolAttributes(toolAttributes);
		vv.setDateToolAttribute("dateTool");
		vv.setNumberToolAttribute("numberTool");
		vv.setExposeSpringMacroHelpers(false);

		vv.render(new HashMap(), req, expectedResponse);

		wmc.verify();
		reqControl.verify();
		assertEquals(AbstractView.DEFAULT_CONTENT_TYPE, expectedResponse.getContentType());
	}

}
