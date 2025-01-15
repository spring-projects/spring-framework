/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.servlet.support;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.jstl.core.Config;
import org.jspecify.annotations.Nullable;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Context holder for request-specific state, like current web application context, current locale,
 * and potential binding errors. Provides easy access to localized messages and
 * Errors instances.
 *
 * <p>Suitable for exposition to views, and usage within JSP's "useBean" tag, JSP scriptlets, JSTL EL,
 * etc. Necessary for views that do not have access to the servlet request, like FreeMarker templates.
 *
 * <p>Can be instantiated manually, or automatically exposed to views as model attribute via AbstractView's
 * "requestContextAttribute" property.
 *
 * <p>Will also work outside DispatcherServlet requests, accessing the root WebApplicationContext
 * and using an appropriate fallback for the locale (the HttpServletRequest's primary locale).
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 03.03.2003
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.view.AbstractView#setRequestContextAttribute
 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setRequestContextAttribute
 */
public class RequestContext {

	/**
	 * Request attribute to hold the current web application context for RequestContext usage.
	 * By default, the DispatcherServlet's context (or the root context as fallback) is exposed.
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = RequestContext.class.getName() + ".CONTEXT";


	protected static final boolean jstlPresent = ClassUtils.isPresent(
			"jakarta.servlet.jsp.jstl.core.Config", RequestContext.class.getClassLoader());

	private final HttpServletRequest request;

	private final @Nullable HttpServletResponse response;

	private final @Nullable Map<String, Object> model;

	private final WebApplicationContext webApplicationContext;

	private @Nullable Locale locale;

	private @Nullable TimeZone timeZone;

	private @Nullable Boolean defaultHtmlEscape;

	private final @Nullable Boolean responseEncodedHtmlEscape;

	private UrlPathHelper urlPathHelper;

	private @Nullable RequestDataValueProcessor requestDataValueProcessor;

	private @Nullable Map<String, Errors> errorsMap;


	/**
	 * Create a new RequestContext for the given request, using the request attributes for Errors retrieval.
	 * <p>This only works with InternalResourceViews, as Errors instances are part of the model and not
	 * normally exposed as request attributes. It will typically be used within JSPs or custom tags.
	 * <p>As of 6.2, this will work within a DispatcherServlet request as well as with the root
	 * WebApplicationContext (outside a DispatcherServlet).
	 * @param request current HTTP request
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.ServletContext)
	 */
	public RequestContext(HttpServletRequest request) {
		this(request, null, request.getServletContext(), null);
	}

	/**
	 * Create a new RequestContext for the given request, using the request attributes for Errors retrieval.
	 * <p>This only works with InternalResourceViews, as Errors instances are part of the model and not
	 * normally exposed as request attributes. It will typically be used within JSPs or custom tags.
	 * <p>As of 6.2, this will work within a DispatcherServlet request as well as with the root
	 * WebApplicationContext (outside a DispatcherServlet).
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, jakarta.servlet.ServletContext, Map)
	 */
	public RequestContext(HttpServletRequest request, HttpServletResponse response) {
		this(request, response, request.getServletContext(), null);
	}

	/**
	 * Create a new RequestContext for the given request, using the request attributes for Errors retrieval.
	 * <p>This only works with InternalResourceViews, as Errors instances are part of the model and not
	 * normally exposed as request attributes. It will typically be used within JSPs or custom tags.
	 * <p>If a ServletContext is specified, the RequestContext will also work with the root
	 * WebApplicationContext (outside a DispatcherServlet).
	 * @param request current HTTP request
	 * @param servletContext the servlet context of the web application (can be {@code null};
	 * necessary for fallback to root WebApplicationContext)
	 * @see org.springframework.web.context.WebApplicationContext
	 * @see org.springframework.web.servlet.DispatcherServlet
	 */
	public RequestContext(HttpServletRequest request, @Nullable ServletContext servletContext) {
		this(request, null, servletContext, null);
	}

	/**
	 * Create a new RequestContext for the given request, using the given model attributes for Errors retrieval.
	 * <p>This works with all View implementations. It will typically be used by View implementations.
	 * <p>As of 6.2, this will work within a DispatcherServlet request as well as with the root
	 * WebApplicationContext (outside a DispatcherServlet).
	 * @param request current HTTP request
	 * @param model the model attributes for the current view (can be {@code null},
	 * using the request attributes for Errors retrieval)
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, jakarta.servlet.ServletContext, Map)
	 */
	public RequestContext(HttpServletRequest request, @Nullable Map<String, Object> model) {
		this(request, null, request.getServletContext(), model);
	}

	/**
	 * Create a new RequestContext for the given request, using the given model attributes for Errors retrieval.
	 * <p>This works with all View implementations. It will typically be used by View implementations.
	 * <p>If a ServletContext is specified, the RequestContext will also work with a root
	 * WebApplicationContext (outside a DispatcherServlet).
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param servletContext the servlet context of the web application (can be {@code null}; necessary for
	 * fallback to root WebApplicationContext)
	 * @param model the model attributes for the current view (can be {@code null}, using the request attributes
	 * for Errors retrieval)
	 * @see org.springframework.web.context.WebApplicationContext
	 * @see org.springframework.web.servlet.DispatcherServlet
	 */
	public RequestContext(HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable ServletContext servletContext, @Nullable Map<String, Object> model) {

		this.request = request;
		this.response = response;
		this.model = model;

		// Fetch WebApplicationContext, either from DispatcherServlet or the root context.
		// ServletContext needs to be specified to be able to fall back to the root context!
		WebApplicationContext wac = (WebApplicationContext) request.getAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (wac == null) {
			wac = RequestContextUtils.findWebApplicationContext(request, servletContext);
			if (wac == null) {
				throw new IllegalStateException("No WebApplicationContext found: not in a DispatcherServlet " +
						"request and no ContextLoaderListener registered?");
			}
		}
		this.webApplicationContext = wac;

		Locale locale = null;
		TimeZone timeZone = null;

		// Determine locale to use for this RequestContext.
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
		if (localeResolver instanceof LocaleContextResolver localeContextResolver) {
			LocaleContext localeContext = localeContextResolver.resolveLocaleContext(request);
			locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext timeZoneAwareLocaleContext) {
				timeZone = timeZoneAwareLocaleContext.getTimeZone();
			}
		}
		else if (localeResolver != null) {
			// Try LocaleResolver (we're within a DispatcherServlet request).
			locale = localeResolver.resolveLocale(request);
		}

		this.locale = locale;
		this.timeZone = timeZone;

		// Determine default HTML escape setting from the "defaultHtmlEscape"
		// context-param in web.xml, if any.
		this.defaultHtmlEscape = WebUtils.getDefaultHtmlEscape(this.webApplicationContext.getServletContext());

		// Determine response-encoded HTML escape setting from the "responseEncodedHtmlEscape"
		// context-param in web.xml, if any.
		this.responseEncodedHtmlEscape =
				WebUtils.getResponseEncodedHtmlEscape(this.webApplicationContext.getServletContext());

		this.urlPathHelper = new UrlPathHelper();

		if (this.webApplicationContext.containsBean(RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			this.requestDataValueProcessor = this.webApplicationContext.getBean(
					RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
		}
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
	protected final @Nullable ServletContext getServletContext() {
		return this.webApplicationContext.getServletContext();
	}

	/**
	 * Return the current WebApplicationContext.
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}

	/**
	 * Return the model Map that this RequestContext encapsulates, if any.
	 * @return the populated model Map, or {@code null} if none available
	 */
	public final @Nullable Map<String, Object> getModel() {
		return this.model;
	}

	/**
	 * Return the MessageSource to use (typically the current WebApplicationContext).
	 * <p>Note: As of 6.2, this method is non-final and therefore overridable.
	 */
	public MessageSource getMessageSource() {
		return this.webApplicationContext;
	}

	/**
	 * Return the current Locale (falling back to the request locale; never {@code null}).
	 * <p>Typically coming from a DispatcherServlet's {@link LocaleResolver}.
	 * Also includes a fallback check for JSTL's Locale attribute.
	 * <p>Note: As of 6.2, this method is non-final and therefore overridable.
	 * @see RequestContextUtils#getLocale
	 */
	public Locale getLocale() {
		return (this.locale != null ? this.locale : getFallbackLocale());
	}

	/**
	 * Return the current TimeZone (or {@code null} if none derivable from the request).
	 * <p>Typically coming from a DispatcherServlet's {@link LocaleContextResolver}.
	 * Also includes a fallback check for JSTL's TimeZone attribute.
	 * @see RequestContextUtils#getTimeZone
	 */
	public @Nullable TimeZone getTimeZone() {
		return (this.timeZone != null ? this.timeZone : getFallbackTimeZone());
	}

	/**
	 * Determine the fallback locale for this context.
	 * <p>The default implementation checks for a JSTL locale attribute in request, session
	 * or application scope; if not found, returns the {@code HttpServletRequest.getLocale()}.
	 * @return the fallback locale (never {@code null})
	 * @see jakarta.servlet.http.HttpServletRequest#getLocale()
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
	 * Determine the fallback time zone for this context.
	 * <p>The default implementation checks for a JSTL time zone attribute in request,
	 * session or application scope; returns {@code null} if not found.
	 * @return the fallback time zone (or {@code null} if none derivable from the request)
	 */
	protected @Nullable TimeZone getFallbackTimeZone() {
		if (jstlPresent) {
			TimeZone timeZone = JstlLocaleResolver.getJstlTimeZone(getRequest(), getServletContext());
			if (timeZone != null) {
				return timeZone;
			}
		}
		return null;
	}

	/**
	 * Change the current locale to the specified one,
	 * storing the new locale through the configured {@link LocaleResolver}.
	 * @param locale the new locale
	 * @see LocaleResolver#setLocale
	 * @see #changeLocale(java.util.Locale, java.util.TimeZone)
	 */
	public void changeLocale(Locale locale) {
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(this.request);
		if (localeResolver == null) {
			throw new IllegalStateException("Cannot change locale if no LocaleResolver configured");
		}
		localeResolver.setLocale(this.request, this.response, locale);
		this.locale = locale;
	}

	/**
	 * Change the current locale to the specified locale and time zone context,
	 * storing the new locale context through the configured {@link LocaleResolver}.
	 * @param locale the new locale
	 * @param timeZone the new time zone
	 * @see LocaleContextResolver#setLocaleContext
	 * @see org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext
	 */
	public void changeLocale(Locale locale, TimeZone timeZone) {
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(this.request);
		if (!(localeResolver instanceof LocaleContextResolver localeContextResolver)) {
			throw new IllegalStateException("Cannot change locale context if no LocaleContextResolver configured");
		}
		localeContextResolver.setLocaleContext(this.request, this.response,
				new SimpleTimeZoneAwareLocaleContext(locale, timeZone));
		this.locale = locale;
		this.timeZone = timeZone;
	}

	/**
	 * (De)activate default HTML escaping for messages and errors, for the scope of this RequestContext.
	 * <p>The default is the application-wide setting (the "defaultHtmlEscape" context-param in web.xml).
	 * @see org.springframework.web.util.WebUtils#getDefaultHtmlEscape
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}

	/**
	 * Is default HTML escaping active? Falls back to {@code false} in case of no explicit default given.
	 */
	public boolean isDefaultHtmlEscape() {
		return (this.defaultHtmlEscape != null && this.defaultHtmlEscape);
	}

	/**
	 * Return the default HTML escape setting, differentiating between no default specified and an explicit value.
	 * @return whether default HTML escaping is enabled (null = no explicit default)
	 */
	public @Nullable Boolean getDefaultHtmlEscape() {
		return this.defaultHtmlEscape;
	}

	/**
	 * Is HTML escaping using the response encoding by default?
	 * If enabled, only XML markup significant characters will be escaped with UTF-* encodings.
	 * <p>Falls back to {@code true} in case of no explicit default given, as of Spring 4.2.
	 * @since 4.1.2
	 */
	public boolean isResponseEncodedHtmlEscape() {
		return (this.responseEncodedHtmlEscape == null || this.responseEncodedHtmlEscape);
	}

	/**
	 * Return the default setting about use of response encoding for HTML escape setting,
	 * differentiating between no default specified and an explicit value.
	 * @return whether default use of response encoding HTML escaping is enabled (null = no explicit default)
	 * @since 4.1.2
	 */
	public @Nullable Boolean getResponseEncodedHtmlEscape() {
		return this.responseEncodedHtmlEscape;
	}


	/**
	 * Set the UrlPathHelper to use for context path and request URI decoding.
	 * Can be used to pass a shared UrlPathHelper instance in.
	 * <p>A default UrlPathHelper is always available.
	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
	 * for use at runtime in web modules in favor of parsed patterns with
	 * {@link PathPatternParser}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * Return the UrlPathHelper used for context path and request URI decoding.
	 * Can be used to configure the current UrlPathHelper.
	 * <p>A default UrlPathHelper is always available.
	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
	 * for use at runtime in web modules in favor of parsed patterns with
	 * {@link PathPatternParser}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * Return the RequestDataValueProcessor instance to use obtained from the
	 * WebApplicationContext under the name {@code "requestDataValueProcessor"}.
	 * Or {@code null} if no matching bean was found.
	 */
	public @Nullable RequestDataValueProcessor getRequestDataValueProcessor() {
		return this.requestDataValueProcessor;
	}

	/**
	 * Return the context path of the original request, that is, the path that
	 * indicates the current web application. This is useful for building links
	 * to other resources within the application.
	 * <p>Delegates to the UrlPathHelper for decoding.
	 * @see jakarta.servlet.http.HttpServletRequest#getContextPath
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
	 * @param relativeUrl the relative URL part
	 * @param params a map of parameters to insert as placeholders in the url
	 * @return a URL that points back to the server with an absolute path (also URL-encoded accordingly)
	 */
	public String getContextUrl(String relativeUrl, Map<String, ?> params) {
		String url = getContextPath() + relativeUrl;
		url = UriComponentsBuilder.fromUriString(url).buildAndExpand(params).encode().toUri().toASCIIString();
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
	 * Return the request URI of the original request, that is, the invoked URL
	 * without parameters. This is particularly useful as HTML form action target,
	 * possibly in combination with the original query string.
	 * <p>Delegates to the UrlPathHelper for decoding.
	 * @see #getQueryString
	 * @see org.springframework.web.util.UrlPathHelper#getOriginatingRequestUri
	 * @see #getUrlPathHelper
	 */
	public String getRequestUri() {
		return this.urlPathHelper.getOriginatingRequestUri(this.request);
	}

	/**
	 * Return the query string of the current request, that is, the part after
	 * the request path. This is particularly useful for building an HTML form
	 * action target in combination with the original request URI.
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
	 * @param code the code of the message
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, String defaultMessage) {
		return getMessage(code, null, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, Object @Nullable [] args, String defaultMessage) {
		return getMessage(code, args, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code the code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, @Nullable List<?> args, String defaultMessage) {
		return getMessage(code, (args != null ? args.toArray() : null), defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code.
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @param htmlEscape if the message should be HTML-escaped
	 * @return the message
	 */
	public String getMessage(String code, Object @Nullable [] args, String defaultMessage, boolean htmlEscape) {
		String msg = getMessageSource().getMessage(code, args, defaultMessage, getLocale());
		if (msg == null) {
			return "";
		}
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code the code of the message
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return getMessage(code, null, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, Object @Nullable [] args) throws NoSuchMessageException {
		return getMessage(code, args, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code, using the "defaultHtmlEscape" setting.
	 * @param code the code of the message
	 * @param args arguments for the message as a List, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, @Nullable List<?> args) throws NoSuchMessageException {
		return getMessage(code, (args != null ? args.toArray() : null), isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the message for the given code.
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param htmlEscape if the message should be HTML-escaped
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, Object @Nullable [] args, boolean htmlEscape) throws NoSuchMessageException {
		String msg = getMessageSource().getMessage(code, args, getLocale());
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * Retrieve the given MessageSourceResolvable (for example, an ObjectError instance), using the "defaultHtmlEscape" setting.
	 * @param resolvable the MessageSourceResolvable
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getMessage(resolvable, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the given MessageSourceResolvable (for example, an ObjectError instance).
	 * @param resolvable the MessageSourceResolvable
	 * @param htmlEscape if the message should be HTML-escaped
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable, boolean htmlEscape) throws NoSuchMessageException {
		String msg = getMessageSource().getMessage(resolvable, getLocale());
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * Retrieve the Errors instance for the given bind object, using the "defaultHtmlEscape" setting.
	 * @param name the name of the bind object
	 * @return the Errors instance, or {@code null} if not found
	 */
	public @Nullable Errors getErrors(String name) {
		return getErrors(name, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the Errors instance for the given bind object.
	 * @param name the name of the bind object
	 * @param htmlEscape create an Errors instance with automatic HTML escaping?
	 * @return the Errors instance, or {@code null} if not found
	 */
	public @Nullable Errors getErrors(String name, boolean htmlEscape) {
		if (this.errorsMap == null) {
			this.errorsMap = new HashMap<>();
		}
		Errors errors = this.errorsMap.get(name);
		boolean put = false;
		if (errors == null) {
			errors = (Errors) getModelObject(BindingResult.MODEL_KEY_PREFIX + name);
			// Check old BindException prefix for backwards compatibility.
			if (errors instanceof BindException bindException) {
				errors = bindException.getBindingResult();
			}
			if (errors == null) {
				return null;
			}
			put = true;
		}
		if (htmlEscape && !(errors instanceof EscapedErrors)) {
			errors = new EscapedErrors(errors);
			put = true;
		}
		else if (!htmlEscape && errors instanceof EscapedErrors escapedErrors) {
			errors = escapedErrors.getSource();
			put = true;
		}
		if (put) {
			this.errorsMap.put(name, errors);
		}
		return errors;
	}

	/**
	 * Retrieve the model object for the given model name, either from the model
	 * or from the request attributes.
	 * @param modelName the name of the model object
	 * @return the model object
	 */
	protected @Nullable Object getModelObject(String modelName) {
		if (this.model != null) {
			return this.model.get(modelName);
		}
		else {
			return this.request.getAttribute(modelName);
		}
	}

	/**
	 * Create a BindStatus for the given bind object, using the "defaultHtmlEscape" setting.
	 * @param path the bean and property path for which values and errors will be resolved (for example, "person.age")
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path) throws IllegalStateException {
		return new BindStatus(this, path, isDefaultHtmlEscape());
	}

	/**
	 * Create a BindStatus for the given bind object, using the "defaultHtmlEscape" setting.
	 * @param path the bean and property path for which values and errors will be resolved (for example, "person.age")
	 * @param htmlEscape create a BindStatus with automatic HTML escaping?
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(this, path, htmlEscape);
	}


	/**
	 * Inner class that isolates the JSTL dependency.
	 * Just called to resolve the fallback locale if the JSTL API is present.
	 */
	private static class JstlLocaleResolver {

		public static @Nullable Locale getJstlLocale(HttpServletRequest request, @Nullable ServletContext servletContext) {
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
			return (localeObject instanceof Locale locale ? locale : null);
		}

		public static @Nullable TimeZone getJstlTimeZone(HttpServletRequest request, @Nullable ServletContext servletContext) {
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
			return (timeZoneObject instanceof TimeZone timeZone ? timeZone : null);
		}
	}

}
