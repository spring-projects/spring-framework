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

package org.springframework.ui.jasperreports;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanArrayDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;

/**
 * Utility methods for working with JasperReports. Provides a set of convenience
 * methods for generating reports in a CSV, HTML, PDF and XLS formats.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.1.3
 */
public abstract class JasperReportsUtils {

	/**
	 * Convert the given report data value to a <code>JRDataSource</code>.
	 * <p>In the default implementation, a <code>JRDataSource</code>,
	 * <code>java.util.Collection</code> or object array is detected.
	 * The latter are converted to <code>JRBeanCollectionDataSource</code>
	 * or <code>JRBeanArrayDataSource</code>, respectively.
	 * @param value the report data value to convert
	 * @return the JRDataSource (never <code>null</code>)
	 * @throws IllegalArgumentException if the value could not be converted
	 * @see net.sf.jasperreports.engine.JRDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanArrayDataSource
	 */
	public static JRDataSource convertReportData(Object value) throws IllegalArgumentException {
		if (value instanceof JRDataSource) {
			return (JRDataSource) value;
		}
		else if (value instanceof Collection) {
			return new JRBeanCollectionDataSource((Collection) value);
		}
		else if (value instanceof Object[]) {
			return new JRBeanArrayDataSource((Object[]) value);
		}
		else {
			throw new IllegalArgumentException("Value [" + value + "] cannot be converted to a JRDataSource");
		}
	}

	/**
	 * Render the supplied <code>JasperPrint</code> instance using the
	 * supplied <code>JRAbstractExporter</code> instance and write the results
	 * to the supplied <code>Writer</code>.
	 * <p>Make sure that the <code>JRAbstractExporter</code> implementation
	 * you supply is capable of writing to a <code>Writer</code>.
	 * @param exporter the <code>JRAbstractExporter</code> to use to render the report
	 * @param print the <code>JasperPrint</code> instance to render
	 * @param writer the <code>Writer</code> to write the result to
	 * @throws JRException if rendering failed
	 */
	public static void render(JRExporter exporter, JasperPrint print, Writer writer)
			throws JRException {

		exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
		exporter.setParameter(JRExporterParameter.OUTPUT_WRITER, writer);
		exporter.exportReport();
	}

