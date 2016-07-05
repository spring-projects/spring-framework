/*
 * Copyright 2002-2016 the original author or authors.
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.Assume;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 18.11.2004
 */
@SuppressWarnings("deprecation")
public class JasperReportsUtilsTests {

	@BeforeClass
	public static void assumptions() {
		Assume.canLoadNativeDirFonts();
	}

	@Test
	public void renderAsCsvWithDataSource() throws Exception {
		StringWriter writer = new StringWriter();
		JasperReportsUtils.renderAsCsv(getReport(), getParameters(), getDataSource(), writer);
		String output = writer.getBuffer().toString();
		assertCsvOutputCorrect(output);
	}

	@Test
	public void renderAsCsvWithCollection() throws Exception {
		StringWriter writer = new StringWriter();
		JasperReportsUtils.renderAsCsv(getReport(), getParameters(), getData(), writer);
		String output = writer.getBuffer().toString();
		assertCsvOutputCorrect(output);
	}

	@Test
	public void renderAsCsvWithExporterParameters() throws Exception {
		StringWriter writer = new StringWriter();
		Map<JRExporterParameter, Object> exporterParameters = new HashMap<>();
		exporterParameters.put(JRCsvExporterParameter.FIELD_DELIMITER, "~");
		JasperReportsUtils.renderAsCsv(getReport(), getParameters(), getData(), writer, exporterParameters);
		String output = writer.getBuffer().toString();
		assertCsvOutputCorrect(output);
		assertTrue("Delimiter is incorrect", output.contains("~"));
	}

	@Test
	public void renderAsHtmlWithDataSource() throws Exception {
		StringWriter writer = new StringWriter();
		JasperReportsUtils.renderAsHtml(getReport(), getParameters(), getDataSource(), writer);
		String output = writer.getBuffer().toString();
		assertHtmlOutputCorrect(output);
	}

	@Test
	public void renderAsHtmlWithCollection() throws Exception {
		StringWriter writer = new StringWriter();
		JasperReportsUtils.renderAsHtml(getReport(), getParameters(), getData(), writer);
		String output = writer.getBuffer().toString();
		assertHtmlOutputCorrect(output);
	}

	@Test
	public void renderAsHtmlWithExporterParameters() throws Exception {
		StringWriter writer = new StringWriter();
		Map<JRExporterParameter, Object> exporterParameters = new HashMap<>();
		String uri = "/my/uri";
		exporterParameters.put(JRHtmlExporterParameter.IMAGES_URI, uri);
		JasperReportsUtils.renderAsHtml(getReport(), getParameters(), getData(), writer, exporterParameters);
		String output = writer.getBuffer().toString();
		assertHtmlOutputCorrect(output);
		assertTrue("URI not included", output.contains(uri));
	}

	@Test
	public void renderAsPdfWithDataSource() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JasperReportsUtils.renderAsPdf(getReport(), getParameters(), getDataSource(), os);
		byte[] output = os.toByteArray();
		assertPdfOutputCorrect(output);
	}

	@Test
	public void renderAsPdfWithCollection() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JasperReportsUtils.renderAsPdf(getReport(), getParameters(), getData(), os);
		byte[] output = os.toByteArray();
		assertPdfOutputCorrect(output);
	}

	@Test
	public void renderAsPdfWithExporterParameters() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Map<JRExporterParameter, Object> exporterParameters = new HashMap<>();
		exporterParameters.put(JRPdfExporterParameter.PDF_VERSION, JRPdfExporterParameter.PDF_VERSION_1_6.toString());
		JasperReportsUtils.renderAsPdf(getReport(), getParameters(), getData(), os, exporterParameters);
		byte[] output = os.toByteArray();
		assertPdfOutputCorrect(output);
		assertTrue(new String(output).contains("PDF-1.6"));
	}

	@Test
	public void renderAsXlsWithDataSource() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JasperReportsUtils.renderAsXls(getReport(), getParameters(), getDataSource(), os);
		byte[] output = os.toByteArray();
		assertXlsOutputCorrect(output);
	}

	@Test
	public void renderAsXlsWithCollection() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JasperReportsUtils.renderAsXls(getReport(), getParameters(), getData(), os);
		byte[] output = os.toByteArray();
		assertXlsOutputCorrect(output);
	}

	@Test
	public void renderAsXlsWithExporterParameters() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Map<JRExporterParameter, Object> exporterParameters = new HashMap<>();

		SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		exporterParameters.put(JRXlsExporterParameter.PROGRESS_MONITOR, monitor);

		JasperReportsUtils.renderAsXls(getReport(), getParameters(), getData(), os, exporterParameters);
		byte[] output = os.toByteArray();
		assertXlsOutputCorrect(output);
		assertTrue(monitor.isInvoked());
	}

	@Test
	public void renderWithWriter() throws Exception {
		StringWriter writer = new StringWriter();
		JasperPrint print = JasperFillManager.fillReport(getReport(), getParameters(), getDataSource());
		JasperReportsUtils.render(new JRHtmlExporter(), print, writer);
		String output = writer.getBuffer().toString();
		assertHtmlOutputCorrect(output);
	}

	@Test
	public void renderWithOutputStream() throws Exception {
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

	@SuppressWarnings("resource")
	private void assertXlsOutputCorrect(byte[] output) throws Exception {
		HSSFWorkbook workbook = new HSSFWorkbook(new ByteArrayInputStream(output));
		HSSFSheet sheet = workbook.getSheetAt(0);
		assertNotNull("Sheet should not be null", sheet);
		HSSFRow row = sheet.getRow(3);
		HSSFCell cell = row.getCell((short) 1);
		assertNotNull("Cell should not be null", cell);
		assertEquals("Cell content should be Dear Lord!", "Dear Lord!", cell.getRichStringCellValue().getString());
	}

	private JasperReport getReport() throws Exception {
		ClassPathResource resource = new ClassPathResource("DataSourceReport.jasper", getClass());
		return (JasperReport) JRLoader.loadObject(resource.getInputStream());
	}

	private Map<String, Object> getParameters() {
		Map<String, Object> model = new HashMap<>();
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
		List<PersonBean> list = new ArrayList<>();
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

		@Override
		public void afterPageExport() {
			this.invoked = true;
		}

		public boolean isInvoked() {
			return invoked;
		}
	}

}
