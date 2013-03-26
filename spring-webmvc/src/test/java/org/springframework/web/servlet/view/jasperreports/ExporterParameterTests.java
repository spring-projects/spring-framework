/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;

import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Rob Harrop
 */
public class ExporterParameterTests extends AbstractJasperReportsTests {

	public void testParameterParsing() throws Exception {
		Map params = new HashMap();
		params.put("net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI", "/foo/bar");

		AbstractJasperReportsView view = new AbstractJasperReportsView() {
			@Override
			protected void renderReport(JasperPrint filledReport, Map model, HttpServletResponse response)
					throws Exception {

				assertEquals("Invalid number of exporter parameters", 1, getConvertedExporterParameters().size());

				JRExporterParameter key = JRHtmlExporterParameter.IMAGES_URI;
				Object value = getConvertedExporterParameters().get(key);

				assertNotNull("Value not mapped to correct key", value);
				assertEquals("Incorrect value for parameter", "/foo/bar", value);
			}

			/**
			 * Merges the configured {@link net.sf.jasperreports.engine.JRExporterParameter JRExporterParameters} with any specified
			 * in the supplied model data. {@link net.sf.jasperreports.engine.JRExporterParameter JRExporterParameters} in the model
			 * override those specified in the configuration.
			 * @see #setExporterParameters(java.util.Map)
			 */
			protected Map mergeExporterParameters(Map model) {
				Map mergedParameters = new HashMap(getConvertedExporterParameters());
				for (Iterator iterator = model.keySet().iterator(); iterator.hasNext();) {
					Object key = iterator.next();

					if (key instanceof JRExporterParameter) {
						Object value = model.get(key);
						if (value instanceof String) {
							mergedParameters.put(key, value);
						}
						else {
							logger.warn("Ignoring exporter parameter [" + key + "]. Value is not a String.");
						}
					}
				}
				return mergedParameters;
			}
		};

		view.setExporterParameters(params);
		setViewProperties(view);
		view.render(getModel(), request, response);
	}

	public void testInvalidClass() throws Exception {
		Map params = new HashMap();
		params.put("foo.net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI", "/foo");

		AbstractJasperReportsView view = new JasperReportsHtmlView();
		setViewProperties(view);

		try {
			view.setExporterParameters(params);
			view.convertExporterParameters();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testInvalidField() {
		Map params = new HashMap();
		params.put("net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI_FOO", "/foo");

		AbstractJasperReportsView view = new JasperReportsHtmlView();
		setViewProperties(view);

		try {
			view.setExporterParameters(params);
			view.convertExporterParameters();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testInvalidType() {
		Map params = new HashMap();
		params.put("java.lang.Boolean.TRUE", "/foo");

		AbstractJasperReportsView view = new JasperReportsHtmlView();
		setViewProperties(view);

		try {
			view.setExporterParameters(params);
			view.convertExporterParameters();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}


	public void testTypeConversion() {
		Map params = new HashMap();
		params.put("net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN", "true");

		AbstractJasperReportsView view = new JasperReportsHtmlView();
		setViewProperties(view);

		view.setExporterParameters(params);
		view.convertExporterParameters();
		Object value = view.getConvertedExporterParameters().get(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN);
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
