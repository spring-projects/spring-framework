/*
 * Copyright 2002-2008 the original author or authors.
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

import junit.framework.TestCase;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.tools.LinkTool;
import org.easymock.MockControl;

import org.springframework.context.ApplicationContextException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class VelocityViewTests extends TestCase {

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
			assertTrue(ex.getMessage().indexOf("VelocityConfig") != -1);
		}

		wmc.verify();
	}

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

	public void testCannotResolveTemplateNameResourceNotFoundException() throws Exception {
		testCannotResolveTemplateName(new ResourceNotFoundException(""));
	}

	public void testCannotResolveTemplateNameParseErrorException() throws Exception {
		testCannotResolveTemplateName(new ParseErrorException(""));
	}

	public void testCannotResolveTemplateNameNonspecificException() throws Exception {
		testCannotResolveTemplateName(new Exception(""));
	}

	/**
	 * Check for failure to lookup a template for a range of reasons.
	 */
	private void testCannotResolveTemplateName(final Exception templateLookupException) throws Exception {
		final String templateName = "test.vm";

		MockControl wmc = MockControl.createControl(WebApplicationContext.class);
		WebApplicationContext wac = (WebApplicationContext) wmc.getMock();
		wac.getParentBeanFactory();
		wmc.setReturnValue(null);
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				return new VelocityEngine() {
					public Template getTemplate(String tn)
						throws ResourceNotFoundException, ParseErrorException, Exception {
						assertEquals(tn, templateName);
						throw templateLookupException;
					}
				};
			}
		};
		wac.getBeansOfType(VelocityConfig.class, true, false);
		Map configurers = new HashMap();
		configurers.put("velocityConfigurer", vc);
		wmc.setReturnValue(configurers);
		wmc.replay();

		VelocityView vv = new VelocityView();
		//vv.setExposeDateFormatter(false);
		//vv.setExposeCurrencyFormatter(false);
		vv.setUrl(templateName);

		try {
			vv.setApplicationContext(wac);
			fail();
		}
		catch (ApplicationContextException ex) {
			assertEquals(ex.getCause(), templateLookupException);
		}

		wmc.verify();
	}
	
	public void testMergeTemplateSucceeds() throws Exception {
		testValidTemplateName(null);
	}
	
	public void testMergeTemplateFailureWithIOException() throws Exception {
		testValidTemplateName(new IOException());
	}
	
	public void testMergeTemplateFailureWithParseErrorException() throws Exception {
		testValidTemplateName(new ParseErrorException(""));
	}
		
	public void testMergeTemplateFailureWithUnspecifiedException() throws Exception {
		testValidTemplateName(new Exception(""));
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
		wmc.setReturnValue(sc, 4);
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
		wmc.setReturnValue(sc, 4);
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

	public void testVelocityToolboxView() throws Exception {
		final String templateName = "test.vm";

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		final Template expectedTemplate = new Template();
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine(templateName, expectedTemplate);
			}
		};
		wac.getDefaultListableBeanFactory().registerSingleton("velocityConfigurer", vc);

		final HttpServletRequest expectedRequest = new MockHttpServletRequest();
		final HttpServletResponse expectedResponse = new MockHttpServletResponse();

		VelocityToolboxView vv = new VelocityToolboxView() {
			protected void mergeTemplate(Template template, Context context, HttpServletResponse response) throws Exception {
				assertTrue(template == expectedTemplate);
				assertTrue(response == expectedResponse);
				assertTrue(context instanceof ChainedContext);

				assertEquals("this is foo.", context.get("foo"));
				assertTrue(context.get("map") instanceof HashMap);
				assertTrue(context.get("date") instanceof DateTool);
				assertTrue(context.get("math") instanceof MathTool);

				assertTrue(context.get("link") instanceof LinkTool);
				LinkTool linkTool = (LinkTool) context.get("link");
				assertNotNull(linkTool.getContextURL());

				assertTrue(context.get("link2") instanceof LinkTool);
				LinkTool linkTool2 = (LinkTool) context.get("link2");
				assertNotNull(linkTool2.getContextURL());
			}
		};

		vv.setUrl(templateName);
		vv.setApplicationContext(wac);
		Map<String, Class> toolAttributes = new HashMap<String, Class>();
		toolAttributes.put("math", MathTool.class);
		toolAttributes.put("link2", LinkTool.class);
		vv.setToolAttributes(toolAttributes);
		vv.setToolboxConfigLocation("org/springframework/web/servlet/view/velocity/toolbox.xml");
		vv.setExposeSpringMacroHelpers(false);

		vv.render(new HashMap(), expectedRequest, expectedResponse);
	}

	public void testVelocityViewResolver() throws Exception {
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine("prefix_test_suffix", new Template());
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.getBeanFactory().registerSingleton("configurer", vc);

		VelocityViewResolver vr = new VelocityViewResolver();
		vr.setPrefix("prefix_");
		vr.setSuffix("_suffix");
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertEquals("Correct view class", VelocityView.class, view.getClass());
		assertEquals("Correct URL", "prefix_test_suffix", ((VelocityView) view).getUrl());

		view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
		assertEquals("Correct view class", RedirectView.class, view.getClass());
		assertEquals("Correct URL", "myUrl", ((RedirectView) view).getUrl());

		view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
		assertEquals("Correct view class", InternalResourceView.class, view.getClass());
		assertEquals("Correct URL", "myUrl", ((InternalResourceView) view).getUrl());
	}

	public void testVelocityViewResolverWithToolbox() throws Exception {
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine("prefix_test_suffix", new Template());
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.getBeanFactory().registerSingleton("configurer", vc);

		String toolbox = "org/springframework/web/servlet/view/velocity/toolbox.xml";

		VelocityViewResolver vr = new VelocityViewResolver();
		vr.setPrefix("prefix_");
		vr.setSuffix("_suffix");
		vr.setToolboxConfigLocation(toolbox);
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertEquals("Correct view class", VelocityToolboxView.class, view.getClass());
		assertEquals("Correct URL", "prefix_test_suffix", ((VelocityView) view).getUrl());
		assertEquals("Correct toolbox", toolbox, ((VelocityToolboxView) view).getToolboxConfigLocation());
	}

	public void testVelocityViewResolverWithToolboxSubclass() throws Exception {
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				TestVelocityEngine ve = new TestVelocityEngine();
				ve.addTemplate("prefix_test_suffix", new Template());
				ve.addTemplate(VelocityLayoutView.DEFAULT_LAYOUT_URL, new Template());
				return ve;
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.getBeanFactory().registerSingleton("configurer", vc);

		String toolbox = "org/springframework/web/servlet/view/velocity/toolbox.xml";

		VelocityViewResolver vr = new VelocityViewResolver();
		vr.setViewClass(VelocityLayoutView.class);
		vr.setPrefix("prefix_");
		vr.setSuffix("_suffix");
		vr.setToolboxConfigLocation(toolbox);
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertEquals("Correct view class", VelocityLayoutView.class, view.getClass());
		assertEquals("Correct URL", "prefix_test_suffix", ((VelocityView) view).getUrl());
		assertEquals("Correct toolbox", toolbox, ((VelocityToolboxView) view).getToolboxConfigLocation());
	}

	public void testVelocityLayoutViewResolver() throws Exception {
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				TestVelocityEngine ve = new TestVelocityEngine();
				ve.addTemplate("prefix_test_suffix", new Template());
				ve.addTemplate("myLayoutUrl", new Template());
				return ve;
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.getBeanFactory().registerSingleton("configurer", vc);

		VelocityLayoutViewResolver vr = new VelocityLayoutViewResolver();
		vr.setPrefix("prefix_");
		vr.setSuffix("_suffix");
		vr.setLayoutUrl("myLayoutUrl");
		vr.setLayoutKey("myLayoutKey");
		vr.setScreenContentKey("myScreenContentKey");
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertEquals("Correct view class", VelocityLayoutView.class, view.getClass());
		assertEquals("Correct URL", "prefix_test_suffix", ((VelocityView) view).getUrl());
		// TODO: Need to test actual VelocityLayoutView properties and their functionality!
	}

}
