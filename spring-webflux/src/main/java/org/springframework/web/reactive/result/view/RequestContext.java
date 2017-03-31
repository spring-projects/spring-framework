/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.result.view;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriTemplate;

/**
 * Context holder for request-specific state, like the {@link MessageSource} to
 * use, current locale, binding errors, etc. Provides easy access to localized
 * messages and Errors instances.
 *
 * <p>Suitable for exposition to views, and usage within FreeMarker templates,
 * and tag libraries.
 *
 * <p>Can be instantiated manually, or automatically exposed to views as model
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

	private Boolean defaultHtmlEscape;

	private Map<String, Errors> errorsMap;

	private RequestDataValueProcessor dataValueProcessor;


	public RequestContext(ServerWebExchange exchange, Map<String, Object> model, MessageSource messageSource) {
		this(exchange, model, messageSource, null);
	}

	public RequestContext(ServerWebExchange exchange, Map<String, Object> model, MessageSource messageSource,
			RequestDataValueProcessor dataValueProcessor) {

		Assert.notNull(exchange, "'exchange' is required");
		Assert.notNull(model, "'model' is required");
		Assert.notNull(messageSource, "'messageSource' is required");
		this.exchange = exchange;
		this.model = model;
		this.messageSource = messageSource;

		Locale acceptLocale = exchange.getRequest().getHeaders().getAcceptLanguageAsLocale();
		this.locale = acceptLocale != null ? acceptLocale : Locale.getDefault();
		this.timeZone = TimeZone.getDefault(); // TODO

		this.defaultHtmlEscape = null; // TODO
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
	public Map<String, Object> getModel() {
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
	 * TODO: currently this is the Timezone.getDefault()
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
		return (this.defaultHtmlEscape != null && this.defaultHtmlEscape.booleanValue());
	}

	/**
	 * Return the default HTML escape setting, differentiating between no default
	 * specified and an explicit value.
	 * @return whether default HTML escaping is enabled (null = no explicit default)
	 */
	public Boolean getDefaultHtmlEscape() {
		return this.defaultHtmlEscape;
	}

	/**
	 * Return the {@link RequestDataValueProcessor} instance to apply to in form
	 * tag libraries and to redirect URLs.
	 */
	public Optional<RequestDataValueProcessor> getRequestDataValueProcessor() {
		return Optional.ofNullable(this.dataValueProcessor);
	}

	/**
	 * Return the context path of the current web application. This is
	 * useful for building links to other resources within the application.
	 * <p>Delegates to {@link ServerHttpRequest#getContextPath()}.
	 */
	public String getContextPath() {
		return this.exchange.getRequest().getContextPath();
	}

	/**
	 * Return a context-aware URl for the given relative URL.
	 * @param relativeUrl the relative URL part
	 * @return a URL that points back to the current web application with an
	 * absolute path also URL-encoded accordingly
	 */
	public String getContextUrl(String relativeUrl) {
		String url = getContextPath() + relativeUrl;
		return getExchange().getResponse().encodeUrl(url);
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
		String url = getContextPath() + relativeUrl;
		UriTemplate template = new UriTemplate(url);
		url = template.expand(params).toASCIIString();
		return getExchange().getResponse().encodeUrl(url);
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
	public String getMessage(String code, List<?> args, String defaultMessage) {
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
		String msg = this.messageSource.getMessage(code, args, defaultMessage, this.locale);
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
	public String getMessage(String code, List<?> args) throws NoSuchMessageException {
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
		String msg = this.messageSource.getMessage(code, args, this.locale);
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
		String msg = this.messageSource.getMessage(resolvable, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * Retrieve the Errors instance for the given bind object, using the
	 * "defaultHtmlEscape" setting.
	 * @param name name of the bind object
	 * @return the Errors instance, or {@code null} if not found
	 */
	public Optional<Errors> getErrors(String name) {
		return getErrors(name, isDefaultHtmlEscape());
	}

	/**
	 * Retrieve the Errors instance for the given bind object.
	 * @param name name of the bind object
	 * @param htmlEscape create an Errors instance with automatic HTML escaping?
	 * @return the Errors instance, or {@code null} if not found
	 */
	public Optional<Errors> getErrors(String name, boolean htmlEscape) {
		if (this.errorsMap == null) {
			this.errorsMap = new HashMap<>();
		}

		// Since there is no Optional orElse + flatMap...
		Optional<Errors> optional = Optional.ofNullable(this.errorsMap.get(name));
		optional = optional.isPresent() ? optional : getModelObject(BindingResult.MODEL_KEY_PREFIX + name);

		return optional
				.map(errors -> {
					if (errors instanceof BindException) {
						return ((BindException) errors).getBindingResult();
					}
					else {
						return errors;
					}
				})
				.map(errors -> {
					if (htmlEscape && !(errors instanceof EscapedErrors)) {
						errors = new EscapedErrors(errors);
					}
					else if (!htmlEscape && errors instanceof EscapedErrors) {
						errors = ((EscapedErrors) errors).getSource();
					}
					this.errorsMap.put(name, errors);
					return errors;
				});
	}

	/**
	 * Retrieve the model object for the given model name, either from the model
	 * or from the request attributes.
	 * @param modelName the name of the model object
	 * @return the model object
	 */
	@SuppressWarnings("unchecked")
	protected <T> Optional<T> getModelObject(String modelName) {
		return Optional.ofNullable(this.model)
				.map(model -> Optional.ofNullable((T) model.get(modelName)))
				.orElse(this.exchange.getAttribute(modelName));
	}

	/**
	 * Create a BindStatus for the given bind object using the
	 * "defaultHtmlEscape" setting.
	 * @param path the bean and property path for which values and errors will
	 * be resolved (e.g. "person.age")
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
	 * be resolved (e.g. "person.age")
	 * @param htmlEscape create a BindStatus with automatic HTML escaping?
	 * @return the new BindStatus instance
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(this, path, htmlEscape);
	}

}
