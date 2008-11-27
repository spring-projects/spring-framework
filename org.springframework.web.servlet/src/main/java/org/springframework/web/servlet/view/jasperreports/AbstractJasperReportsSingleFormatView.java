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

package org.springframework.web.servlet.view.jasperreports;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;

import org.springframework.ui.jasperreports.JasperReportsUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.WebUtils;

/**
 * Extends <code>AbstractJasperReportsView</code> to provide basic rendering logic
 * for views that use a fixed format, e.g. always PDF or always HTML.
 *
 * <p>Subclasses need to implement two template methods: <code>createExporter</code>
 * to create a JasperReports exporter for a specific output format, and
 * <code>useWriter</code> to determine whether to write text or binary content.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see #createExporter()
 * @see #useWriter()
 */
public abstract class AbstractJasperReportsSingleFormatView extends AbstractJasperReportsView {

	@Override
	protected boolean generatesDownloadContent() {
		return !useWriter();
	}

	/**
	 * Perform rendering for a single Jasper Reports exporter, that is,
	 * for a pre-defined output format.
	 */
	@Override
	protected void renderReport(JasperPrint populatedReport, Map<String, Object> model, HttpServletResponse response)
			throws Exception {

		JRExporter exporter = createExporter();

		Map mergedExporterParameters = getConvertedExporterParameters();
		if (!CollectionUtils.isEmpty(mergedExporterParameters)) {
			exporter.setParameters(mergedExporterParameters);
		}

		if (useWriter()) {
			renderReportUsingWriter(exporter, populatedReport, response);
		}
		else {
			renderReportUsingOutputStream(exporter, populatedReport, response);
		}
	}

	/**
	 * We need to write text to the response Writer.
	 * @param exporter the JasperReports exporter to use
	 * @param populatedReport the populated <code>JasperPrint</code> to render
	 * @param response the HTTP response the report should be rendered to
	 * @throws Exception if rendering failed
	 */
	protected void renderReportUsingWriter(
			JRExporter exporter, JasperPrint populatedReport, HttpServletResponse response) throws Exception {

		// Copy the encoding configured for the report into the response.
		String contentType = getContentType();
		String encoding = (String) exporter.getParameter(JRExporterParameter.CHARACTER_ENCODING);
		if (encoding != null) {
			// Only apply encoding if content type is specified but does not contain charset clause already.
			if (contentType != null && !contentType.toLowerCase().contains(WebUtils.CONTENT_TYPE_CHARSET_PREFIX)) {
				contentType = contentType + WebUtils.CONTENT_TYPE_CHARSET_PREFIX + encoding;
			}
		}
		response.setContentType(contentType);

		// Render report into HttpServletResponse's Writer.
		JasperReportsUtils.render(exporter, populatedReport, response.getWriter());
	}

	/**
	 * We need to write binary output to the response OutputStream.
	 * @param exporter the JasperReports exporter to use
	 * @param populatedReport the populated <code>JasperPrint</code> to render
	 * @param response the HTTP response the report should be rendered to
	 * @throws Exception if rendering failed
	 */
	protected void renderReportUsingOutputStream(
			JRExporter exporter, JasperPrint populatedReport, HttpServletResponse response) throws Exception {

		// IE workaround: write into byte array first.
		ByteArrayOutputStream baos = createTemporaryOutputStream();
		JasperReportsUtils.render(exporter, populatedReport, baos);
		writeToResponse(response, baos);
	}


	/**
	 * Create a JasperReports exporter for a specific output format,
	 * which will be used to render the report to the HTTP response.
	 * <p>The <code>useWriter</code> method determines whether the
	 * output will be written as text or as binary content.
	 * @see #useWriter()
	 */
	protected abstract JRExporter createExporter();

	/**
	 * Return whether to use a <code>java.io.Writer</code> to write text content
	 * to the HTTP response. Else, a <code>java.io.OutputStream</code> will be used,
	 * to write binary content to the response.
	 * @see javax.servlet.ServletResponse#getWriter()
	 * @see javax.servlet.ServletResponse#getOutputStream()
	 */
	protected abstract boolean useWriter();

}
