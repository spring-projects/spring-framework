/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataSourceProvider;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.core.io.Resource;
import org.springframework.ui.jasperreports.JasperReportsUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * Base class for all JasperReports views. Applies on-the-fly compilation
 * of report designs as required and coordinates the rendering process.
 * The resource path of the main report needs to be specified as {@code url}.
 *
 * <p>This class is responsible for getting report data from the model that has
 * been provided to the view. The default implementation checks for a model object
 * under the specified {@code reportDataKey} first, then falls back to looking
 * for a value of type {@code JRDataSource}, {@code java.util.Collection},
 * object array (in that order).
 *
 * <p>If no {@code JRDataSource} can be found in the model, then reports will
 * be filled using the configured {@code javax.sql.DataSource} if any. If neither
 * a {@code JRDataSource} or {@code javax.sql.DataSource} is available then
 * an {@code IllegalArgumentException} is raised.
 *
 * <p>Provides support for sub-reports through the {@code subReportUrls} and
 * {@code subReportDataKeys} properties.
 *
 * <p>When using sub-reports, the master report should be configured using the
 * {@code url} property and the sub-reports files should be configured using
 * the {@code subReportUrls} property. Each entry in the {@code subReportUrls}
 * Map corresponds to an individual sub-report. The key of an entry must match up
 * to a sub-report parameter in your report file of type
 * {@code net.sf.jasperreports.engine.JasperReport},
 * and the value of an entry must be the URL for the sub-report file.
 *
 * <p>For sub-reports that require an instance of {@code JRDataSource}, that is,
 * they don't have a hard-coded query for data retrieval, you can include the
 * appropriate data in your model as would with the data source for the parent report.
 * However, you must provide a List of parameter names that need to be converted to
 * {@code JRDataSource} instances for the sub-report via the
 * {@code subReportDataKeys} property. When using {@code JRDataSource}
 * instances for sub-reports, you <i>must</i> specify a value for the
 * {@code reportDataKey} property, indicating the data to use for the main report.
 *
 * <p>Allows for exporter parameters to be configured declatively using the
 * {@code exporterParameters} property. This is a {@code Map} typed
 * property where the key of an entry corresponds to the fully-qualified name
 * of the static field for the {@code JRExporterParameter} and the value
 * of an entry is the value you want to assign to the exporter parameter.
 *
 * <p>Response headers can be controlled via the {@code headers} property. Spring
 * will attempt to set the correct value for the {@code Content-Diposition} header
 * so that reports render correctly in Internet Explorer. However, you can override this
 * setting through the {@code headers} property.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see #setUrl
 * @see #setReportDataKey
 * @see #setSubReportUrls
 * @see #setSubReportDataKeys
 * @see #setHeaders
 * @see #setExporterParameters
 * @see #setJdbcDataSource
 */
public abstract class AbstractJasperReportsView extends AbstractUrlBasedView {

	/**
	 * Constant that defines "Content-Disposition" header.
	 */
	protected static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

	/**
	 * The default Content-Disposition header. Used to make IE play nice.
	 */
	protected static final String CONTENT_DISPOSITION_INLINE = "inline";


	/**
	 * A String key used to lookup the {@code JRDataSource} in the model.
	 */
	private String reportDataKey;

	/**
	 * Stores the paths to any sub-report files used by this top-level report,
	 * along with the keys they are mapped to in the top-level report file.
	 */
	private Properties subReportUrls;

	/**
	 * Stores the names of any data source objects that need to be converted to
	 * {@code JRDataSource} instances and included in the report parameters
	 * to be passed on to a sub-report.
	 */
	private String[] subReportDataKeys;

	/**
	 * Stores the headers to written with each response
	 */
	private Properties headers;

	/**
	 * Stores the exporter parameters passed in by the user as passed in by the user. May be keyed as
	 * {@code String}s with the fully qualified name of the exporter parameter field.
	 */
	private Map<?, ?> exporterParameters = new HashMap<Object, Object>();

	/**
	 * Stores the converted exporter parameters - keyed by {@code JRExporterParameter}.
	 */
	private Map<JRExporterParameter, Object> convertedExporterParameters;

	/**
	 * Stores the {@code DataSource}, if any, used as the report data source.
	 */
	private DataSource jdbcDataSource;

