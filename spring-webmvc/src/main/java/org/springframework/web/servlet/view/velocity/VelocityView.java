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

package org.springframework.web.servlet.view.velocity;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.NestedIOException;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.springframework.web.util.NestedServletException;

/**
 * View using the Velocity template engine.
 *
 * <p>Exposes the following JavaBean properties:
 * <ul>
 * <li><b>url</b>: the location of the Velocity template to be wrapped,
 * relative to the Velocity resource loader path (see VelocityConfigurer).
 * <li><b>encoding</b> (optional, default is determined by Velocity configuration):
 * the encoding of the Velocity template file
 * <li><b>velocityFormatterAttribute</b> (optional, default=null): the name of
 * the VelocityFormatter helper object to expose in the Velocity context of this
 * view, or {@code null} if not needed. VelocityFormatter is part of standard Velocity.
 * <li><b>dateToolAttribute</b> (optional, default=null): the name of the
 * DateTool helper object to expose in the Velocity context of this view,
 * or {@code null} if not needed. DateTool is part of Velocity Tools.
 * <li><b>numberToolAttribute</b> (optional, default=null): the name of the
 * NumberTool helper object to expose in the Velocity context of this view,
 * or {@code null} if not needed. NumberTool is part of Velocity Tools.
 * <li><b>cacheTemplate</b> (optional, default=false): whether or not the Velocity
 * template should be cached. It should normally be true in production, but setting
 * this to false enables us to modify Velocity templates without restarting the
 * application (similar to JSPs). Note that this is a minor optimization only,
 * as Velocity itself caches templates in a modification-aware fashion.
 * </ul>
 *
 * <p>Depends on a VelocityConfig object such as VelocityConfigurer being
 * accessible in the current web application context, with any bean name.
 * Alternatively, you can set the VelocityEngine object as bean property.
 *
 * <p>Note: Spring 3.0's VelocityView requires Velocity 1.4 or higher, and optionally
 * Velocity Tools 1.1 or higher (depending on the use of DateTool and/or NumberTool).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Dave Syer
 * @see VelocityConfig
 * @see VelocityConfigurer
 * @see #setUrl
 * @see #setExposeSpringMacroHelpers
 * @see #setEncoding
 * @see #setVelocityEngine
 * @see VelocityConfig
 * @see VelocityConfigurer
 */
public class VelocityView extends AbstractTemplateView {

	private Map<String, Class> toolAttributes;

	private String dateToolAttribute;

	private String numberToolAttribute;

	private String encoding;

	private boolean cacheTemplate = false;

	private VelocityEngine velocityEngine;

	private Template template;


	/**
	 * Set tool attributes to expose to the view, as attribute name / class name pairs.
	 * An instance of the given class will be added to the Velocity context for each
	 * rendering operation, under the given attribute name.
	 * <p>For example, an instance of MathTool, which is part of the generic package
	 * of Velocity Tools, can be bound under the attribute name "math", specifying the
	 * fully qualified class name "org.apache.velocity.tools.generic.MathTool" as value.
	 * <p>Note that VelocityView can only create simple generic tools or values, that is,
	 * classes with a public default constructor and no further initialization needs.
	 * This class does not do any further checks, to not introduce a required dependency
	 * on a specific tools package.
	 * <p>For tools that are part of the view package of Velocity Tools, a special
	 * Velocity context and a special init callback are needed. Use VelocityToolboxView
	 * in such a case, or override {@code createVelocityContext} and
	 * {@code initTool} accordingly.
	 * <p>For a simple VelocityFormatter instance or special locale-aware instances
	 * of DateTool/NumberTool, which are part of the generic package of Velocity Tools,
	 * specify the "velocityFormatterAttribute", "dateToolAttribute" or
	 * "numberToolAttribute" properties, respectively.
	 * @param toolAttributes attribute names as keys, and tool class names as values
	 * @see org.apache.velocity.tools.generic.MathTool
	 * @see VelocityToolboxView
	 * @see #createVelocityContext
	 * @see #initTool
	 * @see #setDateToolAttribute
	 * @see #setNumberToolAttribute
	 */
	public void setToolAttributes(Map<String, Class> toolAttributes) {
		this.toolAttributes = toolAttributes;
	}

