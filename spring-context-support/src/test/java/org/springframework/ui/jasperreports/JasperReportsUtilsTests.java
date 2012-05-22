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

package org.springframework.ui.jasperreports;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import junit.framework.TestCase;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporterParameter;
import net.sf.jasperreports.engine.export.JRExportProgressMonitor;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import org.springframework.core.io.ClassPathResource;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 18.11.2004
 */
public class JasperReportsUtilsTests extends TestCase {

	public void testRenderAsCsvWithDataSource() throws Exception {
		StringWriter writer = new StringWriter();
		JasperReportsUtils.renderAsCsv(getReport(), getParameters(), getDataSource(), writer);
		String output = writer.getBuffer().toString();
		assertCsvOutputCorrect(output);
	}

	public void testRenderAsCsvWithCollection() throws Exception {
		StringWriter writer = new StringWriter();
		JasperReportsUtils.renderAsCsv(getReport(), getParameters(), getData(), writer);
		String output = writer.getBuffer().toString();
		assertCsvOutputCorrect(output);
	}

	public void testRenderAsCsvWithExporterParameters() throws Exception {
		StringWriter writer = new StringWriter();
		Map<Object, String> exporterParameters = new HashMap<Object, String>();
		exporterParameters.put(JRCsvExporterParameter.FIELD_DELIMITER, "~");
		JasperReportsUtils.renderAsCsv(getReport(), getParameters(), getData(), writer, exporterParameters);
		String output = writer.getBuffer().toString();
		assertCsvOutputCorrect(output);
		assertTrue("Delimiter is incorrect", output.contains("~"));
	}

	public void testRenderAsHtmlWithDataSource() throws Exception {
		StringWriter writer = new StringWriter();
		JasperReportsUtils.renderAsHtml(getReport(), getParameters(), getDataSource(), writer);
		String output = writer.getBuffer().toString();
		assertHtmlOutputCorrect(output);
	}

	public void testRenderAsHtmlWithCollection() throws Exception {
		StringWriter writer = new StringWriter();
		JasperReportsUtils.renderAsHtml(getReport(), getParameters(), getData(), writer);
		String output = writer.getBuffer().toString();
		assertHtmlOutputCorrect(output);
	}

	public void testRenderAsHtmlWithExporterParameters() throws Exception {
		StringWriter writer = new StringWriter();
		Map<Object, String> exporterParameters = new HashMap<Object, String>();
		String uri = "/my/uri";
		exporterParameters.put(JRHtmlExporterParameter.IMAGES_URI, uri);
		JasperReportsUtils.renderAsHtml(getReport(), getParameters(), getData(), writer, exporterParameters);
		String output = writer.getBuffer().toString();
		assertHtmlOutputCorrect(output);
		assertTrue("URI not included", output.contains(uri));
	}

