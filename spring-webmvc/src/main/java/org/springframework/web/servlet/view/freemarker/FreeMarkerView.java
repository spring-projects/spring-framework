/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.core.ParseException;
import freemarker.ext.jakarta.jsp.TaglibFactory;
import freemarker.ext.jakarta.servlet.AllHttpScopesHashModel;
import freemarker.ext.jakarta.servlet.FreemarkerServlet;
import freemarker.ext.jakarta.servlet.HttpRequestHashModel;
import freemarker.ext.jakarta.servlet.HttpRequestParametersHashModel;
import freemarker.ext.jakarta.servlet.HttpSessionHashModel;
import freemarker.ext.jakarta.servlet.ServletContextHashModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * View using the FreeMarker template engine.
 *
 * <p>Exposes the following configuration properties:
 * <ul>
 * <li><b>{@link #setUrl(String) url}</b>: the location of the FreeMarker template
 * relative to the FreeMarker template context (directory).</li>
 * <li><b>{@link #setEncoding(String) encoding}</b>: the encoding used to decode
 * byte sequences to character sequences when reading the FreeMarker template file.
 * Default is determined by the FreeMarker {@link Configuration}.</li>
 * <li><b>{@link #setContentType(String) contentType}</b>: the content type of the
 * rendered response. Defaults to {@code "text/html;charset=ISO-8859-1"} but may
 * need to be set to a value that corresponds to the actual generated content
 * type (see note below).</li>
 * </ul>
 *
 * <p>Depends on a single {@link FreeMarkerConfig} object such as
 * {@link FreeMarkerConfigurer} being accessible in the current web application
 * context. Alternatively the FreeMarker {@link Configuration} can be set directly
 * via {@link #setConfiguration}.
 *
 * <p><b>Note:</b> To ensure that the correct encoding is used when rendering the
 * response, set the {@linkplain #setContentType(String) content type} with an
 * appropriate {@code charset} attribute &mdash; for example,
 * {@code "text/html;charset=UTF-8"}. When using {@link FreeMarkerViewResolver}
 * to create the view for you, set the
 * {@linkplain FreeMarkerViewResolver#setContentType(String) content type}
 * directly in the {@code FreeMarkerViewResolver}; however, as of Spring Framework
 * 6.2, it is no longer necessary to explicitly set the content type in the
 * {@code FreeMarkerViewResolver} if you have set an explicit encoding via either
 * {@link #setEncoding(String)}, {@link FreeMarkerConfigurer#setDefaultEncoding(String)},
 * or {@link Configuration#setDefaultEncoding(String)}.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3.33 or higher.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 03.03.2004
 * @see #setUrl
 * @see #setExposeSpringMacroHelpers
 * @see #setEncoding
 * @see #setConfiguration
 * @see FreeMarkerConfig
 * @see FreeMarkerConfigurer
 */
public class FreeMarkerView extends AbstractTemplateView {

	@Nullable
	private String encoding;

	@Nullable
	private Configuration configuration;

	@Nullable
	private TaglibFactory taglibFactory;

	@Nullable
	private ServletContextHashModel servletContextHashModel;


	/**
	 * Set the encoding used to decode byte sequences to character sequences when
	 * reading the FreeMarker template file for this view.
	 * <p>Defaults to {@code null} to signal that the FreeMarker
	 * {@link Configuration} should be used to determine the encoding.
	 * <p>A non-null encoding will override the default encoding determined by
	 * the FreeMarker {@code Configuration}.
	 * <p>If the encoding is not explicitly set here or in the FreeMarker
	 * {@code Configuration}, FreeMarker will read template files using the platform
	 * file encoding (defined by the JVM system property {@code file.encoding})
	 * or UTF-8 if the platform file encoding is undefined.
	 * <p>It's recommended to specify the encoding in the FreeMarker {@code Configuration}
	 * rather than per template if all your templates share a common encoding.
	 * <p>See the note in the {@linkplain FreeMarkerView class-level documentation}
	 * for details regarding the encoding used to render the response.
	 * @see freemarker.template.Configuration#setDefaultEncoding
	 * @see #setCharset(Charset)
	 * @see #getEncoding()
	 * @see #setContentType(String)
	 */
	public void setEncoding(@Nullable String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Set the {@link Charset} used to decode byte sequences to character sequences
	 * when reading the FreeMarker template file for this view.
	 * <p>See {@link #setEncoding(String)} for details.
	 * @since 6.2
	 * @see java.nio.charset.StandardCharsets
	 */
	public void setCharset(@Nullable Charset charset) {
		this.encoding = (charset != null ? charset.name() : null);
	}

	/**
	 * Get the encoding used to decode byte sequences to character sequences
	 * when reading the FreeMarker template file for this view, or {@code null}
	 * to signal that the FreeMarker {@link Configuration} should be used to
	 * determine the encoding.
	 * @see #setEncoding(String)
	 */
	@Nullable
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * Set the FreeMarker {@link Configuration} to be used by this view.
	 * <p>If not set, the default lookup will occur: a single {@link FreeMarkerConfig}
	 * is expected in the current web application context, with any bean name.
	 * <strong>Note:</strong> using this method will cause a new instance of {@link TaglibFactory}
	 * to created for every single {@link FreeMarkerView} instance. This can be quite expensive
	 * in terms of memory and initial CPU usage. In production, it is recommended that you use
	 * a {@link FreeMarkerConfig} which exposes a single shared {@link TaglibFactory}.
	 */
	public void setConfiguration(@Nullable Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Return the FreeMarker {@link Configuration} used by this view.
	 */
	@Nullable
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Obtain the FreeMarker {@link Configuration} for actual use.
	 * @return the FreeMarker configuration (never {@code null})
	 * @throws IllegalStateException in case of no Configuration object set
	 * @since 5.0
	 */
	protected Configuration obtainConfiguration() {
		Configuration configuration = getConfiguration();
		Assert.state(configuration != null, "No Configuration set");
		return configuration;
	}


	/**
	 * Invoked on startup. Looks for a single {@link FreeMarkerConfig} bean to
	 * find the relevant {@link Configuration} for this view.
	 * <p>Checks that the template for the default Locale can be found:
	 * FreeMarker will check non-Locale-specific templates if a
	 * locale-specific one is not found.
	 * @see freemarker.cache.TemplateCache#getTemplate
	 */
	@Override
	protected void initServletContext(ServletContext servletContext) throws BeansException {
		if (getConfiguration() != null) {
			this.taglibFactory = new TaglibFactory(servletContext);
		}
		else {
			FreeMarkerConfig config = autodetectConfiguration();
			setConfiguration(config.getConfiguration());
			this.taglibFactory = config.getTaglibFactory();
		}

		GenericServlet servlet = new GenericServletAdapter();
		try {
			servlet.init(new DelegatingServletConfig());
		}
		catch (ServletException ex) {
			throw new BeanInitializationException("Initialization of GenericServlet adapter failed", ex);
		}
		this.servletContextHashModel = new ServletContextHashModel(servlet, getObjectWrapper());
	}

	/**
	 * Autodetect a {@link FreeMarkerConfig} object via the {@code ApplicationContext}.
	 * @return the {@code FreeMarkerConfig} instance to use for FreeMarkerViews
	 * @throws BeansException if no {@link FreeMarkerConfig} bean could be found
	 * @see #getApplicationContext
	 * @see #setConfiguration
	 */
	protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), FreeMarkerConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single FreeMarkerConfig bean in this web application context " +
					"(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.", ex);
		}
	}

	/**
	 * Return the configured FreeMarker {@link ObjectWrapper}, or a default
	 * wrapper if none specified.
	 * @see freemarker.template.Configuration#getObjectWrapper()
	 */
	protected ObjectWrapper getObjectWrapper() {
		ObjectWrapper ow = obtainConfiguration().getObjectWrapper();
		return (ow != null ? ow :
				new DefaultObjectWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
	}

	/**
	 * Check that the FreeMarker template used for this view exists and is valid.
	 * <p>Can be overridden to customize the behavior, for example in case of
	 * multiple templates to be rendered into a single view.
	 */
	@Override
	public boolean checkResource(Locale locale) throws Exception {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		try {
			// Check that we can get the template, even if we might subsequently get it again.
			getTemplate(url, locale);
			return true;
		}
		catch (FileNotFoundException ex) {
			// Allow for ViewResolver chaining...
			return false;
		}
		catch (ParseException ex) {
			throw new ApplicationContextException("Failed to parse [" + url + "]", ex);
		}
		catch (IOException ex) {
			throw new ApplicationContextException("Failed to load [" + url + "]", ex);
		}
	}


	/**
	 * Process the model map by merging it with the FreeMarker template.
	 * <p>Output is directed to the servlet response.
	 * <p>This method can be overridden if custom behavior is needed.
	 */
	@Override
	protected void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		exposeHelpers(model, request);
		doRender(model, request, response);
	}

	/**
	 * Expose helpers unique to each rendering operation. This is necessary so that
	 * different rendering operations can't overwrite each other's formats etc.
	 * <p>Called by {@code renderMergedTemplateModel}. The default implementation
	 * is empty. This method can be overridden to add custom helpers to the model.
	 * @param model the model that will be passed to the template at merge time
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding information to the context
	 * @see #renderMergedTemplateModel
	 */
	protected void exposeHelpers(Map<String, Object> model, HttpServletRequest request) throws Exception {
	}

	/**
	 * Render the FreeMarker view to the given response, using the given model
	 * map which contains the complete template model to use.
	 * <p>The default implementation renders the template specified by the "url"
	 * bean property, retrieved via {@code getTemplate}. It delegates to the
	 * {@code processTemplate} method to merge the template instance with
	 * the given template model.
	 * <p>Adds the standard Freemarker hash models to the model: request parameters,
	 * request, session and application (ServletContext), as well as the JSP tag
	 * library hash model.
	 * <p>Can be overridden to customize the behavior, for example to render
	 * multiple templates into a single view.
	 * @param model the model to use for rendering
	 * @param request current HTTP request
	 * @param response current servlet response
	 * @throws IOException if the template file could not be retrieved
	 * @throws Exception if rendering failed
	 * @see #setUrl
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see #getTemplate(java.util.Locale)
	 * @see #processTemplate
	 * @see freemarker.ext.servlet.FreemarkerServlet
	 */
	protected void doRender(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		// Expose model to JSP tags (as request attributes).
		exposeModelAsRequestAttributes(model, request);
		// Expose FreeMarker hash model.
		SimpleHash fmModel = buildTemplateModel(model, request, response);

		// Grab the locale-specific version of the template.
		Locale locale = RequestContextUtils.getLocale(request);
		processTemplate(getTemplate(locale), fmModel, response);
	}

	/**
	 * Build a FreeMarker template model for the given model Map.
	 * <p>The default implementation builds a {@link AllHttpScopesHashModel}.
	 * @param model the model to use for rendering
	 * @param request current HTTP request
	 * @param response current servlet response
	 * @return the FreeMarker template model, as a {@link SimpleHash} or subclass thereof
	 */
	protected SimpleHash buildTemplateModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) {

		AllHttpScopesHashModel fmModel = new AllHttpScopesHashModel(getObjectWrapper(), getServletContext(), request);
		fmModel.put(FreemarkerServlet.KEY_JSP_TAGLIBS, this.taglibFactory);
		fmModel.put(FreemarkerServlet.KEY_APPLICATION, this.servletContextHashModel);
		fmModel.put(FreemarkerServlet.KEY_SESSION, buildSessionModel(request, response));
		fmModel.put(FreemarkerServlet.KEY_REQUEST, new HttpRequestHashModel(request, response, getObjectWrapper()));
		fmModel.put(FreemarkerServlet.KEY_REQUEST_PARAMETERS, new HttpRequestParametersHashModel(request));
		fmModel.putAll(model);
		return fmModel;
	}

	/**
	 * Build a FreeMarker {@link HttpSessionHashModel} for the given request,
	 * detecting whether a session already exists and reacting accordingly.
	 * @param request current HTTP request
	 * @param response current servlet response
	 * @return the FreeMarker HttpSessionHashModel
	 */
	private HttpSessionHashModel buildSessionModel(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			return new HttpSessionHashModel(session, getObjectWrapper());
		}
		else {
			return new HttpSessionHashModel(null, request, response, getObjectWrapper());
		}
	}

	/**
	 * Retrieve the FreeMarker {@link Template} to be rendered by this view, for
	 * the specified locale and using the {@linkplain #setEncoding(String) configured
	 * encoding} if set.
	 * <p>By default, the template specified by the "url" bean property will be retrieved.
	 * @param locale the current locale
	 * @return the FreeMarker {@code Template} to render
	 * @throws IOException if the template file could not be retrieved
	 * @see #setUrl
	 * @see #getTemplate(String, java.util.Locale)
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		return getTemplate(url, locale);
	}

	/**
	 * Retrieve the FreeMarker {@link Template} to be rendered by this view, for
	 * the specified name and locale and using the {@linkplain #setEncoding(String)
	 * configured encoding} if set.
	 * <p>Can be called by subclasses to retrieve a specific template,
	 * for example to render multiple templates into a single view.
	 * @param name the file name of the desired template
	 * @param locale the current locale
	 * @return the FreeMarker template
	 * @throws IOException if the template file could not be retrieved
	 * @see #setEncoding(String)
	 */
	protected Template getTemplate(String name, Locale locale) throws IOException {
		return (getEncoding() != null ?
				obtainConfiguration().getTemplate(name, locale, getEncoding()) :
				obtainConfiguration().getTemplate(name, locale));
	}

	/**
	 * Process the FreeMarker template and write the result to the response.
	 * <p>As of Spring Framework 6.2, this method sets the
	 * {@linkplain Environment#setOutputEncoding(String) output encoding} of the
	 * FreeMarker {@link Environment} to the character encoding of the supplied
	 * {@link HttpServletResponse}.
	 * <p>Can be overridden to customize the behavior.
	 * @param template the template to process
	 * @param model the model for the template
	 * @param response servlet response (use this to get the OutputStream or Writer)
	 * @throws IOException if the template file could not be retrieved
	 * @throws TemplateException if thrown by FreeMarker
	 * @see freemarker.template.Template#createProcessingEnvironment(Object, java.io.Writer)
	 * @see freemarker.core.Environment#process()
	 */
	protected void processTemplate(Template template, SimpleHash model, HttpServletResponse response)
			throws IOException, TemplateException {

		Environment env = template.createProcessingEnvironment(model, response.getWriter());
		env.setOutputEncoding(response.getCharacterEncoding());
		env.process();
	}


	/**
	 * Simple adapter class that extends {@link GenericServlet}.
	 * Needed for JSP access in FreeMarker.
	 */
	@SuppressWarnings("serial")
	private static class GenericServletAdapter extends GenericServlet {

		@Override
		public void service(ServletRequest servletRequest, ServletResponse servletResponse) {
			// no-op
		}
	}


	/**
	 * Internal implementation of the {@link ServletConfig} interface,
	 * to be passed to the servlet adapter.
	 */
	private class DelegatingServletConfig implements ServletConfig {

		@Override
		@Nullable
		public String getServletName() {
			return FreeMarkerView.this.getBeanName();
		}

		@Override
		@Nullable
		public ServletContext getServletContext() {
			return FreeMarkerView.this.getServletContext();
		}

		@Override
		@Nullable
		public String getInitParameter(String paramName) {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(Collections.emptySet());
		}
	}

}
