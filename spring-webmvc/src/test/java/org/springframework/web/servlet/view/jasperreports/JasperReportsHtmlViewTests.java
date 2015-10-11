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

package org.springframework.web.servlet.view.jasperreports;

import org.junit.Test;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 */
public class JasperReportsHtmlViewTests extends AbstractJasperReportsViewTests {

	@Override
	protected AbstractJasperReportsView getViewImplementation() {
		return new JasperReportsHtmlView();
	}

	@Override
	protected String getDesiredContentType() {
		return "text/html";
	}

	@Test
	@SuppressWarnings("deprecation")
	public void configureExporterParametersWithEncodingFromPropertiesFile() throws Exception {
		GenericWebApplicationContext ac = new GenericWebApplicationContext();
		ac.setServletContext(new MockServletContext());
		BeanDefinitionReader reader = new PropertiesBeanDefinitionReader(ac);
		reader.loadBeanDefinitions(new ClassPathResource("view.properties", getClass()));
		ac.refresh();

		AbstractJasperReportsView view = (AbstractJasperReportsView) ac.getBean("report");
		String encoding = (String) view.getConvertedExporterParameters().get(
			net.sf.jasperreports.engine.export.JRHtmlExporterParameter.CHARACTER_ENCODING);
		assertEquals("UTF-8", encoding);

		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, ac);
		view.render(getModel(), request, response);
		assertEquals("Response content type is incorrect", "text/html;charset=UTF-8", response.getContentType());
	}

}