	public void testRenderAsPdfWithDataSource() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JasperReportsUtils.renderAsPdf(getReport(), getParameters(), getDataSource(), os);
		byte[] output = os.toByteArray();
		assertPdfOutputCorrect(output);
	}

	public void testRenderAsPdfWithCollection() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JasperReportsUtils.renderAsPdf(getReport(), getParameters(), getData(), os);
		byte[] output = os.toByteArray();
		assertPdfOutputCorrect(output);
	}

	public void testRenderAsPdfWithExporterParameters() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Map<Object, String> exporterParameters = new HashMap<Object, String>();
		exporterParameters.put(JRPdfExporterParameter.PDF_VERSION, JRPdfExporterParameter.PDF_VERSION_1_6.toString());
		JasperReportsUtils.renderAsPdf(getReport(), getParameters(), getData(), os, exporterParameters);
		byte[] output = os.toByteArray();
		assertPdfOutputCorrect(output);
		assertTrue(new String(output).contains("PDF-1.6"));
	}

	public void testRenderAsXlsWithDataSource() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JasperReportsUtils.renderAsXls(getReport(), getParameters(), getDataSource(), os);
		byte[] output = os.toByteArray();
		assertXlsOutputCorrect(output);
	}

	public void testRenderAsXlsWithCollection() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JasperReportsUtils.renderAsXls(getReport(), getParameters(), getData(), os);
		byte[] output = os.toByteArray();
		assertXlsOutputCorrect(output);
	}

	public void testRenderAsXlsWithExporterParameters() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Map<Object, Object> exporterParameters = new HashMap<Object, Object>();

		SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		exporterParameters.put(JRXlsExporterParameter.PROGRESS_MONITOR, monitor);

		JasperReportsUtils.renderAsXls(getReport(), getParameters(), getData(), os, exporterParameters);
		byte[] output = os.toByteArray();
		assertXlsOutputCorrect(output);
		assertTrue(monitor.isInvoked());
	}

	public void testRenderWithWriter() throws Exception {
		StringWriter writer = new StringWriter();
		JasperPrint print = JasperFillManager.fillReport(getReport(), getParameters(), getDataSource());
		JasperReportsUtils.render(new JRHtmlExporter(), print, writer);
		String output = writer.getBuffer().toString();
		assertHtmlOutputCorrect(output);
	}

	public void testRenderWithOutputStream() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JasperPrint print = JasperFillManager.fillReport(getReport(), getParameters(), getDataSource());
		JasperReportsUtils.render(new JRPdfExporter(), print, os);
		byte[] output = os.toByteArray();
		assertPdfOutputCorrect(output);
	}

	private void assertCsvOutputCorrect(String output) {
		assertTrue("Output length should be greater than 0", (output.length() > 0));
		assertTrue("Output should start with Dear Lord!", output.startsWith("Dear Lord!"));
		assertTrue("Output should contain 'MeineSeite'", output.contains("MeineSeite"));
	}

	private void assertHtmlOutputCorrect(String output) {
		assertTrue("Output length should be greater than 0", (output.length() > 0));
		assertTrue("Output should contain <html>", output.contains("<html>"));
		assertTrue("Output should contain 'MeineSeite'", output.contains("MeineSeite"));
	}

	private void assertPdfOutputCorrect(byte[] output) throws Exception {
		assertTrue("Output length should be greater than 0", (output.length > 0));

		String translated = new String(output, "US-ASCII");
		assertTrue("Output should start with %PDF", translated.startsWith("%PDF"));
	}

	private void assertXlsOutputCorrect(byte[] output) throws Exception {
		HSSFWorkbook workbook = new HSSFWorkbook(new ByteArrayInputStream(output));
		HSSFSheet sheet = workbook.getSheetAt(0);
		assertNotNull("Sheet should not be null", sheet);
		HSSFRow row = sheet.getRow(3);
		HSSFCell cell = row.getCell((short) 1);
		assertNotNull("Cell should not be null", cell);
		assertEquals("Cell content should be Dear Lord!", "Dear Lord!", cell.getStringCellValue());
	}

	private JasperReport getReport() throws Exception {
		ClassPathResource resource = new ClassPathResource("DataSourceReport.jasper", getClass());
		return (JasperReport) JRLoader.loadObject(resource.getInputStream());
	}

	private Map<Object, Object> getParameters() {
		HashMap<Object, Object> model = new HashMap<Object, Object>();
		model.put("ReportTitle", "Dear Lord!");
		model.put(JRParameter.REPORT_LOCALE, Locale.GERMAN);
		model.put(JRParameter.REPORT_RESOURCE_BUNDLE,
				ResourceBundle.getBundle("org/springframework/ui/jasperreports/messages", Locale.GERMAN));
		return model;
	}

	private JRDataSource getDataSource() {
		return new JRBeanCollectionDataSource(getData());
	}

	private List<PersonBean> getData() {
		ArrayList<PersonBean> list = new ArrayList<PersonBean>();
		for (int x = 0; x < 10; x++) {
			PersonBean bean = new PersonBean();
			bean.setId(x);
			bean.setName("Rob Harrop");
			bean.setStreet("foo");
			list.add(bean);
		}
		return list;
	}


	private static class SimpleProgressMonitor implements JRExportProgressMonitor {

		private boolean invoked = false;

		public void afterPageExport() {
			this.invoked = true;
		}

		public boolean isInvoked() {
			return invoked;
		}
	}

}