	/**
	 * The {@code JasperReport} that is used to render the view.
	 */
	private JasperReport report;

	/**
	 * Holds mappings between sub-report keys and {@code JasperReport} objects.
	 */
	private Map<String, JasperReport> subReports;


	/**
	 * Set the name of the model attribute that represents the report data.
	 * If not specified, the model map will be searched for a matching value type.
	 * <p>A {@code JRDataSource} will be taken as-is. For other types, conversion
	 * will apply: By default, a {@code java.util.Collection} will be converted
	 * to {@code JRBeanCollectionDataSource}, and an object array to
	 * {@code JRBeanArrayDataSource}.
	 * <p><b>Note:</b> If you pass in a Collection or object array in the model map
	 * for use as plain report parameter, rather than as report data to extract fields
	 * from, you need to specify the key for the actual report data to use, to avoid
	 * mis-detection of report data by type.
	 * @see #convertReportData
	 * @see net.sf.jasperreports.engine.JRDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanArrayDataSource
	 */
	public void setReportDataKey(String reportDataKey) {
		this.reportDataKey = reportDataKey;
	}

	/**
	 * Specify resource paths which must be loaded as instances of
	 * {@code JasperReport} and passed to the JasperReports engine for
	 * rendering as sub-reports, under the same keys as in this mapping.
	 * @param subReports mapping between model keys and resource paths
	 * (Spring resource locations)
	 * @see #setUrl
	 * @see org.springframework.context.ApplicationContext#getResource
	 */
	public void setSubReportUrls(Properties subReports) {
		this.subReportUrls = subReports;
	}

	/**
	 * Set the list of names corresponding to the model parameters that will contain
	 * data source objects for use in sub-reports. Spring will convert these objects
	 * to instances of {@code JRDataSource} where applicable and will then
	 * include the resulting {@code JRDataSource} in the parameters passed into
	 * the JasperReports engine.
	 * <p>The name specified in the list should correspond to an attribute in the
	 * model Map, and to a sub-report data source parameter in your report file.
	 * If you pass in {@code JRDataSource} objects as model attributes,
	 * specifing this list of keys is not required.
	 * <p>If you specify a list of sub-report data keys, it is required to also
	 * specify a {@code reportDataKey} for the main report, to avoid confusion
	 * between the data source objects for the various reports involved.
	 * @param subReportDataKeys list of names for sub-report data source objects
	 * @see #setReportDataKey
	 * @see #convertReportData
	 * @see net.sf.jasperreports.engine.JRDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanArrayDataSource
	 */
	public void setSubReportDataKeys(String[] subReportDataKeys) {
		this.subReportDataKeys = subReportDataKeys;
	}

	/**
	 * Specify the set of headers that are included in each of response.
	 * @param headers the headers to write to each response.
	 */
	public void setHeaders(Properties headers) {
		this.headers = headers;
	}

	/**
	 * Set the exporter parameters that should be used when rendering a view.
	 * @param parameters {@code Map} with the fully qualified field name
	 * of the {@code JRExporterParameter} instance as key
	 * (e.g. "net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI")
	 * and the value you wish to assign to the parameter as value
	 */
	public void setExporterParameters(Map<?, ?> parameters) {
		this.exporterParameters = parameters;
	}

	/**
	 * Return the exporter parameters that this view uses, if any.
	 */
	public Map<?, ?> getExporterParameters() {
		return this.exporterParameters;
	}

	/**
	 * Allows subclasses to populate the converted exporter parameters.
	 */
	protected void setConvertedExporterParameters(Map<JRExporterParameter, Object> convertedExporterParameters) {
		this.convertedExporterParameters = convertedExporterParameters;
	}

	/**
	 * Allows subclasses to retrieve the converted exporter parameters.
	 */
	protected Map<JRExporterParameter, Object> getConvertedExporterParameters() {
		return this.convertedExporterParameters;
	}

	/**
	 * Specify the {@code javax.sql.DataSource} to use for reports with
	 * embedded SQL statements.
	 */
	public void setJdbcDataSource(DataSource jdbcDataSource) {
		this.jdbcDataSource = jdbcDataSource;
	}