	/**
	 * Set the name of the DateTool helper object to expose in the Velocity context
	 * of this view, or {@code null} if not needed. The exposed DateTool will be aware of
	 * the current locale, as determined by Spring's LocaleResolver.
	 * <p>DateTool is part of the generic package of Velocity Tools 1.0.
	 * Spring uses a special locale-aware subclass of DateTool.
	 * @see org.apache.velocity.tools.generic.DateTool
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 * @see org.springframework.web.servlet.LocaleResolver
	 */
	public void setDateToolAttribute(String dateToolAttribute) {
		this.dateToolAttribute = dateToolAttribute;
	}

	/**
	 * Set the name of the NumberTool helper object to expose in the Velocity context
	 * of this view, or {@code null} if not needed. The exposed NumberTool will be aware of
	 * the current locale, as determined by Spring's LocaleResolver.
	 * <p>NumberTool is part of the generic package of Velocity Tools 1.1.
	 * Spring uses a special locale-aware subclass of NumberTool.
	 * @see org.apache.velocity.tools.generic.NumberTool
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see org.springframework.web.servlet.LocaleResolver
	 */
	public void setNumberToolAttribute(String numberToolAttribute) {
		this.numberToolAttribute = numberToolAttribute;
	}

	/**
	 * Set the encoding of the Velocity template file. Default is determined
	 * by the VelocityEngine: "ISO-8859-1" if not specified otherwise.
	 * <p>Specify the encoding in the VelocityEngine rather than per template
	 * if all your templates share a common encoding.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Return the encoding for the Velocity template.
	 */
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * Set whether the Velocity template should be cached. Default is "false".
	 * It should normally be true in production, but setting this to false enables us to
	 * modify Velocity templates without restarting the application (similar to JSPs).
	 * <p>Note that this is a minor optimization only, as Velocity itself caches
	 * templates in a modification-aware fashion.
	 */
	public void setCacheTemplate(boolean cacheTemplate) {
		this.cacheTemplate = cacheTemplate;
	}

	/**
	 * Return whether the Velocity template should be cached.
	 */
	protected boolean isCacheTemplate() {
		return this.cacheTemplate;
	}

	/**
	 * Set the VelocityEngine to be used by this view.
	 * <p>If this is not set, the default lookup will occur: A single VelocityConfig
	 * is expected in the current web application context, with any bean name.
	 * @see VelocityConfig
	 */
	public void setVelocityEngine(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}

	/**
	 * Return the VelocityEngine used by this view.
	 */
	protected VelocityEngine getVelocityEngine() {
		return this.velocityEngine;
	}


