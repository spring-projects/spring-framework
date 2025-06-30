/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.jspecify.annotations.Nullable;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Context holder for request-specific state, like the {@link MessageSource} to
 * use, current locale, binding errors, etc. Provides easy access to localized
 * messages and Errors instances.
 *
 * <p>Suitable for exposition to views, and usage within FreeMarker templates
 * and tag libraries.
 *
 * <p>Can be instantiated manually or automatically exposed to views as a model
 * attribute via AbstractView's "requestContextAttribute" property.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestContext {

	private final ServerWebExchange exchange;

	private final Map<String, Object> model;

	private final MessageSource messageSource;

	private Locale locale;

	private TimeZone timeZone;

	private @Nullable Boolean defaultHtmlEscape;

	private @Nullable Map<String, Errors> errorsMap;

	private final @Nullable RequestDataValueProcessor dataValueProcessor;


	public RequestContext(ServerWebExchange exchange, Map<String, Object> model, MessageSource messageSource) {
		this(exchange, model, messageSource, null);
	}

	public RequestContext(ServerWebExchange exchange, Map<String, Object> model, MessageSource messageSource,
			@Nullable RequestDataValueProcessor dataValueProcessor) {

		Assert.notNull(exchange, "ServerWebExchange is required");
		Assert.notNull(model, "Model is required");
		Assert.notNull(messageSource, "MessageSource is required");
		this.exchange = exchange;
		this.model = model;
		this.messageSource = messageSource;

		LocaleContext localeContext = exchange.getLocaleContext();
		Locale locale = localeContext.getLocale();
		this.locale = (locale != null ? locale : Locale.getDefault());
		TimeZone timeZone = (localeContext instanceof TimeZoneAwareLocaleContext tzaLocaleContext ?
				tzaLocaleContext.getTimeZone() : null);
		this.timeZone = (timeZone != null ? timeZone : TimeZone.getDefault());

		this.defaultHtmlEscape = null;  // TODO
		this.dataValueProcessor = dataValueProcessor;
	}


	protected final ServerWebExchange getExchange() {
		return this.exchange;
	}

	/**
	 * Return the MessageSource in use with this request.
	 */
	public MessageSource getMessageSource() {
		return this.messageSource;
	}

	/**
	 * Return the model Map that this RequestContext encapsulates, if any.
	 * @return the populated model Map, or {@code null} if none available
	 */
	public @Nullable Map<String, Object> getModel() {
		return this.model;
	}

	/**
	 * Return the current Locale.
	 */
	public final Locale getLocale() {
		return this.locale;
	}

	/**
	 * Return the current TimeZone.
	 */
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	/**
	 * Change the current locale to the specified one.
	 */
	public void changeLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * Change the current locale to the specified locale and time zone context.
	 */
	public void changeLocale(Locale locale, TimeZone timeZone) {
		this.locale = locale;
		this.timeZone = timeZone;
	}

	/**
	 * (De)activate default HTML escaping for messages and errors, for the scope
	 * of this RequestContext.
	 * <p>TODO: currently no application-wide setting ...
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}

	/**
	 * Is default HTML escaping active? Falls back to {@code false} in case of
	 * no explicit default given.
	 */
	public boolean isDefaultHtmlEscape() {
		return (this.defaultHtmlEscape != null && this.defaultHtmlEscape);
	}

	/**
	 * Return the default HTML escape setting, differentiating between no default
	 * specified and an explicit value.
	 * @return whether default HTML escaping is enabled (null = no explicit default)
	 */
	public @Nullable Boolean getDefaultHtmlEscape() {
		return this.defaultHtmlEscape;
	}

	/**
	 * Return the {@link RequestDataValueProcessor} instance to apply to in form
	 * tag libraries and to redirect URLs.
	 */
	public @Nullable RequestDataValueProcessor getRequestDataValueProcessor() {
		return this.dataValueProcessor;
	}

	/**
	 * Return the context path of the current web application. This is
	 * useful for building links to other resources within the application.
	 * <p>Delegates to {@link ServerHttpRequest#getPath()}.
	 */
	public String getContextPath() {
		return this.exchange.getRequest().getPath().contextPath().value();
	}

	/**
	 * Return a context-aware URl for the given relative URL.
	 * @param relativeUrl the relative URL part
	 * @return a URL that points back to the current web application with an
	 * absolute path also URL-encoded accordingly
	 */
	public String getContextUrl(String relativeUrl) {
		String url = StringUtils.applyRelativePath(getContextPath() + "/", relativeUrl);
		return getExchange().transformUrl(url);
	}

	/**
	 * Return a context-aware URl for the given relative URL with placeholders --
	 * named keys with braces {@code {}}. For example, send in a relative URL
	 * {@code foo/{bar}?spam={spam}} and a parameter map {@code {bar=baz,spam=nuts}}
	 * and the result will be {@code [contextpath]/foo/baz?spam=nuts}.
	 * @param relativeUrl the relative URL part
	 * @param params a map of parameters to insert as placeholders in the url
	 * @return a URL that points back to the current web application with an
	 * absolute path also URL-encoded accordingly
	 */
	public String getContextUrl(String relativeUrl, Map<String, ?> params) {
		String url = StringUtils.applyRelativePath(getContextPath() + "/", relativeUrl);
		url = UriComponentsBuilder.fromUriString(url).buildAndExpand(params).encode().toUri().toASCIIString();
		return getExchange().transformUrl(url);
	}

	/**
	 * Return the request path of the request. This is useful as HTML form
	 * action target, also in combination with the original query string.
	 */
	public String getRequestPath() {
		return this.exchange.getRequest().getURI().getPath();
	}

	/**
	 * Return the query string of the current request. This is useful for
	 * building an HTML form action target in combination with the original
	 * request path.
	 */
	public String getQueryString() {
		return this.exchange.getRequest().getURI().getQuery();
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
		String msg = this.messageSource.getMessage(code, args, defaultMessage, this.locale);
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
		String msg = this.messageSource.getMessage(code, args, this.locale);
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
		String msg = this.messageSource.getMessage(resolvable, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * Retrieve the Errors instance for the given bind object, using the
	 * "defaultHtmlEscape" setting.
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
		if (errors == null) {
			errors = getModelObject(BindingResult.MODEL_KEY_PREFIX + name);
			if (errors == null) {
				return null;
			}
		}

		if (errors instanceof BindException bindException) {
			errors = bindException.getBindingResult();
		}

		if (htmlEscape && !(errors instanceof EscapedErrors)) {
			errors = new EscapedErrors(errors);
		}
		else if (!htmlEscape && errors instanceof EscapedErrors escapedErrors) {
			errors = escapedErrors.getSource();
		}

		this.errorsMap.put(name, errors);
		return errors;
	}

	/**
	 * Retrieve the model object for the given model name, either from the model
	 * or from the request attributes.
	 * @param modelName the name of the model object
	 * @return the model object
	 */
	@SuppressWarnings("unchecked")
	protected <T> @Nullable T getModelObject(String modelName) {
		T modelObject = (T) this.model.get(modelName);
		if (modelObject == null) {
			modelObject = this.exchange.getAttribute(modelName);
		}
		return modelObject;
	}

	/**
	 * Create a BindStatus for the given bind object using the
	 * "defaultHtmlEscape" setting.
	 * @param path the bean and property path for which values and errors will
	 * be resolved (for example, "person.age")
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path) throws IllegalStateException {
		return new BindStatus(this, path, isDefaultHtmlEscape());
	}

	/**
	 * Create a BindStatus for the given bind object, using the
	 * "defaultHtmlEscape" setting.
	 * @param path the bean and property path for which values and errors will
	 * be resolved (for example, "person.age")
	 * @param htmlEscape create a BindStatus with automatic HTML escaping?
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(this, path, htmlEscape);
	}

}
