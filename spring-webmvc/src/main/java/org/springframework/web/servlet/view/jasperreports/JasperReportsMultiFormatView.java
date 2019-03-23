/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

/**
 * JasperReports view class that allows for the actual rendering format
 * to be specified at runtime using a parameter contained in the model.
 *
 * <p>This view works on the concept of a format key and a mapping key.
 * The format key is used to pass the mapping key from your {@code Controller}
 * to Spring through as part of the model and the mapping key is used to map
 * a logical format to an actual JasperReports view class.
 *
 * <p>For example, you might add the following code to your {@code Controller}:
 *
 * <pre class="code">
 * Map<String, Object> model = new HashMap<String, Object>();
 * model.put("format", "pdf");</pre>
 *
 * Here {@code format} is the format key and {@code pdf} is the mapping key.
 * When rendering a report, this class looks for a model parameter under the
 * format key, which by default is {@code format}. It then uses the value of
 * this parameter to lookup the actual {@code View} class to use.
 *
 * <p>The default mappings for the format lookup are:
 *
 * <p><ul>
 * <li>{@code csv} - {@code JasperReportsCsvView}</li>
 * <li>{@code html} - {@code JasperReportsHtmlView}</li>
 * <li>{@code pdf} - {@code JasperReportsPdfView}</li>
 * <li>{@code xls} - {@code JasperReportsXlsView}</li>
 * <li>{@code xlsx} - {@code JasperReportsXlsxView}</li> (as of Spring 4.2)
 * </ul>
 *
 * <p>The format key can be changed using the {@code formatKey} property.
 * The applicable key-to-view-class mappings can be configured using the
 * {@code formatMappings} property.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see #setFormatKey
 * @see #setFormatMappings
 */
public class JasperReportsMultiFormatView extends AbstractJasperReportsView {

	/**
	 * Default value used for format key: "format"
	 */
	public static final String DEFAULT_FORMAT_KEY = "format";


	/**
	 * The key of the model parameter that holds the format key.
	 */
	private String formatKey = DEFAULT_FORMAT_KEY;

	/**
	 * Stores the format mappings, with the format discriminator
	 * as key and the corresponding view class as value.
	 */
	private Map<String, Class<? extends AbstractJasperReportsView>> formatMappings;

	/**
	 * Stores the mappings of mapping keys to Content-Disposition header values.
	 */
	private Properties contentDispositionMappings;


  /**
   * Creates a new {@code JasperReportsMultiFormatView} instance
   * with a default set of mappings.
   */
	public JasperReportsMultiFormatView() {
		this.formatMappings = new HashMap<String, Class<? extends AbstractJasperReportsView>>(4);
		this.formatMappings.put("csv", JasperReportsCsvView.class);
		this.formatMappings.put("html", JasperReportsHtmlView.class);
		this.formatMappings.put("pdf", JasperReportsPdfView.class);
		this.formatMappings.put("xls", JasperReportsXlsView.class);
		this.formatMappings.put("xlsx", JasperReportsXlsxView.class);
	}


	/**
	 * Set the key of the model parameter that holds the format discriminator.
	 * Default is "format".
	 */
	public void setFormatKey(String formatKey) {
		this.formatKey = formatKey;
	}

	/**
	 * Set the mappings of format discriminators to view class names.
	 * The default mappings are:
	 * <p><ul>
	 * <li>{@code csv} - {@code JasperReportsCsvView}</li>
	 * <li>{@code html} - {@code JasperReportsHtmlView}</li>
	 * <li>{@code pdf} - {@code JasperReportsPdfView}</li>
	 * <li>{@code xls} - {@code JasperReportsXlsView}</li>
	 * <li>{@code xlsx} - {@code JasperReportsXlsxView}</li> (as of Spring 4.2)
	 * </ul>
	 */
	public void setFormatMappings(Map<String, Class<? extends AbstractJasperReportsView>> formatMappings) {
		if (CollectionUtils.isEmpty(formatMappings)) {
			throw new IllegalArgumentException("'formatMappings' must not be empty");
		}
		this.formatMappings = formatMappings;
	}

	/**
	 * Set the mappings of {@code Content-Disposition} header values to
	 * mapping keys. If specified, Spring will look at these mappings to determine
	 * the value of the {@code Content-Disposition} header for a given
	 * format mapping.
	 */
	public void setContentDispositionMappings(Properties mappings) {
		this.contentDispositionMappings = mappings;
	}

	/**
	 * Return the mappings of {@code Content-Disposition} header values to
	 * mapping keys. Mainly available for configuration through property paths
	 * that specify individual keys.
	 */
	public Properties getContentDispositionMappings() {
		if (this.contentDispositionMappings == null) {
			this.contentDispositionMappings = new Properties();
		}
		return this.contentDispositionMappings;
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	/**
	 * Locates the format key in the model using the configured discriminator key and uses this
	 * key to lookup the appropriate view class from the mappings. The rendering of the
	 * report is then delegated to an instance of that view class.
	 */
	@Override
	protected void renderReport(JasperPrint populatedReport, Map<String, Object> model, HttpServletResponse response)
			throws Exception {

		String format = (String) model.get(this.formatKey);
		if (format == null) {
			throw new IllegalArgumentException("No format found in model");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Rendering report using format mapping key [" + format + "]");
		}

		Class<? extends AbstractJasperReportsView> viewClass = this.formatMappings.get(format);
		if (viewClass == null) {
			throw new IllegalArgumentException("Format discriminator [" + format + "] is not a configured mapping");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Rendering report using view class [" + viewClass.getName() + "]");
		}

		AbstractJasperReportsView view = BeanUtils.instantiateClass(viewClass);
		// Can skip most initialization since all relevant URL processing
		// has been done - just need to convert parameters on the sub view.
		view.setExporterParameters(getExporterParameters());
		view.setConvertedExporterParameters(getConvertedExporterParameters());

		// Prepare response and render report.
		populateContentDispositionIfNecessary(response, format);
		view.renderReport(populatedReport, model, response);
	}

	/**
	 * Adds/overwrites the {@code Content-Disposition} header value with the format-specific
	 * value if the mappings have been specified and a valid one exists for the given format.
	 * @param response the {@code HttpServletResponse} to set the header in
	 * @param format the format key of the mapping
	 * @see #setContentDispositionMappings
	 */
	private void populateContentDispositionIfNecessary(HttpServletResponse response, String format) {
		if (this.contentDispositionMappings != null) {
			String header = this.contentDispositionMappings.getProperty(format);
			if (header != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting Content-Disposition header to: [" + header + "]");
				}
				response.setHeader(HEADER_CONTENT_DISPOSITION, header);
			}
		}
	}

}
