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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperPrint;

import org.junit.Test;

import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public class ExporterParameterTests extends AbstractJasperReportsTests {

	@Test
	public void parameterParsing() throws Exception {
		Map<String, String> params = new HashMap<String, String>();
		params.put("net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI", "/foo/bar");

		AbstractJasperReportsView view = new AbstractJasperReportsView() {

			@Override
			protected void renderReport(JasperPrint filledReport, Map<String, Object> model, HttpServletResponse response)
					throws Exception {

				assertEquals("Invalid number of exporter parameters", 1, getConvertedExporterParameters().size());

				net.sf.jasperreports.engine.JRExporterParameter key = net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI;
				Object value = getConvertedExporterParameters().get(key);

				assertNotNull("Value not mapped to correct key", value);
				assertEquals("Incorrect value for parameter", "/foo/bar", value);
			}

		};

		view.setExporterParameters(params);
		setViewProperties(view);
		view.render(getModel(), request, response);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidClass() throws Exception {
		Map<String, String> params = new HashMap<String, String>();
		params.put("foo.net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI", "/foo");

		AbstractJasperReportsView view = new JasperReportsHtmlView();
		setViewProperties(view);

		view.setExporterParameters(params);
		view.convertExporterParameters();
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidField() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI_FOO", "/foo");

		AbstractJasperReportsView view = new JasperReportsHtmlView();
		setViewProperties(view);

		view.setExporterParameters(params);
		view.convertExporterParameters();
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidType() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("java.lang.Boolean.TRUE", "/foo");

		AbstractJasperReportsView view = new JasperReportsHtmlView();
		setViewProperties(view);

		view.setExporterParameters(params);
		view.convertExporterParameters();
	}

	@Test
	public void typeConversion() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN", "true");

		AbstractJasperReportsView view = new JasperReportsHtmlView();
		setViewProperties(view);

		view.setExporterParameters(params);
		view.convertExporterParameters();
		Object value = view.getConvertedExporterParameters().get(
			net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN);
		assertEquals(Boolean.TRUE, value);
	}

	private void setViewProperties(AbstractJasperReportsView view) {
		view.setUrl("/org/springframework/ui/jasperreports/DataSourceReport.jasper");
		StaticWebApplicationContext ac = new StaticWebApplicationContext();
		ac.setServletContext(new MockServletContext());
		ac.addMessage("page", Locale.GERMAN, "MeineSeite");
		ac.refresh();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, ac);
		view.setApplicationContext(ac);
	}

}