	/**
	 * Render the supplied <code>JasperPrint</code> instance using the
	 * supplied <code>JRAbstractExporter</code> instance and write the results
	 * to the supplied <code>OutputStream</code>.
	 * <p>Make sure that the <code>JRAbstractExporter</code> implementation you
	 * supply is capable of writing to a <code>OutputStream</code>.
	 * @param exporter the <code>JRAbstractExporter</code> to use to render the report
	 * @param print the <code>JasperPrint</code> instance to render
	 * @param outputStream the <code>OutputStream</code> to write the result to
	 * @throws JRException if rendering failed
	 */
	public static void render(JRExporter exporter, JasperPrint print, OutputStream outputStream)
			throws JRException {

		exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
		exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, outputStream);
		exporter.exportReport();
	}

	/**
	 * Render a report in CSV format using the supplied report data.
	 * Writes the results to the supplied <code>Writer</code>.
	 * @param report the <code>JasperReport</code> instance to render
	 * @param parameters the parameters to use for rendering
	 * @param writer the <code>Writer</code> to write the rendered report to
	 * @param reportData a <code>JRDataSource</code>, <code>java.util.Collection</code>
	 * or object array (converted accordingly), representing the report data to read
	 * fields from
	 * @throws JRException if rendering failed
	 * @see #convertReportData
	 */
	public static void renderAsCsv(JasperReport report, Map parameters, Object reportData, Writer writer)
			throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		render(new JRCsvExporter(), print, writer);
	}

	/**
	 * Render a report in CSV format using the supplied report data.
	 * Writes the results to the supplied <code>Writer</code>.
	 * @param report the <code>JasperReport</code> instance to render
	 * @param parameters the parameters to use for rendering
	 * @param writer the <code>Writer</code> to write the rendered report to
	 * @param reportData a <code>JRDataSource</code>, <code>java.util.Collection</code>
	 * or object array (converted accordingly), representing the report data to read
	 * fields from
	 * @param exporterParameters a {@link Map} of {@link JRExporterParameter exporter parameters}
	 * @throws JRException if rendering failed
	 * @see #convertReportData
	 */
	public static void renderAsCsv(JasperReport report, Map parameters, Object reportData, Writer writer,
			Map exporterParameters) throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		JRCsvExporter exporter = new JRCsvExporter();
		exporter.setParameters(exporterParameters);
		render(exporter, print, writer);
	}

	/**
	 * Render a report in HTML format using the supplied report data.
	 * Writes the results to the supplied <code>Writer</code>.
	 * @param report the <code>JasperReport</code> instance to render
	 * @param parameters the parameters to use for rendering
	 * @param writer the <code>Writer</code> to write the rendered report to
	 * @param reportData a <code>JRDataSource</code>, <code>java.util.Collection</code>
	 * or object array (converted accordingly), representing the report data to read
	 * fields from
	 * @throws JRException if rendering failed
	 * @see #convertReportData
	 */
	public static void renderAsHtml(JasperReport report, Map parameters, Object reportData, Writer writer)
			throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		render(new JRHtmlExporter(), print, writer);
	}

	/**
	 * Render a report in HTML format using the supplied report data.
	 * Writes the results to the supplied <code>Writer</code>.
	 * @param report the <code>JasperReport</code> instance to render
	 * @param parameters the parameters to use for rendering
	 * @param writer the <code>Writer</code> to write the rendered report to
	 * @param reportData a <code>JRDataSource</code>, <code>java.util.Collection</code>
	 * or object array (converted accordingly), representing the report data to read
	 * fields from
	 * @param exporterParameters a {@link Map} of {@link JRExporterParameter exporter parameters}
	 * @throws JRException if rendering failed
	 * @see #convertReportData
	 */
	public static void renderAsHtml(JasperReport report, Map parameters, Object reportData, Writer writer,
			Map exporterParameters) throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		JRHtmlExporter exporter = new JRHtmlExporter();
		exporter.setParameters(exporterParameters);
		render(exporter, print, writer);
	}

	/**
	 * Render a report in PDF format using the supplied report data.
	 * Writes the results to the supplied <code>OutputStream</code>.
	 * @param report the <code>JasperReport</code> instance to render
	 * @param parameters the parameters to use for rendering
	 * @param stream the <code>OutputStream</code> to write the rendered report to
	 * @param reportData a <code>JRDataSource</code>, <code>java.util.Collection</code>
	 * or object array (converted accordingly), representing the report data to read
	 * fields from
	 * @throws JRException if rendering failed
	 * @see #convertReportData
	 */
	public static void renderAsPdf(JasperReport report, Map parameters, Object reportData, OutputStream stream)
			throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		render(new JRPdfExporter(), print, stream);
	}

	/**
	 * Render a report in PDF format using the supplied report data.
	 * Writes the results to the supplied <code>OutputStream</code>.
	 * @param report the <code>JasperReport</code> instance to render
	 * @param parameters the parameters to use for rendering
	 * @param stream the <code>OutputStream</code> to write the rendered report to
	 * @param reportData a <code>JRDataSource</code>, <code>java.util.Collection</code>
	 * or object array (converted accordingly), representing the report data to read
	 * fields from
	 * @param exporterParameters a {@link Map} of {@link JRExporterParameter exporter parameters}
	 * @throws JRException if rendering failed
	 * @see #convertReportData
	 */
	public static void renderAsPdf(JasperReport report, Map parameters, Object reportData, OutputStream stream,
			Map exporterParameters) throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		JRPdfExporter exporter = new JRPdfExporter();
		exporter.setParameters(exporterParameters);
		render(exporter, print, stream);
	}

	/**
	 * Render a report in XLS format using the supplied report data.
	 * Writes the results to the supplied <code>OutputStream</code>.
	 * @param report the <code>JasperReport</code> instance to render
	 * @param parameters the parameters to use for rendering
	 * @param stream the <code>OutputStream</code> to write the rendered report to
	 * @param reportData a <code>JRDataSource</code>, <code>java.util.Collection</code>
	 * or object array (converted accordingly), representing the report data to read
	 * fields from
	 * @throws JRException if rendering failed
	 * @see #convertReportData
	 */
	public static void renderAsXls(JasperReport report, Map parameters, Object reportData, OutputStream stream)
			throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		render(new JRXlsExporter(), print, stream);
	}

	/**
	 * Render a report in XLS format using the supplied report data.
	 * Writes the results to the supplied <code>OutputStream</code>.
	 * @param report the <code>JasperReport</code> instance to render
	 * @param parameters the parameters to use for rendering
	 * @param stream the <code>OutputStream</code> to write the rendered report to
	 * @param reportData a <code>JRDataSource</code>, <code>java.util.Collection</code>
	 * or object array (converted accordingly), representing the report data to read
	 * fields from
	 * @param exporterParameters a {@link Map} of {@link JRExporterParameter exporter parameters}
	 * @throws JRException if rendering failed
	 * @see #convertReportData
	 */
	public static void renderAsXls(JasperReport report, Map parameters, Object reportData, OutputStream stream,
			Map exporterParameters) throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		JRXlsExporter exporter = new JRXlsExporter();
		exporter.setParameters(exporterParameters);
		render(exporter, print, stream);
	}

}