	/**
 	 * Invoked on startup. Looks for a single VelocityConfig bean to
 	 * find the relevant VelocityEngine for this factory.
 	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		super.initApplicationContext();

		if (getVelocityEngine() == null) {
			// No explicit VelocityEngine: try to autodetect one.
			setVelocityEngine(autodetectVelocityEngine());
		}
	}

	/**
	 * Autodetect a VelocityEngine via the ApplicationContext.
	 * Called if no explicit VelocityEngine has been specified.
	 * @return the VelocityEngine to use for VelocityViews
	 * @throws BeansException if no VelocityEngine could be found
	 * @see #getApplicationContext
	 * @see #setVelocityEngine
	 */
	protected VelocityEngine autodetectVelocityEngine() throws BeansException {
		try {
			VelocityConfig velocityConfig = BeanFactoryUtils.beanOfTypeIncludingAncestors(
					getApplicationContext(), VelocityConfig.class, true, false);
			return velocityConfig.getVelocityEngine();
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single VelocityConfig bean in this web application context " +
					"(may be inherited): VelocityConfigurer is the usual implementation. " +
					"This bean may be given any name.", ex);
		}
	}

	/**
	 * Check that the Velocity template used for this view exists and is valid.
	 * <p>Can be overridden to customize the behavior, for example in case of
	 * multiple templates to be rendered into a single view.
	 */
	@Override
	public boolean checkResource(Locale locale) throws Exception {
		try {
			// Check that we can get the template, even if we might subsequently get it again.
			this.template = getTemplate(getUrl());
			return true;
		}
		catch (ResourceNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No Velocity view found for URL: " + getUrl());
			}
			return false;
		}
		catch (Exception ex) {
			throw new NestedIOException(
					"Could not load Velocity template for URL [" + getUrl() + "]", ex);
		}
	}


	/**
	 * Process the model map by merging it with the Velocity template.
	 * Output is directed to the servlet response.
	 * <p>This method can be overridden if custom behavior is needed.
	 */
	@Override
	protected void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		exposeHelpers(model, request);

		Context velocityContext = createVelocityContext(model, request, response);
		exposeHelpers(velocityContext, request, response);
		exposeToolAttributes(velocityContext, request);

		doRender(velocityContext, response);
	}

	/**
	 * Expose helpers unique to each rendering operation. This is necessary so that
	 * different rendering operations can't overwrite each other's formats etc.
	 * <p>Called by {@code renderMergedTemplateModel}. The default implementation
	 * is empty. This method can be overridden to add custom helpers to the model.
	 * @param model the model that will be passed to the template for merging
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding model attributes
	 * @see #renderMergedTemplateModel
	 */
	protected void exposeHelpers(Map<String, Object> model, HttpServletRequest request) throws Exception {
	}

	/**
	 * Create a Velocity Context instance for the given model,
	 * to be passed to the template for merging.
	 * <p>The default implementation delegates to {@link #createVelocityContext(Map)}.
	 * Can be overridden for a special context class, for example ChainedContext which
	 * is part of the view package of Velocity Tools. ChainedContext is needed for
	 * initialization of ViewTool instances.
	 * <p>Have a look at {@link VelocityToolboxView}, which pre-implements
	 * ChainedContext support. This is not part of the standard VelocityView class
	 * in order to avoid a required dependency on the view package of Velocity Tools.
	 * @param model the model Map, containing the model attributes to be exposed to the view
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return the Velocity Context
	 * @throws Exception if there's a fatal error while creating the context
	 * @see #createVelocityContext(Map)
	 * @see #initTool
	 * @see org.apache.velocity.tools.view.context.ChainedContext
	 * @see VelocityToolboxView
	 */
	protected Context createVelocityContext(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		return createVelocityContext(model);
	}

	/**
	 * Create a Velocity Context instance for the given model,
	 * to be passed to the template for merging.
	 * <p>Default implementation creates an instance of Velocity's
	 * VelocityContext implementation class.
	 * @param model the model Map, containing the model attributes
	 * to be exposed to the view
	 * @return the Velocity Context
	 * @throws Exception if there's a fatal error while creating the context
	 * @see org.apache.velocity.VelocityContext
	 */
	protected Context createVelocityContext(Map<String, Object> model) throws Exception {
		return new VelocityContext(model);
	}

	/**
	 * Expose helpers unique to each rendering operation. This is necessary so that
	 * different rendering operations can't overwrite each other's formats etc.
	 * <p>Called by {@code renderMergedTemplateModel}. Default implementation
	 * delegates to {@code exposeHelpers(velocityContext, request)}. This method
	 * can be overridden to add special tools to the context, needing the servlet response
	 * to initialize (see Velocity Tools, for example LinkTool and ViewTool/ChainedContext).
	 * @param velocityContext Velocity context that will be passed to the template
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception if there's a fatal error while we're adding model attributes
	 * @see #exposeHelpers(org.apache.velocity.context.Context, HttpServletRequest)
	 */
	protected void exposeHelpers(
			Context velocityContext, HttpServletRequest request, HttpServletResponse response) throws Exception {

		exposeHelpers(velocityContext, request);
	}

	/**
	 * Expose helpers unique to each rendering operation. This is necessary so that
	 * different rendering operations can't overwrite each other's formats etc.
	 * <p>Default implementation is empty. This method can be overridden to add
	 * custom helpers to the Velocity context.
	 * @param velocityContext Velocity context that will be passed to the template
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding model attributes
	 * @see #exposeHelpers(Map, HttpServletRequest)
	 */
	protected void exposeHelpers(Context velocityContext, HttpServletRequest request) throws Exception {
	}

	/**
	 * Expose the tool attributes, according to corresponding bean property settings.
	 * <p>Do not override this method unless for further tools driven by bean properties.
	 * Override one of the {@code exposeHelpers} methods to add custom helpers.
	 * @param velocityContext Velocity context that will be passed to the template
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding model attributes
	 * @see #setDateToolAttribute
	 * @see #setNumberToolAttribute
	 * @see #exposeHelpers(Map, HttpServletRequest)
	 * @see #exposeHelpers(org.apache.velocity.context.Context, HttpServletRequest, HttpServletResponse)
	 */
	protected void exposeToolAttributes(Context velocityContext, HttpServletRequest request) throws Exception {
		// Expose generic attributes.
		if (this.toolAttributes != null) {
			for (Map.Entry<String, Class> entry : this.toolAttributes.entrySet()) {
				String attributeName = entry.getKey();
				Class toolClass = entry.getValue();
				try {
					Object tool = toolClass.newInstance();
					initTool(tool, velocityContext);
					velocityContext.put(attributeName, tool);
				}
				catch (Exception ex) {
					throw new NestedServletException("Could not instantiate Velocity tool '" + attributeName + "'", ex);
				}
			}
		}

		// Expose locale-aware DateTool/NumberTool attributes.
		if (this.dateToolAttribute != null || this.numberToolAttribute != null) {
			Locale locale = RequestContextUtils.getLocale(request);
			if (this.dateToolAttribute != null) {
				TimeZone timeZone = RequestContextUtils.getTimeZone(request);
				velocityContext.put(this.dateToolAttribute, new LocaleAwareDateTool(locale, timeZone));
			}
			if (this.numberToolAttribute != null) {
				velocityContext.put(this.numberToolAttribute, new LocaleAwareNumberTool(locale));
			}
		}
	}

	/**
	 * Initialize the given tool instance. The default implementation is empty.
	 * <p>Can be overridden to check for special callback interfaces, for example
	 * the ViewContext interface which is part of the view package of Velocity Tools.
	 * In the particular case of ViewContext, you'll usually also need a special
	 * Velocity context, like ChainedContext which is part of Velocity Tools too.
	 * <p>Have a look at {@link VelocityToolboxView}, which pre-implements such a
	 * ViewTool check. This is not part of the standard VelocityView class in order
	 * to avoid a required dependency on the view package of Velocity Tools.
	 * @param tool the tool instance to initialize
	 * @param velocityContext the Velocity context
	 * @throws Exception if initializion of the tool failed
	 * @see #createVelocityContext
	 * @see org.apache.velocity.tools.view.context.ViewContext
	 * @see org.apache.velocity.tools.view.context.ChainedContext
	 * @see VelocityToolboxView
	 */
	protected void initTool(Object tool, Context velocityContext) throws Exception {
	}


	/**
	 * Render the Velocity view to the given response, using the given Velocity
	 * context which contains the complete template model to use.
	 * <p>The default implementation renders the template specified by the "url"
	 * bean property, retrieved via {@code getTemplate}. It delegates to the
	 * {@code mergeTemplate} method to merge the template instance with the
	 * given Velocity context.
	 * <p>Can be overridden to customize the behavior, for example to render
	 * multiple templates into a single view.
	 * @param context the Velocity context to use for rendering
	 * @param response servlet response (use this to get the OutputStream or Writer)
	 * @throws Exception if thrown by Velocity
	 * @see #setUrl
	 * @see #getTemplate()
	 * @see #mergeTemplate
	 */
	protected void doRender(Context context, HttpServletResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering Velocity template [" + getUrl() + "] in VelocityView '" + getBeanName() + "'");
		}
		mergeTemplate(getTemplate(), context, response);
	}

	/**
	 * Retrieve the Velocity template to be rendered by this view.
	 * <p>By default, the template specified by the "url" bean property will be
	 * retrieved: either returning a cached template instance or loading a fresh
	 * instance (according to the "cacheTemplate" bean property)
	 * @return the Velocity template to render
	 * @throws Exception if thrown by Velocity
	 * @see #setUrl
	 * @see #setCacheTemplate
	 * @see #getTemplate(String)
	 */
	protected Template getTemplate() throws Exception {
		// We already hold a reference to the template, but we might want to load it
		// if not caching. Velocity itself caches templates, so our ability to
		// cache templates in this class is a minor optimization only.
		if (isCacheTemplate() && this.template != null) {
			return this.template;
		}
		else {
			return getTemplate(getUrl());
		}
	}

	/**
	 * Retrieve the Velocity template specified by the given name,
	 * using the encoding specified by the "encoding" bean property.
	 * <p>Can be called by subclasses to retrieve a specific template,
	 * for example to render multiple templates into a single view.
	 * @param name the file name of the desired template
	 * @return the Velocity template
	 * @throws Exception if thrown by Velocity
	 * @see org.apache.velocity.app.VelocityEngine#getTemplate
	 */
	protected Template getTemplate(String name) throws Exception {
		return (getEncoding() != null ?
				getVelocityEngine().getTemplate(name, getEncoding()) :
				getVelocityEngine().getTemplate(name));
	}

	/**
	 * Merge the template with the context.
	 * Can be overridden to customize the behavior.
	 * @param template the template to merge
	 * @param context the Velocity context to use for rendering
	 * @param response servlet response (use this to get the OutputStream or Writer)
	 * @throws Exception if thrown by Velocity
	 * @see org.apache.velocity.Template#merge
	 */
	protected void mergeTemplate(
			Template template, Context context, HttpServletResponse response) throws Exception {

		try {
			template.merge(context, response.getWriter());
		}
		catch (MethodInvocationException ex) {
			Throwable cause = ex.getWrappedThrowable();
			throw new NestedServletException(
					"Method invocation failed during rendering of Velocity view with name '" +
					getBeanName() + "': " + ex.getMessage() + "; reference [" + ex.getReferenceName() +
					"], method '" + ex.getMethodName() + "'",
					cause==null ? ex : cause);
		}
	}


	/**
	 * Subclass of DateTool from Velocity Tools, using a passed-in Locale
	 * (usually the RequestContext Locale) instead of the default Locale.N
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	private static class LocaleAwareDateTool extends DateTool {

		private final Locale locale;

		private final TimeZone timeZone;

		private LocaleAwareDateTool(Locale locale, TimeZone timeZone) {
			this.locale = locale;
			this.timeZone = timeZone;
		}

		@Override
		public Locale getLocale() {
			return this.locale;
		}

		@Override
		public TimeZone getTimeZone() {
			if (this.timeZone != null) {
				return this.timeZone;
			}
			return super.getTimeZone();
		}
	}


	/**
	 * Subclass of NumberTool from Velocity Tools, using a passed-in Locale
	 * (usually the RequestContext Locale) instead of the default Locale.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 */
	private static class LocaleAwareNumberTool extends NumberTool {

		private final Locale locale;

		private LocaleAwareNumberTool(Locale locale) {
			this.locale = locale;
		}

		@Override
		public Locale getLocale() {
			return this.locale;
		}
	}

}
