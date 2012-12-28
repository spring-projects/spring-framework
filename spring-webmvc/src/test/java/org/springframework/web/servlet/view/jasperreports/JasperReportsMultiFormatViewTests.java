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

package org.springframework.web.servlet.view.jasperreports;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperPrint;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class JasperReportsMultiFormatViewTests extends AbstractJasperReportsViewTests {

	@Override
	protected void extendModel(Map<String, Object> model) {
		model.put(getDiscriminatorKey(), "csv");
	}

	public void testSimpleHtmlRender() throws Exception {
		if (!canCompileReport) {
			return;
		}

		AbstractJasperReportsView view = getView(UNCOMPILED_REPORT);

		Map<String, Object> model = getBaseModel();
		model.put(getDiscriminatorKey(), "html");

		view.render(model, request, response);

		assertEquals("Invalid content type", "text/html", response.getContentType());
	}

	@Override
	public void testOverrideContentDisposition() throws Exception {
		if (!canCompileReport) {
			return;
		}

		AbstractJasperReportsView view = getView(UNCOMPILED_REPORT);

		Map<String, Object> model = getBaseModel();
		model.put(getDiscriminatorKey(), "csv");

		String headerValue = "inline; filename=foo.txt";

		Properties mappings = new Properties();
		mappings.put("csv", headerValue);

		((JasperReportsMultiFormatView) view).setContentDispositionMappings(mappings);

		view.render(model, request, response);

		assertEquals("Invalid Content-Disposition header value", headerValue,
				response.getHeader("Content-Disposition"));
	}

	public void testExporterParametersAreCarriedAcross() throws Exception {
		if (!canCompileReport) {
			return;
		}

		JasperReportsMultiFormatView view = (JasperReportsMultiFormatView) getView(UNCOMPILED_REPORT);

		Map<String, Class<? extends AbstractJasperReportsView>> mappings =
				new HashMap<String, Class<? extends AbstractJasperReportsView>>();
		mappings.put("test", ExporterParameterTestView.class);

		Map<String, String> exporterParameters = new HashMap<String, String>();

		// test view class performs the assertions - robh
		exporterParameters.put(ExporterParameterTestView.TEST_PARAM, "foo");

		view.setExporterParameters(exporterParameters);
		view.setFormatMappings(mappings);
		view.initApplicationContext();

		Map<String, Object> model = getBaseModel();
		model.put(getDiscriminatorKey(), "test");

		view.render(model, request, response);
	}

	protected String getDiscriminatorKey() {
		return "format";
	}

	@Override
	protected AbstractJasperReportsView getViewImplementation() {
		return new JasperReportsMultiFormatView();
	}

	@Override
	protected String getDesiredContentType() {
		return "text/csv";
	}

	private Map<String, Object> getBaseModel() {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("ReportTitle", "Foo");
		model.put("dataSource", getData());
		return model;
	}


	public static class ExporterParameterTestView extends AbstractJasperReportsView {

		public static final String TEST_PARAM = "net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI";

		@Override
		protected void renderReport(JasperPrint filledReport, Map parameters, HttpServletResponse response) {
			assertNotNull("Exporter parameters are null", getExporterParameters());
			assertEquals("Incorrect number of exporter parameters", 1, getExporterParameters().size());
		}
	}

}
