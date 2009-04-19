/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.servlet.view.freemarker;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
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
 * @author Juergen Hoeller
 * @since 14.03.2004
 */
public class FreeMarkerViewTests {

	@Test
	public void testNoFreemarkerConfig() throws Exception {
		FreeMarkerView fv = new FreeMarkerView();

		MockControl wmc = MockControl.createControl(WebApplicationContext.class);
		WebApplicationContext wac = (WebApplicationContext) wmc.getMock();
		wac.getBeansOfType(FreeMarkerConfig.class, true, false);
		wmc.setReturnValue(new HashMap());
		wac.getParentBeanFactory();
		wmc.setReturnValue(null);
		wac.getServletContext();
		wmc.setReturnValue(new MockServletContext());
		wmc.replay();

		fv.setUrl("anythingButNull");
		try {
			fv.setApplicationContext(wac);
			fv.afterPropertiesSet();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (ApplicationContextException ex) {
			// Check there's a helpful error message
			assertTrue(ex.getMessage().indexOf("FreeMarkerConfig") != -1);
		}

		wmc.verify();
	}

	@Test
	public void testNoTemplateName() throws Exception {
		FreeMarkerView fv = new FreeMarkerView();
		try {
			fv.afterPropertiesSet();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// Check there's a helpful error message
			assertTrue(ex.getMessage().indexOf("url") != -1);
		}
	}

	@Test
	public void testValidTemplateName() throws Exception {
		FreeMarkerView fv = new FreeMarkerView();

		MockControl wmc = MockControl.createNiceControl(WebApplicationContext.class);
		WebApplicationContext wac = (WebApplicationContext) wmc.getMock();
		MockServletContext sc = new MockServletContext();

		wac.getBeansOfType(FreeMarkerConfig.class, true, false);
		Map configs = new HashMap();
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());
		configs.put("freemarkerConfig", configurer);
		wmc.setReturnValue(configs);
		wac.getParentBeanFactory();
		wmc.setReturnValue(null);
		wac.getServletContext();
		wmc.setReturnValue(sc, 5);
		wmc.replay();

		fv.setUrl("templateName");
		fv.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.US);
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		HttpServletResponse response = new MockHttpServletResponse();

		Map model = new HashMap();
		model.put("myattr", "myvalue");
		fv.render(model, request, response);

		wmc.verify();
		assertEquals(AbstractView.DEFAULT_CONTENT_TYPE, response.getContentType());
	}

	@Test
	public void testKeepExistingContentType() throws Exception {
		FreeMarkerView fv = new FreeMarkerView();

		MockControl wmc = MockControl.createNiceControl(WebApplicationContext.class);
		WebApplicationContext wac = (WebApplicationContext) wmc.getMock();
		MockServletContext sc = new MockServletContext();

		wac.getBeansOfType(FreeMarkerConfig.class, true, false);
		Map configs = new HashMap();
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());
		configs.put("freemarkerConfig", configurer);
		wmc.setReturnValue(configs);
		wac.getParentBeanFactory();
		wmc.setReturnValue(null);
		wac.getServletContext();
		wmc.setReturnValue(sc, 5);
		wmc.replay();

		fv.setUrl("templateName");
		fv.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.US);
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		HttpServletResponse response = new MockHttpServletResponse();
		response.setContentType("myContentType");

		Map model = new HashMap();
		model.put("myattr", "myvalue");
		fv.render(model, request, response);

		wmc.verify();
		assertEquals("myContentType", response.getContentType());
	}


	private class TestConfiguration extends Configuration {

		public Template getTemplate(String name, final Locale locale) throws IOException {
			assertEquals("templateName", name);
			return new Template(name, new StringReader("test")) {
				public void process(Object model, Writer writer) throws TemplateException, IOException {
					assertEquals(Locale.US, locale);
					assertTrue(model instanceof AllHttpScopesHashModel);
					AllHttpScopesHashModel fmModel = (AllHttpScopesHashModel) model;
					assertEquals("myvalue", fmModel.get("myattr").toString());
				}
			};
		}
	}

}
