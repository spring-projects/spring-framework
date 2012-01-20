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

import net.sf.jasperreports.engine.JRExporter;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * Configurable JasperReports View, allowing to specify the JasperReports exporter
 * to be specified through bean properties rather than through the view class name.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see JasperReportsCsvView
 * @see JasperReportsHtmlView
 * @see JasperReportsPdfView
 * @see JasperReportsXlsView
 */
public class ConfigurableJasperReportsView extends AbstractJasperReportsSingleFormatView {

	private Class<? extends JRExporter> exporterClass;

	private boolean useWriter = true;


	/**
	 * Set the {@link JRExporter} implementation <code>Class</code> to use. Throws
	 * {@link IllegalArgumentException} if the <code>Class</code> doesn't implement
	 * {@link JRExporter}. Required setting, as it does not have a default.
	 */
	public void setExporterClass(Class<? extends JRExporter> exporterClass) {
		Assert.isAssignable(JRExporter.class, exporterClass);
		this.exporterClass = exporterClass;
	}

	/**
	 * Specifies whether or not the {@link JRExporter} writes to the {@link java.io.PrintWriter}
	 * of the associated with the request (<code>true</code>) or whether it writes directly to the
	 * {@link java.io.InputStream} of the request (<code>false</code>). Default is <code>true</code>.
	 */
	public void setUseWriter(boolean useWriter) {
		this.useWriter = useWriter;
	}

	/**
	 * Checks that the {@link #setExporterClass(Class) exporterClass} property is specified.
	 */
	@Override
	protected void onInit() {
		if (this.exporterClass == null) {
			throw new IllegalArgumentException("exporterClass is required");
		}
	}


	/**
	 * Returns a new instance of the specified {@link JRExporter} class.
	 * @see #setExporterClass(Class)
	 * @see BeanUtils#instantiateClass(Class)
	 */
	@Override
	protected JRExporter createExporter() {
		return BeanUtils.instantiateClass(this.exporterClass);
	}

	/**
	 * Indicates how the {@link JRExporter} should render its data.
	 * @see #setUseWriter(boolean)
	 */
	@Override
	protected boolean useWriter() {
		return this.useWriter;
	}

}
