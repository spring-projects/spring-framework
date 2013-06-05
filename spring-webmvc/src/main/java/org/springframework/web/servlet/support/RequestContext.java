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

package org.springframework.web.servlet.support;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.ResourceBundleThemeSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.TimeZoneResolver;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * Context holder for request-specific state, like current web application context, current locale, current theme, and
 * potential binding errors. Provides easy access to localized messages and Errors instances.
 *
 * <p>Suitable for exposition to views, and usage within JSP's "useBean" tag, JSP scriptlets, JSTL EL, Velocity
 * templates, etc. Necessary for views that do not have access to the servlet request, like Velocity templates.
 *
 * <p>Can be instantiated manually, or automatically exposed to views as model attribute via AbstractView's
 * "requestContextAttribute" property.
 *
 * <p>Will also work outside of DispatcherServlet requests, accessing the root WebApplicationContext and using an
 * appropriate fallback for the locale and time zone (the HttpServletRequest's primary
 * locale and the system's time zone).
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 03.03.2003
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.view.AbstractView#setRequestContextAttribute
 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setRequestContextAttribute
 * @see #getFallbackLocale()
 * @see #getFallbackTimeZone()
 */
public class RequestContext {

	/**
	 * Default theme name used if the RequestContext cannot find a ThemeResolver. Only applies to non-DispatcherServlet
	 * requests. <p>Same as AbstractThemeResolver's default, but not linked in here to avoid package interdependencies.
	 * @see org.springframework.web.servlet.theme.AbstractThemeResolver#ORIGINAL_DEFAULT_THEME_NAME
	 */
	public static final String DEFAULT_THEME_NAME = "theme";

	/**
	 * Request attribute to hold the current web application context for RequestContext usage. By default, the
	 * DispatcherServlet's context (or the root context as fallback) is exposed.
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = RequestContext.class.getName() + ".CONTEXT";

	/**
	 * The name of the bean to use to look up in an implementation of
	 * {@link RequestDataValueProcessor} has been configured.
	 */
	private static final String REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME = "requestDataValueProcessor";

	protected static final boolean jstlPresent = ClassUtils.isPresent("javax.servlet.jsp.jstl.core.Config",
			RequestContext.class.getClassLoader());

	private HttpServletRequest request;

	private HttpServletResponse response;

	private Map<String, Object> model;

	private WebApplicationContext webApplicationContext;

	private Locale locale;

	private TimeZone timeZone;

	private Theme theme;

	private Boolean defaultHtmlEscape;

	private UrlPathHelper urlPathHelper;

	private RequestDataValueProcessor requestDataValueProcessor;

	private Map<String, Errors> errorsMap;

	/**
	 * Create a new RequestContext for the given request, using the request attributes for Errors retrieval. <p>This
	 * only works with InternalResourceViews, as Errors instances are part of the model and not normally exposed as
	 * request attributes. It will typically be used within JSPs or custom tags. <p><b>Will only work within a
	 * DispatcherServlet request.</b> Pass in a ServletContext to be able to fallback to the root WebApplicationContext.
	 * @param request current HTTP request
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(javax.servlet.http.HttpServletRequest, javax.servlet.ServletContext)
	 */
	public RequestContext(HttpServletRequest request) {
		initContext(request, null, null, null);
	}

	/**
	 * Create a new RequestContext for the given request, using the request attributes for Errors retrieval. <p>This
	 * only works with InternalResourceViews, as Errors instances are part of the model and not normally exposed as
	 * request attributes. It will typically be used within JSPs or custom tags. <p>If a ServletContext is specified,
	 * the RequestContext will also work with the root WebApplicationContext (outside a DispatcherServlet).
	 * @param request current HTTP request
	 * @param servletContext the servlet context of the web application (can be {@code null}; necessary for
	 * fallback to root WebApplicationContext)
	 * @see org.springframework.web.context.WebApplicationContext
	 * @see org.springframework.web.servlet.DispatcherServlet
	 */
	public RequestContext(HttpServletRequest request, ServletContext servletContext) {
		initContext(request, null, servletContext, null);
	}

	/**
	 * Create a new RequestContext for the given request, using the given model attributes for Errors retrieval. <p>This
	 * works with all View implementations. It will typically be used by View implementations. <p><b>Will only work
	 * within a DispatcherServlet request.</b> Pass in a ServletContext to be able to fallback to the root
	 * WebApplicationContext.
	 * @param request current HTTP request
	 * @param model the model attributes for the current view (can be {@code null}, using the request attributes
	 * for Errors retrieval)
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.ServletContext, Map)
	 */
	public RequestContext(HttpServletRequest request, Map<String, Object> model) {
		initContext(request, null, null, model);
	}

	/**
	 * Create a new RequestContext for the given request, using the given model attributes for Errors retrieval. <p>This
	 * works with all View implementations. It will typically be used by View implementations. <p>If a ServletContext is
	 * specified, the RequestContext will also work with a root WebApplicationContext (outside a DispatcherServlet).
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param servletContext the servlet context of the web application (can be {@code null}; necessary for
	 * fallback to root WebApplicationContext)
	 * @param model the model attributes for the current view (can be {@code null}, using the request attributes
	 * for Errors retrieval)
	 * @see org.springframework.web.context.WebApplicationContext
	 * @see org.springframework.web.servlet.DispatcherServlet
	 */
	public RequestContext(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext,
			Map<String, Object> model) {

		initContext(request, response, servletContext, model);
	}

	/**
	 * Default constructor for subclasses.
	 */
	protected RequestContext() {
	}

	/**
	 * Initialize this context with the given request, using the given model attributes for Errors retrieval.
	 * <p>Delegates to {@code getFallbackLocale}, {@code getFallbackTimeZone}, and
	 * {@code getFallbackTheme} for determining the fallback locale, time zone, and theme,
	 * respectively, if no LocaleResolver, TimeZoneResolver, and/or ThemeResolver can be found in the request.
	 * @param request current HTTP request
	 * @param servletContext the servlet context of the web application (can be {@code null}; necessary for
	 * fallback to root WebApplicationContext)
	 * @param model the model attributes for the current view (can be {@code null}, using the request attributes
	 * for Errors retrieval)
	 * @see #getFallbackLocale
	 * @see #getFallbackTimeZone
	 * @see #getFallbackTheme
	 * @see org.springframework.web.servlet.DispatcherServlet#LOCALE_RESOLVER_ATTRIBUTE
	 * @see org.springframework.web.servlet.DispatcherServlet#TIME_ZONE_RESOLVER_ATTRIBUTE
	 * @see org.springframework.web.servlet.DispatcherServlet#THEME_RESOLVER_ATTRIBUTE
	 */
	protected void initContext(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext,
			Map<String, Object> model) {

		this.request = request;
		this.response = response;
		this.model = model;

		// Fetch WebApplicationContext, either from DispatcherServlet or the root context.
		// ServletContext needs to be specified to be able to fall back to the root context!
		this.webApplicationContext = (WebApplicationContext) request.getAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (this.webApplicationContext == null) {
			this.webApplicationContext = RequestContextUtils.getWebApplicationContext(request, servletContext);
		}

		// Determine locale to use for this RequestContext.
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
		if (localeResolver != null) {
			// Try LocaleResolver (we're within a DispatcherServlet request).
			this.locale = localeResolver.resolveLocale(request);
		} else {
			// No LocaleResolver available -> try fallback.
			this.locale = getFallbackLocale();
		}

		TimeZoneResolver timeZoneResolver = RequestContextUtils.getTimeZoneResolver(request);
		if (timeZoneResolver != null) {
			// Try TimeZoneResolver (we're within a DispatcherServlet request).
			this.timeZone = timeZoneResolver.resolveTimeZone(request);
		} else {
			// No TimeZoneResolver available -> try fallback.
			this.timeZone = getFallbackTimeZone();
		}

		// Determine default HTML escape setting from the "defaultHtmlEscape"
		// context-param in web.xml, if any.
		this.defaultHtmlEscape = WebUtils.getDefaultHtmlEscape(this.webApplicationContext.getServletContext());

		this.urlPathHelper = new UrlPathHelper();

		try {
			this.requestDataValueProcessor = this.webApplicationContext.getBean(
					REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ignored
		}
	}

	/**
	 * Determine the fallback locale for this context.
	 * <p>The default implementation checks for a JSTL locale attribute in request,
	 * session or application scope; if not found, returns the
	 * {@code HttpServletRequest.getLocale()}.
	 * @return the fallback locale (never {@code null})
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	protected Locale getFallbackLocale() {
		if (jstlPresent) {
			Locale locale = JstlLocaleResolver.getJstlLocale(getRequest(), getServletContext());
			if (locale != null) {
				return locale;
			}
		}
		return getRequest().getLocale();
	}

	/**
	 * Determine the fallback locale for this context.
	 * <p>The default implementation checks for a JSTL locale attribute in request,
	 * session or application scope; if not found, returns the {@code null}.
	 * @return the fallback locale or {@code null}
	 */
	protected TimeZone getFallbackTimeZone() {
		if (jstlPresent) {
			return JstlTimeZoneResolver.getJstlTimeZone(getRequest(), getServletContext());
		}
		return null;
	}

	/**
	 * Determine the fallback theme for this context.
	 * <p>The default implementation returns the default theme (with name "theme").
	 * @return the fallback theme (never {@code null})
	 */
	protected Theme getFallbackTheme() {
		ThemeSource themeSource = RequestContextUtils.getThemeSource(getRequest());
		if (themeSource == null) {
			themeSource = new ResourceBundleThemeSource();
		}
		Theme theme = themeSource.getTheme(DEFAULT_THEME_NAME);
		if (theme == null) {
			throw new IllegalStateException("No theme defined and no fallback theme found");
		}
		return theme;
	}

	/**
	 * Return the underlying HttpServletRequest. Only intended for cooperating classes in this package.
	 */
	protected final HttpServletRequest getRequest() {
		return this.request;
	}

	/**
	 * Return the underlying ServletContext. Only intended for cooperating classes in this package.
	 */
	protected final ServletContext getServletContext() {
		return this.webApplicationContext.getServletContext();
	}

	/**
	 * Return the current WebApplicationContext.
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}

	/**
	 * Return the current WebApplicationContext as MessageSource.
	 */
	public final MessageSource getMessageSource() {
		return this.webApplicationContext;
	}

	/**
	 * Return the model Map that this RequestContext encapsulates, if any.
	 * @return the populated model Map, or {@code null} if none available
	 */
	public final Map<String, Object> getModel() {
		return this.model;
	}

	/**
	 * Return the current Locale (never {@code null}).
	 */
	public final Locale getLocale() {
		return this.locale;
	}

	/**
	 * Return the current TimeZone (never {@code null}).
	 * @return the current TimeZone.
	 */
	public final TimeZone getTimeZone() {
		return this.timeZone;
	}

	/**
	 * Return the current theme (never {@code null}).
	 * <p>Resolved lazily for more efficiency when theme support is not being used.
	 */
	public final Theme getTheme() {
		if (this.theme == null) {
			// Lazily determine theme to use for this RequestContext.
			this.theme = RequestContextUtils.getTheme(this.request);
			if (this.theme == null) {
				// No ThemeResolver and ThemeSource available -> try fallback.
				this.theme = getFallbackTheme();
			}
		}
		return this.theme;
	}

	/**
	 * (De)activate default HTML escaping for messages and errors, for the scope of this RequestContext. The default is
	 * the application-wide setting (the "defaultHtmlEscape" context-param in web.xml).
	 * @see org.springframework.web.util.WebUtils#isDefaultHtmlEscape
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}

	/**
	 * Is default HTML escaping active? Falls back to {@code false} in case of no explicit default given.
	 */
	public boolean isDefaultHtmlEscape() {
		return this.defaultHtmlEscape != null && this.defaultHtmlEscape;
	}

	/**
	 * Return the default HTML escape setting, differentiating between no default specified and an explicit value.
	 * @return whether default HTML escaping is enabled (null = no explicit default)
	 */
	public Boolean getDefaultHtmlEscape() {
		return this.defaultHtmlEscape;
	}

	/**
	 * Set the UrlPathHelper to use for context path and request URI decoding. Can be used to pass a shared
	 * UrlPathHelper instance in.
	 * <p>A default UrlPathHelper is always available.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * Return the UrlPathHelper used for context path and request URI decoding. Can be used to configure the current
	 * UrlPathHelper.
	 * <p>A default UrlPathHelper is always available.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * Return the RequestDataValueProcessor instance to use obtained from the
	 * WebApplicationContext under the name {@code "requestDataValueProcessor"}.
	 * Or {@code null} if no matching bean was found.
	 */
	public RequestDataValueProcessor getRequestDataValueProcessor() {
		return this.requestDataValueProcessor;
	}

	/**
	 * Return the context path of the original request, that is, the path that indicates the current web application.
	 * This is useful for building links to other resources within the application.
	 * <p>Delegates to the UrlPathHelper for decoding.
	 * @see javax.servlet.http.HttpServletRequest#getContextPath
	 * @see #getUrlPathHelper
	 */
	public String getContextPath() {
		return this.urlPathHelper.getOriginatingContextPath(this.request);
	}

	/**
	 * Return a context-aware URl for the given relative URL.
	 * @param relativeUrl the relative URL part
	 * @return a URL that points back to the server with an absolute path (also URL-encoded accordingly)
	 */
	public String getContextUrl(String relativeUrl) {
		String url = getContextPath() + relativeUrl;
		if (this.response != null) {
			url = this.response.encodeURL(url);
		}
		return url;
	}

	/**
	 * Return a context-aware URl for the given relative URL with placeholders (named keys with braces {@code {}}).
	 * For example, send in a relative URL {@code foo/{bar}?spam={spam}} and a parameter map
	 * {@code {bar=baz,spam=nuts}} and the result will be {@code [contextpath]/foo/baz?spam=nuts}.
	 *
	 * @param relativeUrl the relative URL part
	 * @param params a map of parameters to insert as placeholders in the url
	 * @return a URL that points back to the server with an absolute path (also URL-encoded accordingly)
	 */
	public String getContextUrl(String relativeUrl, Map<String, ?> params) {
		String url = getContextPath() + relativeUrl;
		UriTemplate template = new UriTemplate(url);
		url = template.expand(params).toASCIIString();
		if (this.response != null) {
			url = this.response.encodeURL(url);
		}
		return url;
	}

	/**
	 * Return the path to URL mappings within the current servlet including the
	 * context path and the servlet path of the original request. This is useful
	 * for building links to other resources within the application where a
	 * servlet mapping of the style {@code "/main/*"} is used.
	 * <p>Delegates to the UrlPathHelper to determine the context and servlet path.
	 */
	public String getPathToServlet() {
		String path = this.urlPathHelper.getOriginatingContextPath(this.request);
		if (StringUtils.hasText(this.urlPathHelper.getPathWithinServletMapping(this.request))) {
			path += this.urlPathHelper.getOriginatingServletPath(this.request);
		}
		return path;
	}

	/**
	 * Return the request URI of the original request, that is, the invoked URL without parameters. This is particularly
	 * useful as HTML form action target, possibly in combination with the original query string. <p><b>Note this
	 * implementation will correctly resolve to the URI of any originating root request in the presence of a forwarded
	 * request. However, this can only work when the Servlet 2.4 'forward' request attributes are present.
	 * <p>Delegates to the UrlPathHelper for decoding.
	 * @see #getQueryString
	 * @see org.springframework.web.util.UrlPathHelper#getOriginatingRequestUri
	 * @see #getUrlPathHelper
	 */
	public String getRequestUri() {
		return this.urlPathHelper.getOriginatingRequestUri(this.request);
	}

	/**
	 * Return the query string of the current request, that is, the part after the request path. This is particularly
	 * useful for building an HTML form action target in combination with the original request URI. <p><b>Note this
	 * implementation will correctly resolve to the query string of any originating root request in the presence of a
	 * forwarded request. However, this can only work when the Servlet 2.4 'forward' request attributes are present.
	 * <p>Delegates to the UrlPathHelper for decoding.
	 * @see #getRequestUri
	 * @see org.springframework.web.util.UrlPathHelper#getOriginatingQueryString
	 * @see #getUrlPathHelper
	 */
	public String getQueryString() {
		return this.urlPathHelper.getOriginatingQueryString(this.request);
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code code of the message
	 * @param defaultMessage String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, String defaultMessage) {
		return getMessage(code, null, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, Object[] args, String defaultMessage) {
		return getMessage(code, args, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @param defaultMessage String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, List args, String defaultMessage) {
		return getMessage(code, (args != null ? args.toArray() : null), defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code.
	 * @param code code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage String to return if the lookup fails
	 * @param htmlEscape HTML escape the message?
	 * @return the message
	 */
	public String getMessage(String code, Object[] args, String defaultMessage, boolean htmlEscape) {
		String msg = this.webApplicationContext.getMessage(code, args, defaultMessage, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code code of the message
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return getMessage(code, null, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, Object[] args) throws NoSuchMessageException {
		return getMessage(code, args, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, List args) throws NoSuchMessageException {
		return getMessage(code, (args != null ? args.toArray() : null), isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code.
	 * @param code code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param htmlEscape HTML escape the message?
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, Object[] args, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.webApplicationContext.getMessage(code, args, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * Retrieve the given MessageSourceResolvable (e.g. an ObjectError instance), using the "defaultHtmlEscape" setting.
	 * @param resolvable the MessageSourceResolvable
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getMessage(resolvable, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the given MessageSourceResolvable (e.g. an ObjectError instance).
	 * @param resolvable the MessageSourceResolvable
	 * @param htmlEscape HTML escape the message?
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.webApplicationContext.getMessage(resolvable, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * Retrieve the theme message for the given code.
	 * <p>Note that theme messages are never HTML-escaped, as they typically denote
	 * theme-specific resource paths and not client-visible messages.
	 * @param code code of the message
	 * @param defaultMessage String to return if the lookup fails
	 * @return the message
	 */
	public String getThemeMessage(String code, String defaultMessage) {
		return getTheme().getMessageSource().getMessage(code, null, defaultMessage, this.locale);
	}

	/**
	 * Retrieve the theme message for the given code.
	 * <p>Note that theme messages are never HTML-escaped, as they typically denote
	 * theme-specific resource paths and not client-visible messages.
	 * @param code code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage String to return if the lookup fails
	 * @return the message
	 */
	public String getThemeMessage(String code, Object[] args, String defaultMessage) {
		return getTheme().getMessageSource().getMessage(code, args, defaultMessage, this.locale);
	}

	/**
	 * Retrieve the theme message for the given code.
	 * <p>Note that theme messages are never HTML-escaped, as they typically denote
	 * theme-specific resource paths and not client-visible messages.
	 * @param code code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @param defaultMessage String to return if the lookup fails
	 * @return the message
	 */
	public String getThemeMessage(String code, List args, String defaultMessage) {
		return getTheme().getMessageSource().getMessage(code, (args != null ? args.toArray() : null), defaultMessage,
				this.locale);
	}

	/**
	 * Retrieve the theme message for the given code.
	 * <p>Note that theme messages are never HTML-escaped, as they typically denote
	 * theme-specific resource paths and not client-visible messages.
	 * @param code code of the message
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getThemeMessage(String code) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, null, this.locale);
	}

	/**
	 * Retrieve the theme message for the given code.
	 * <p>Note that theme messages are never HTML-escaped, as they typically denote
	 * theme-specific resource paths and not client-visible messages.
	 * @param code code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getThemeMessage(String code, Object[] args) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, args, this.locale);
	}

	/**
	 * Retrieve the theme message for the given code.
	 * <p>Note that theme messages are never HTML-escaped, as they typically denote
	 * theme-specific resource paths and not client-visible messages.
	 * @param code code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getThemeMessage(String code, List args) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, (args != null ? args.toArray() : null), this.locale);
	}

	/**
	 * Retrieve the given MessageSourceResolvable in the current theme.
	 * <p>Note that theme messages are never HTML-escaped, as they typically denote
	 * theme-specific resource paths and not client-visible messages.
	 * @param resolvable the MessageSourceResolvable
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getThemeMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(resolvable, this.locale);
	}

	/**
	 * Retrieve the Errors instance for the given bind object, using the "defaultHtmlEscape" setting.
	 * @param name name of the bind object
	 * @return the Errors instance, or {@code null} if not found
	 */
	public Errors getErrors(String name) {
		return getErrors(name, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the Errors instance for the given bind object.
	 * @param name name of the bind object
	 * @param htmlEscape create an Errors instance with automatic HTML escaping?
	 * @return the Errors instance, or {@code null} if not found
	 */
	public Errors getErrors(String name, boolean htmlEscape) {
		if (this.errorsMap == null) {
			this.errorsMap = new HashMap<String, Errors>();
		}
		Errors errors = this.errorsMap.get(name);
		boolean put = false;
		if (errors == null) {
			errors = (Errors) getModelObject(BindingResult.MODEL_KEY_PREFIX + name);
			// Check old BindException prefix for backwards compatibility.
			if (errors instanceof BindException) {
				errors = ((BindException) errors).getBindingResult();
			}
			if (errors == null) {
				return null;
			}
			put = true;
		}
		if (htmlEscape && !(errors instanceof EscapedErrors)) {
			errors = new EscapedErrors(errors);
			put = true;
		} else if (!htmlEscape && errors instanceof EscapedErrors) {
			errors = ((EscapedErrors) errors).getSource();
			put = true;
		}
		if (put) {
			this.errorsMap.put(name, errors);
		}
		return errors;
	}

	/**
	 * Retrieve the model object for the given model name, either from the model or from the request attributes.
	 * @param modelName the name of the model object
	 * @return the model object
	 */
	protected Object getModelObject(String modelName) {
		if (this.model != null) {
			return this.model.get(modelName);
		} else {
			return this.request.getAttribute(modelName);
		}
	}

	/**
	 * Create a BindStatus for the given bind object, using the "defaultHtmlEscape" setting.
	 * @param path the bean and property path for which values and errors will be resolved (e.g. "person.age")
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path) throws IllegalStateException {
		return new BindStatus(this, path, isDefaultHtmlEscape());
	}

	/**
	 * Create a BindStatus for the given bind object, using the "defaultHtmlEscape" setting.
	 * @param path the bean and property path for which values and errors will be resolved (e.g. "person.age")
	 * @param htmlEscape create a BindStatus with automatic HTML escaping?
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(this, path, htmlEscape);
	}

	/**
	 * Inner class that isolates the JSTL dependency. Just called to resolve the fallback locale if the JSTL API is
	 * present.
	 */
	private static class JstlLocaleResolver {

		public static Locale getJstlLocale(HttpServletRequest request, ServletContext servletContext) {
			Object localeObject = Config.get(request, Config.FMT_LOCALE);
			if (localeObject == null) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					localeObject = Config.get(session, Config.FMT_LOCALE);
				}
				if (localeObject == null && servletContext != null) {
					localeObject = Config.get(servletContext, Config.FMT_LOCALE);
				}
			}
			return (localeObject instanceof Locale ? (Locale) localeObject : null);
		}
	}

	/**
	 * Inner class that isolates the JSTL dependency. Just called to resolve the fallback
	 * time zone if the JSTL API is present.
	 */
	private static class JstlTimeZoneResolver {

		public static TimeZone getJstlTimeZone(HttpServletRequest request,
											   ServletContext servletContext) {
			Object timeZoneObject = Config.get(request, Config.FMT_TIME_ZONE);
			if (timeZoneObject == null) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					timeZoneObject = Config.get(session, Config.FMT_TIME_ZONE);
				}
				if (timeZoneObject == null && servletContext != null) {
					timeZoneObject = Config.get(servletContext, Config.FMT_TIME_ZONE);
				}
			}
			return (timeZoneObject instanceof TimeZone ? (TimeZone) timeZoneObject : null);
		}
	}

}