	/**
	 * Return the {@code javax.sql.DataSource} that this view uses, if any.
	 */
	protected DataSource getJdbcDataSource() {
		return this.jdbcDataSource;
	}


	/**
	 * JasperReports views do not strictly required a 'url' value.
	 * Alternatively, the {@link #getReport()} template method may be overridden.
	 */
	@Override
	protected boolean isUrlRequired() {
		return false;
	}

	/**
	 * Checks to see that a valid report file URL is supplied in the
	 * configuration. Compiles the report file is necessary.
	 * <p>Subclasses can add custom initialization logic by overriding
	 * the {@link #onInit} method.
	 */
	@Override
	protected final void initApplicationContext() throws ApplicationContextException {
		this.report = loadReport();

		// Load sub reports if required, and check data source parameters.
		if (this.subReportUrls != null) {
			if (this.subReportDataKeys != null && this.subReportDataKeys.length > 0 && this.reportDataKey == null) {
				throw new ApplicationContextException(
						"'reportDataKey' for main report is required when specifying a value for 'subReportDataKeys'");
			}
			this.subReports = new HashMap<String, JasperReport>(this.subReportUrls.size());
			for (Enumeration urls = this.subReportUrls.propertyNames(); urls.hasMoreElements();) {
				String key = (String) urls.nextElement();
				String path = this.subReportUrls.getProperty(key);
				Resource resource = getApplicationContext().getResource(path);
				this.subReports.put(key, loadReport(resource));
			}
		}

		// Convert user-supplied exporterParameters.
		convertExporterParameters();

		if (this.headers == null) {
			this.headers = new Properties();
		}
		if (!this.headers.containsKey(HEADER_CONTENT_DISPOSITION)) {
			this.headers.setProperty(HEADER_CONTENT_DISPOSITION, CONTENT_DISPOSITION_INLINE);
		}

		onInit();
	}

	/**
	 * Subclasses can override this to add some custom initialization logic. Called
	 * by {@link #initApplicationContext()} as soon as all standard initialization logic
	 * has finished executing.
	 * @see #initApplicationContext()
	 */
	protected void onInit() {
	}

	/**
	 * Converts the exporter parameters passed in by the user which may be keyed
	 * by {@code String}s corresponding to the fully qualified name of the
	 * {@code JRExporterParameter} into parameters which are keyed by
	 * {@code JRExporterParameter}.
	 * @see #getExporterParameter(Object)
	 */
	protected final void convertExporterParameters() {
		if (!CollectionUtils.isEmpty(this.exporterParameters)) {
			this.convertedExporterParameters = new HashMap<JRExporterParameter, Object>(this.exporterParameters.size());
			for (Map.Entry<?, ?> entry : this.exporterParameters.entrySet()) {
				JRExporterParameter exporterParameter = getExporterParameter(entry.getKey());
				this.convertedExporterParameters.put(
						exporterParameter, convertParameterValue(exporterParameter, entry.getValue()));
			}
		}
	}

	/**
	 * Convert the supplied parameter value into the actual type required by the
	 * corresponding {@link JRExporterParameter}.
	 * <p>The default implementation simply converts the String values "true" and
	 * "false" into corresponding {@code Boolean} objects, and tries to convert
	 * String values that start with a digit into {@code Integer} objects
	 * (simply keeping them as String if number conversion fails).
	 * @param parameter the parameter key
	 * @param value the parameter value
	 * @return the converted parameter value
	 */
	protected Object convertParameterValue(JRExporterParameter parameter, Object value) {
		if (value instanceof String) {
			String str = (String) value;
			if ("true".equals(str)) {
				return Boolean.TRUE;
			}
			else if ("false".equals(str)) {
				return Boolean.FALSE;
			}
			else if (str.length() > 0 && Character.isDigit(str.charAt(0))) {
				// Looks like a number... let's try.
				try {
					return new Integer(str);
				}
				catch (NumberFormatException ex) {
					// OK, then let's keep it as a String value.
					return str;
				}
			}
		}
		return value;
	}

