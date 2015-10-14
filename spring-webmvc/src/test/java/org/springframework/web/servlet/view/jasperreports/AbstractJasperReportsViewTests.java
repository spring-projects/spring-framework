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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRAbstractBeanDataSourceProvider;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ApplicationContextException;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.ui.jasperreports.PersonBean;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public abstract class AbstractJasperReportsViewTests extends AbstractJasperReportsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	/**
	 * Simple test to see if compiled report succeeds.
	 */
	@Test
	public void compiledReport() throws Exception {
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.render(getModel(), request, response);
		assertTrue(response.getContentAsByteArray().length > 0);

		assumeTrue(view instanceof AbstractJasperReportsSingleFormatView
				&& ((AbstractJasperReportsSingleFormatView) view).useWriter());

		String output = response.getContentAsString();
		assertTrue("Output should contain 'MeineSeite'", output.contains("MeineSeite"));
	}

	@Test
	public void uncompiledReport() throws Exception {
		assumeTrue(canCompileReport);

		AbstractJasperReportsView view = getView(UNCOMPILED_REPORT);
		view.render(getModel(), request, response);
		assertTrue(response.getContentAsByteArray().length > 0);
	}

	@Test
	public void withInvalidPath() throws Exception {
		exception.expect(ApplicationContextException.class);
		getView("foo.jasper");
	}

	@Test
	public void invalidExtension() throws Exception {
		exception.expect(IllegalArgumentException.class);
		getView("foo.bar");
	}

	@Test
	public void contentType() throws Exception {
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.render(getModel(), request, response);
		assertEquals("Response content type is incorrect", getDesiredContentType(), response.getContentType());
	}

	@Test
	public void withoutDatasource() throws Exception {
		Map<String, Object> model = getModel();
		model.remove("dataSource");
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.render(model, request, response);
		assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
	}

	@Test
	public void withCollection() throws Exception {
		Map<String, Object> model = getModel();
		model.remove("dataSource");
		model.put("reportData", getData());
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.render(model, request, response);
		assertTrue(response.getContentAsByteArray().length > 0);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void withMultipleCollections() throws Exception {
		Map<String, Object> model = getModel();
		model.remove("dataSource");
		model.put("reportData", getData());
		model.put("otherData", new LinkedList());
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.render(model, request, response);
		// no clear data source found
	}

	@Test
	public void withJRDataSourceProvider() throws Exception {
		Map<String, Object> model = getModel();
		model.remove("dataSource");
		model.put("dataSource", new MockDataSourceProvider(PersonBean.class));
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.render(model, request, response);
		assertTrue(response.getContentAsByteArray().length > 0);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void withSpecificCollection() throws Exception {
		Map<String, Object> model = getModel();
		model.remove("dataSource");
		model.put("reportData", getData());
		model.put("otherData", new LinkedList());
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.setReportDataKey("reportData");
		view.render(model, request, response);
		assertTrue(response.getContentAsByteArray().length > 0);
	}

	@Test
	public void withArray() throws Exception {
		Map<String, Object> model = getModel();
		model.remove("dataSource");
		model.put("reportData", getData().toArray());
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.render(model, request, response);
		assertTrue(response.getContentAsByteArray().length > 0);
	}

	@Test
	public void withMultipleArrays() throws Exception {
		Map<String, Object> model = getModel();
		model.remove("dataSource");
		model.put("reportData", getData().toArray());
		model.put("otherData", new String[0]);
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.render(model, request, response);
		// no clear data source found
	}

	@Test
	public void withSpecificArray() throws Exception {
		Map<String, Object> model = getModel();
		model.remove("dataSource");
		model.put("reportData", getData().toArray());
		model.put("otherData", new String[0]);
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.setReportDataKey("reportData");
		view.render(model, request, response);
		assertTrue(response.getContentAsByteArray().length > 0);
	}

	@Test
	public void withSubReport() throws Exception {
		assumeTrue(canCompileReport);

		Map<String, Object> model = getModel();
		model.put("SubReportData", getProductData());

		Properties subReports = new Properties();
		subReports.put("ProductsSubReport", "/org/springframework/ui/jasperreports/subReportChild.jrxml");

		AbstractJasperReportsView view = getView(SUB_REPORT_PARENT);
		view.setReportDataKey("dataSource");
		view.setSubReportUrls(subReports);
		view.setSubReportDataKeys(new String[]{"SubReportData"});
		view.initApplicationContext();
		view.render(model, request, response);

		assertTrue(response.getContentAsByteArray().length > 0);
	}

	@Test
	public void withNonExistentSubReport() throws Exception {
		assumeTrue(canCompileReport);

		Map<String, Object> model = getModel();
		model.put("SubReportData", getProductData());

		Properties subReports = new Properties();
		subReports.put("ProductsSubReport", "org/springframework/ui/jasperreports/subReportChildFalse.jrxml");

		AbstractJasperReportsView view = getView(SUB_REPORT_PARENT);
		view.setReportDataKey("dataSource");
		view.setSubReportUrls(subReports);
		view.setSubReportDataKeys(new String[]{"SubReportData"});

		// Invalid report URL should throw ApplicationContextException
		exception.expect(ApplicationContextException.class);
		view.initApplicationContext();
	}

	// TODO Determine why encoding does not get overridden.
	@Ignore("Disabled since encoding does not get overridden")
	@Test
	public void overrideExporterParameters() throws Exception {
		AbstractJasperReportsView view = getView(COMPILED_REPORT);

		assumeTrue(view instanceof AbstractJasperReportsSingleFormatView
				&& ((AbstractJasperReportsSingleFormatView) view).useWriter());

		String characterEncoding = "UTF-8";
		String overiddenCharacterEncoding = "ASCII";

		Map<Object, Object> parameters = new HashMap<Object, Object>();
		parameters.put(net.sf.jasperreports.engine.JRExporterParameter.CHARACTER_ENCODING, characterEncoding);

		view.setExporterParameters(parameters);
		view.convertExporterParameters();

		Map<String, Object> model = getModel();
		model.put(net.sf.jasperreports.engine.JRExporterParameter.CHARACTER_ENCODING.toString(),
			overiddenCharacterEncoding);

		view.render(model, this.request, this.response);

		assertEquals(overiddenCharacterEncoding, this.response.getCharacterEncoding());
	}

	@Test
	public void subReportWithUnspecifiedParentDataSource() throws Exception {
		assumeTrue(canCompileReport);

		Map<String, Object> model = getModel();
		model.put("SubReportData", getProductData());

		Properties subReports = new Properties();
		subReports.put("ProductsSubReport", "org/springframework/ui/jasperreports/subReportChildFalse.jrxml");

		AbstractJasperReportsView view = getView(SUB_REPORT_PARENT);
		view.setSubReportUrls(subReports);
		view.setSubReportDataKeys(new String[]{"SubReportData"});

		// Unspecified reportDataKey should throw exception when subReportDataSources is specified
		exception.expect(ApplicationContextException.class);
		view.initApplicationContext();
	}

	@Test
	public void contentDisposition() throws Exception {
		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.render(getModel(), request, response);
		assertEquals("Invalid content type", "inline", response.getHeader("Content-Disposition"));
	}

	@Test
	public void overrideContentDisposition() throws Exception {
		Properties headers = new Properties();
		String cd = "attachment";
		headers.setProperty("Content-Disposition", cd);

		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.setHeaders(headers);
		view.render(getModel(), request, response);
		assertEquals("Invalid content type", cd, response.getHeader("Content-Disposition"));
	}

	@Test
	public void setCustomHeaders() throws Exception {
		Properties headers = new Properties();

		String key = "foo";
		String value = "bar";

		headers.setProperty(key, value);

		AbstractJasperReportsView view = getView(COMPILED_REPORT);
		view.setHeaders(headers);
		view.render(getModel(), request, response);

		assertNotNull("Header not present", response.getHeader(key));
		assertEquals("Invalid header value", value, response.getHeader(key));
	}

	@Test
	public void withJdbcDataSource() throws Exception {
		assumeTrue(canCompileReport);

		AbstractJasperReportsView view = getView(UNCOMPILED_REPORT);
		view.setJdbcDataSource(getMockJdbcDataSource());

		Map<String, Object> model = getModel();
		model.remove("dataSource");

		// DataSource was not used as report DataSource
		exception.expect(SQLException.class);
		view.render(model, request, response);
	}

	@Test
	public void withJdbcDataSourceInModel() throws Exception {
		assumeTrue(canCompileReport);

		AbstractJasperReportsView view = getView(UNCOMPILED_REPORT);

		Map<String, Object> model = getModel();
		model.remove("dataSource");
		model.put("someKey", getMockJdbcDataSource());

		// DataSource was not used as report DataSource
		exception.expect(SQLException.class);
		view.render(model, request, response);
	}

	@Test
	public void jrDataSourceOverridesJdbcDataSource() throws Exception {
		assumeTrue(canCompileReport);

		AbstractJasperReportsView view = getView(UNCOMPILED_REPORT);
		view.setJdbcDataSource(getMockJdbcDataSource());

		view.render(getModel(), request, response);
	}

	@Test
	public void withCharacterEncoding() throws Exception {
		AbstractJasperReportsView view = getView(COMPILED_REPORT);

		assumeTrue(view instanceof AbstractJasperReportsSingleFormatView
				&& ((AbstractJasperReportsSingleFormatView) view).useWriter());

		String characterEncoding = "UTF-8";

		Map<Object, Object> parameters = new HashMap<Object, Object>();
		parameters.put(net.sf.jasperreports.engine.JRExporterParameter.CHARACTER_ENCODING, characterEncoding);

		view.setExporterParameters(parameters);
		view.convertExporterParameters();

		view.render(getModel(), this.request, this.response);
		assertEquals(characterEncoding, this.response.getCharacterEncoding());
	}

	protected AbstractJasperReportsView getView(String url) throws Exception {
		AbstractJasperReportsView view = getViewImplementation();
		view.setUrl(url);
		StaticWebApplicationContext ac = new StaticWebApplicationContext();
		ac.setServletContext(new MockServletContext());
		ac.addMessage("page", Locale.GERMAN, "MeineSeite");
		ac.refresh();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, ac);
		view.setApplicationContext(ac);
		return view;
	}

	protected abstract AbstractJasperReportsView getViewImplementation();

	protected abstract String getDesiredContentType();

	private DataSource getMockJdbcDataSource() throws SQLException {
		DataSource ds = mock(DataSource.class);
		given(ds.getConnection()).willThrow(new SQLException());
		return ds;
	}


	private class MockDataSourceProvider extends JRAbstractBeanDataSourceProvider {

		public MockDataSourceProvider(Class<?> clazz) {
			super(clazz);
		}

		@Override
		public JRDataSource create(JasperReport jasperReport) throws JRException {
			return new JRBeanCollectionDataSource(getData());
		}

		@Override
		public void dispose(JRDataSource jrDataSource) throws JRException {

		}
	}

}