	/**
	 * Return a {@code JRExporterParameter} for the given parameter object,
	 * converting it from a String if necessary.
	 * @param parameter the parameter object, either a String or a JRExporterParameter
	 * @return a JRExporterParameter for the given parameter object
	 * @see #convertToExporterParameter(String)
	 */
	protected JRExporterParameter getExporterParameter(Object parameter) {
		if (parameter instanceof JRExporterParameter) {
			return (JRExporterParameter) parameter;
		}
		if (parameter instanceof String) {
			return convertToExporterParameter((String) parameter);
		}
		throw new IllegalArgumentException(
				"Parameter [" + parameter + "] is invalid type. Should be either String or JRExporterParameter.");
	}

	/**
	 * Convert the given fully qualified field name to a corresponding
	 * JRExporterParameter instance.
	 * @param fqFieldName the fully qualified field name, consisting
	 * of the class name followed by a dot followed by the field name
	 * (e.g. "net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI")
	 * @return the corresponding JRExporterParameter instance
	 */
	protected JRExporterParameter convertToExporterParameter(String fqFieldName) {
		int index = fqFieldName.lastIndexOf('.');
		if (index == -1 || index == fqFieldName.length()) {
			throw new IllegalArgumentException(
					"Parameter name [" + fqFieldName + "] is not a valid static field. " +
					"The parameter name must map to a static field such as " +
					"[net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI]");
		}
		String className = fqFieldName.substring(0, index);
		String fieldName = fqFieldName.substring(index + 1);

		try {
			Class cls = ClassUtils.forName(className, getApplicationContext().getClassLoader());
			Field field = cls.getField(fieldName);

			if (JRExporterParameter.class.isAssignableFrom(field.getType())) {
				try {
					return (JRExporterParameter) field.get(null);
				}
				catch (IllegalAccessException ex) {
					throw new IllegalArgumentException(
							"Unable to access field [" + fieldName + "] of class [" + className + "]. " +
							"Check that it is static and accessible.");
				}
			}
			else {
				throw new IllegalArgumentException("Field [" + fieldName + "] on class [" + className +
						"] is not assignable from JRExporterParameter - check the type of this field.");
			}
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException(
					"Class [" + className + "] in key [" + fqFieldName + "] could not be found.");
		}
		catch (NoSuchFieldException ex) {
			throw new IllegalArgumentException("Field [" + fieldName + "] in key [" + fqFieldName +
					"] could not be found on class [" + className + "].");
		}
	}

	/**
	 * Load the main {@code JasperReport} from the specified {@code Resource}.
	 * If the {@code Resource} points to an uncompiled report design file then the
	 * report file is compiled dynamically and loaded into memory.
	 * @return a {@code JasperReport} instance, or {@code null} if no main
	 * report has been statically defined
	 */
	protected JasperReport loadReport() {
		String url = getUrl();
		if (url == null) {
			return null;
		}
		Resource mainReport = getApplicationContext().getResource(url);
		return loadReport(mainReport);
	}

	/**
	 * Loads a {@code JasperReport} from the specified {@code Resource}.
	 * If the {@code Resource} points to an uncompiled report design file then
	 * the report file is compiled dynamically and loaded into memory.
	 * @param resource the {@code Resource} containing the report definition or design
	 * @return a {@code JasperReport} instance
	 */
	protected final JasperReport loadReport(Resource resource) {
		try {
			String filename = resource.getFilename();
			if (filename != null) {
				if (filename.endsWith(".jasper")) {
					// Load pre-compiled report.
					if (logger.isInfoEnabled()) {
						logger.info("Loading pre-compiled Jasper Report from " + resource);
					}
					InputStream is = resource.getInputStream();
					try {
						return (JasperReport) JRLoader.loadObject(is);
					}
					finally {
						is.close();
					}
				}
				else if (filename.endsWith(".jrxml")) {
					// Compile report on-the-fly.
					if (logger.isInfoEnabled()) {
						logger.info("Compiling Jasper Report loaded from " + resource);
					}
					InputStream is = resource.getInputStream();
					try {
						JasperDesign design = JRXmlLoader.load(is);
						return JasperCompileManager.compileReport(design);
					}
					finally {
						is.close();
					}
				}
			}
			throw new IllegalArgumentException(
					"Report filename [" + filename + "] must end in either .jasper or .jrxml");
		}
		catch (IOException ex) {
			throw new ApplicationContextException(
					"Could not load JasperReports report from " + resource, ex);
		}
		catch (JRException ex) {
			throw new ApplicationContextException(
					"Could not parse JasperReports report from " + resource, ex);
		}
	}


	/**
	 * Finds the report data to use for rendering the report and then invokes the
	 * {@link #renderReport} method that should be implemented by the subclass.
	 * @param model the model map, as passed in for view rendering. Must contain
	 * a report data value that can be converted to a {@code JRDataSource},
	 * acccording to the rules of the {@link #fillReport} method.
	 */
	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		if (this.subReports != null) {
			// Expose sub-reports as model attributes.
			model.putAll(this.subReports);

			// Transform any collections etc into JRDataSources for sub reports.
			if (this.subReportDataKeys != null) {
				for (String key : this.subReportDataKeys) {
					model.put(key, convertReportData(model.get(key)));
				}
			}
		}

		// Expose Spring-managed Locale and MessageSource.
		exposeLocalizationContext(model, request);

		// Fill the report.
		JasperPrint filledReport = fillReport(model);
		postProcessReport(filledReport, model);

		// Prepare response and render report.
		populateHeaders(response);
		renderReport(filledReport, model, response);
	}

	/**
	 * Expose current Spring-managed Locale and MessageSource to JasperReports i18n
	 * ($R expressions etc). The MessageSource should only be exposed as JasperReports
	 * resource bundle if no such bundle is defined in the report itself.
	 * <p>The default implementation exposes the Spring RequestContext Locale and a
	 * MessageSourceResourceBundle adapter for the Spring ApplicationContext,
	 * analogous to the {@code JstlUtils.exposeLocalizationContext} method.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see org.springframework.context.support.MessageSourceResourceBundle
	 * @see #getApplicationContext()
	 * @see net.sf.jasperreports.engine.JRParameter#REPORT_LOCALE
	 * @see net.sf.jasperreports.engine.JRParameter#REPORT_RESOURCE_BUNDLE
	 * @see org.springframework.web.servlet.support.JstlUtils#exposeLocalizationContext
	 */
	protected void exposeLocalizationContext(Map<String, Object> model, HttpServletRequest request) {
		RequestContext rc = new RequestContext(request, getServletContext());
		Locale locale = rc.getLocale();
		if (!model.containsKey(JRParameter.REPORT_LOCALE)) {
			model.put(JRParameter.REPORT_LOCALE, locale);
		}
		TimeZone timeZone = rc.getTimeZone();
		if (timeZone != null && !model.containsKey(JRParameter.REPORT_TIME_ZONE)) {
			model.put(JRParameter.REPORT_TIME_ZONE, timeZone);
		}
		JasperReport report = getReport();
		if ((report == null || report.getResourceBundle() == null) &&
				!model.containsKey(JRParameter.REPORT_RESOURCE_BUNDLE)) {
			model.put(JRParameter.REPORT_RESOURCE_BUNDLE,
					new MessageSourceResourceBundle(rc.getMessageSource(), locale));
		}
	}

	/**
	 * Create a populated {@code JasperPrint} instance from the configured
	 * {@code JasperReport} instance.
	 * <p>By default, this method will use any {@code JRDataSource} instance
	 * (or wrappable {@code Object}) that can be located using {@link #setReportDataKey},
	 * a lookup for type {@code JRDataSource} in the model Map, or a special value
	 * retrieved via {@link #getReportData}.
	 * <p>If no {@code JRDataSource} can be found, this method will use a JDBC
	 * {@code Connection} obtained from the configured {@code javax.sql.DataSource}
	 * (or a DataSource attribute in the model). If no JDBC DataSource can be found
	 * either, the JasperReports engine will be invoked with plain model Map,
	 * assuming that the model contains parameters that identify the source
	 * for report data (e.g. Hibernate or JPA queries).
	 * @param model the model for this request
	 * @throws IllegalArgumentException if no {@code JRDataSource} can be found
	 * and no {@code javax.sql.DataSource} is supplied
	 * @throws SQLException if there is an error when populating the report using
	 * the {@code javax.sql.DataSource}
	 * @throws JRException if there is an error when populating the report using
	 * a {@code JRDataSource}
	 * @return the populated {@code JasperPrint} instance
	 * @see #getReportData
	 * @see #setJdbcDataSource
	 */
	protected JasperPrint fillReport(Map<String, Object> model) throws Exception {
		// Determine main report.
		JasperReport report = getReport();
		if (report == null) {
			throw new IllegalStateException("No main report defined for 'fillReport' - " +
					"specify a 'url' on this view or override 'getReport()' or 'fillReport(Map)'");
		}

		JRDataSource jrDataSource = null;
		DataSource jdbcDataSourceToUse = null;

		// Try model attribute with specified name.
		if (this.reportDataKey != null) {
			Object reportDataValue = model.get(this.reportDataKey);
			if (reportDataValue instanceof DataSource) {
				jdbcDataSourceToUse = (DataSource) reportDataValue;
			}
			else {
				jrDataSource = convertReportData(reportDataValue);
			}
		}
		else {
			Collection values = model.values();
			jrDataSource = CollectionUtils.findValueOfType(values, JRDataSource.class);
			if (jrDataSource == null) {
				JRDataSourceProvider provider = CollectionUtils.findValueOfType(values, JRDataSourceProvider.class);
				if (provider != null) {
					jrDataSource = createReport(provider);
				}
				else {
					jdbcDataSourceToUse = CollectionUtils.findValueOfType(values, DataSource.class);
					if (jdbcDataSourceToUse == null) {
						jdbcDataSourceToUse = this.jdbcDataSource;
					}
				}
			}
		}

		if (jdbcDataSourceToUse != null) {
			return doFillReport(report, model, jdbcDataSourceToUse);
		}
		else {
			// Determine JRDataSource for main report.
			if (jrDataSource == null) {
				jrDataSource = getReportData(model);
			}
			if (jrDataSource != null) {
				// Use the JasperReports JRDataSource.
				if (logger.isDebugEnabled()) {
					logger.debug("Filling report with JRDataSource [" + jrDataSource + "]");
				}
				return JasperFillManager.fillReport(report, model, jrDataSource);
			}
			else {
				// Assume that the model contains parameters that identify
				// the source for report data (e.g. Hibernate or JPA queries).
				logger.debug("Filling report with plain model");
				return JasperFillManager.fillReport(report, model);
			}
		}
	}

	/**
	 * Fill the given report using the given JDBC DataSource and model.
	 */
	private JasperPrint doFillReport(JasperReport report, Map<String, Object> model, DataSource ds) throws Exception {
		// Use the JDBC DataSource.
		if (logger.isDebugEnabled()) {
			logger.debug("Filling report using JDBC DataSource [" + ds + "]");
		}
		Connection con = ds.getConnection();
		try {
			return JasperFillManager.fillReport(report, model, con);
		}
		finally {
			try {
				con.close();
			}
			catch (Throwable ex) {
				logger.debug("Could not close JDBC Connection", ex);
			}
		}
	}

	/**
	 * Populates the headers in the {@code HttpServletResponse} with the
	 * headers supplied by the user.
	 */
	private void populateHeaders(HttpServletResponse response) {
		// Apply the headers to the response.
		for (Enumeration en = this.headers.propertyNames(); en.hasMoreElements();) {
			String key = (String) en.nextElement();
			response.addHeader(key, this.headers.getProperty(key));
		}
	}

	/**
	 * Determine the {@code JasperReport} to fill.
	 * Called by {@link #fillReport}.
	 * <p>The default implementation returns the report as statically configured
	 * through the 'url' property (and loaded by {@link #loadReport()}).
	 * Can be overridden in subclasses in order to dynamically obtain a
	 * {@code JasperReport} instance. As an alternative, consider
	 * overriding the {@link #fillReport} template method itself.
	 * @return an instance of {@code JasperReport}
	 */
	protected JasperReport getReport() {
		return this.report;
	}

	/**
	 * Create an appropriate {@code JRDataSource} for passed-in report data.
	 * Called by {@link #fillReport} when its own lookup steps were not successful.
	 * <p>The default implementation looks for a value of type {@code java.util.Collection}
	 * or object array (in that order). Can be overridden in subclasses.
	 * @param model the model map, as passed in for view rendering
	 * @return the {@code JRDataSource} or {@code null} if the data source is not found
	 * @see #getReportDataTypes
	 * @see #convertReportData
	 */
	protected JRDataSource getReportData(Map<String, Object> model) {
		// Try to find matching attribute, of given prioritized types.
		Object value = CollectionUtils.findValueOfType(model.values(), getReportDataTypes());
		return (value != null ? convertReportData(value) : null);
	}

	/**
	 * Convert the given report data value to a {@code JRDataSource}.
	 * <p>The default implementation delegates to {@code JasperReportUtils} unless
	 * the report data value is an instance of {@code JRDataSourceProvider}.
	 * A {@code JRDataSource}, {@code JRDataSourceProvider},
	 * {@code java.util.Collection} or object array is detected.
	 * {@code JRDataSource}s are returned as is, whilst {@code JRDataSourceProvider}s
	 * are used to create an instance of {@code JRDataSource} which is then returned.
	 * The latter two are converted to {@code JRBeanCollectionDataSource} or
	 * {@code JRBeanArrayDataSource}, respectively.
	 * @param value the report data value to convert
	 * @return the JRDataSource
	 * @throws IllegalArgumentException if the value could not be converted
	 * @see org.springframework.ui.jasperreports.JasperReportsUtils#convertReportData
	 * @see net.sf.jasperreports.engine.JRDataSource
	 * @see net.sf.jasperreports.engine.JRDataSourceProvider
	 * @see net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
	 * @see net.sf.jasperreports.engine.data.JRBeanArrayDataSource
	 */
	protected JRDataSource convertReportData(Object value) throws IllegalArgumentException {
		if (value instanceof JRDataSourceProvider) {
			return createReport((JRDataSourceProvider) value);
		}
		else {
			return JasperReportsUtils.convertReportData(value);
		}
	}

	/**
	 * Create a report using the given provider.
	 * @param provider the JRDataSourceProvider to use
	 * @return the created report
	 */
	protected JRDataSource createReport(JRDataSourceProvider provider) {
		try {
			JasperReport report = getReport();
			if (report == null) {
				throw new IllegalStateException("No main report defined for JRDataSourceProvider - " +
						"specify a 'url' on this view or override 'getReport()'");
			}
			return provider.create(report);
		}
		catch (JRException ex) {
			throw new IllegalArgumentException("Supplied JRDataSourceProvider is invalid", ex);
		}
	}

	/**
	 * Return the value types that can be converted to a {@code JRDataSource},
	 * in prioritized order. Should only return types that the
	 * {@link #convertReportData} method is actually able to convert.
	 * <p>Default value types are: {@code java.util.Collection} and {@code Object} array.
	 * @return the value types in prioritized order
	 */
	protected Class[] getReportDataTypes() {
		return new Class[] {Collection.class, Object[].class};
	}


	/**
	 * Template method to be overridden for custom post-processing of the
	 * populated report. Invoked after filling but before rendering.
	 * <p>The default implementation is empty.
	 * @param populatedReport the populated {@code JasperPrint}
	 * @param model the map containing report parameters
	 * @throws Exception if post-processing failed
	 */
	protected void postProcessReport(JasperPrint populatedReport, Map<String, Object> model) throws Exception {
	}

	/**
	 * Subclasses should implement this method to perform the actual rendering process.
	 * <p>Note that the content type has not been set yet: Implementors should build
	 * a content type String and set it via {@code response.setContentType}.
	 * If necessary, this can include a charset clause for a specific encoding.
	 * The latter will only be necessary for textual output onto a Writer, and only
	 * in case of the encoding being specified in the JasperReports exporter parameters.
	 * <p><b>WARNING:</b> Implementors should not use {@code response.setCharacterEncoding}
	 * unless they are willing to depend on Servlet API 2.4 or higher. Prefer a
	 * concatenated content type String with a charset clause instead.
	 * @param populatedReport the populated {@code JasperPrint} to render
	 * @param model the map containing report parameters
	 * @param response the HTTP response the report should be rendered to
	 * @throws Exception if rendering failed
	 * @see #getContentType()
	 * @see javax.servlet.ServletResponse#setContentType
	 * @see javax.servlet.ServletResponse#setCharacterEncoding
	 */
	protected abstract void renderReport(
			JasperPrint populatedReport, Map<String, Object> model, HttpServletResponse response)
			throws Exception;

}
