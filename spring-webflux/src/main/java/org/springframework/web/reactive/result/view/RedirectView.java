/*
 * Copyright 2002-2021 the original author or authors.
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * View that redirects to an absolute or context relative URL. The URL may be a
 * URI template in which case the URI template variables will be replaced with
 * values from the model or with URI variables from the current request.
 *
 * <p>By default {@link HttpStatus#SEE_OTHER} is used but alternate status codes
 * may be via constructor or setters arguments.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RedirectView extends AbstractUrlBasedView {

	private static final Pattern URI_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");


	private HttpStatus statusCode = HttpStatus.SEE_OTHER;

	private boolean contextRelative = true;

	private boolean propagateQuery = false;

	@Nullable
	private String[] hosts;


	/**
	 * Constructor for use as a bean.
	 */
	public RedirectView() {
	}

	/**
	 * Create a new {@code RedirectView} with the given redirect URL.
	 * Status code {@link HttpStatus#SEE_OTHER} is used by default.
	 */
	public RedirectView(String redirectUrl) {
		super(redirectUrl);
	}

	/**
	 * Create a new {@code RedirectView} with the given URL and an alternate
	 * redirect status code such as {@link HttpStatus#TEMPORARY_REDIRECT} or
	 * {@link HttpStatus#PERMANENT_REDIRECT}.
	 */
	public RedirectView(String redirectUrl, HttpStatus statusCode) {
		super(redirectUrl);
		setStatusCode(statusCode);
	}


	/**
	 * Set an alternate redirect status code such as
	 * {@link HttpStatus#TEMPORARY_REDIRECT} or
	 * {@link HttpStatus#PERMANENT_REDIRECT}.
	 */
	public void setStatusCode(HttpStatus statusCode) {
		Assert.isTrue(statusCode.is3xxRedirection(), "Not a redirect status code");
		this.statusCode = statusCode;
	}

	/**
	 * Get the redirect status code to use.
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Whether to interpret a given redirect URLs that starts with a slash ("/")
	 * as relative to the current context path ({@code true}, the default) or to
	 * the web server root ({@code false}).
	 */
	public void setContextRelative(boolean contextRelative) {
		this.contextRelative = contextRelative;
	}

	/**
	 * Whether to interpret URLs as relative to the current context path.
	 */
	public boolean isContextRelative() {
		return this.contextRelative;
	}

	/**
	 * Whether to append the query string of the current URL to the redirect URL
	 * ({@code true}) or not ({@code false}, the default).
	 */
	public void setPropagateQuery(boolean propagateQuery) {
		this.propagateQuery = propagateQuery;
	}

	/**
	 * Whether the query string of the current URL is appended to the redirect URL.
	 */
	public boolean isPropagateQuery() {
		return this.propagateQuery;
	}

	/**
	 * Configure one or more hosts associated with the application.
	 * All other hosts will be considered external hosts.
	 * <p>In effect this provides a way turn off encoding for URLs that
	 * have a host and that host is not listed as a known host.
	 * <p>If not set (the default) all redirect URLs are encoded.
	 * @param hosts one or more application hosts
	 */
	public void setHosts(@Nullable String... hosts) {
		this.hosts = hosts;
	}

	/**
	 * Return the configured application hosts.
	 */
	@Nullable
	public String[] getHosts() {
		return this.hosts;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}


	@Override
	public boolean isRedirectView() {
		return true;
	}

	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		return true;
	}

	/**
	 * Convert model to request parameters and redirect to the given URL.
	 */
	@Override
	protected Mono<Void> renderInternal(
			Map<String, Object> model, @Nullable MediaType contentType, ServerWebExchange exchange) {

		String targetUrl = createTargetUrl(model, exchange);
		return sendRedirect(targetUrl, exchange);
	}

	/**
	 * Create the target URL and, if necessary, pre-pend the contextPath, expand
	 * URI template variables, append the current request query, and apply the
	 * configured {@link #getRequestDataValueProcessor()
	 * RequestDataValueProcessor}.
	 */
	protected final String createTargetUrl(Map<String, Object> model, ServerWebExchange exchange) {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		ServerHttpRequest request = exchange.getRequest();

		StringBuilder targetUrl = new StringBuilder();
		if (isContextRelative() && url.startsWith("/")) {
			targetUrl.append(request.getPath().contextPath().value());
		}
		targetUrl.append(url);

		if (StringUtils.hasText(targetUrl)) {
			Map<String, String> uriVars = getCurrentUriVariables(exchange);
			targetUrl = expandTargetUrlTemplate(targetUrl.toString(), model, uriVars);
		}

		if (isPropagateQuery()) {
			targetUrl = appendCurrentRequestQuery(targetUrl.toString(), request);
		}

		String result = targetUrl.toString();

		RequestDataValueProcessor processor = getRequestDataValueProcessor();
		return (processor != null ? processor.processUrl(exchange, result) : result);
	}

	private Map<String, String> getCurrentUriVariables(ServerWebExchange exchange) {
		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		return exchange.getAttributeOrDefault(name, Collections.emptyMap());
	}

	/**
	 * Expand URI template variables in the target URL with either model
	 * attribute values or as a fallback with URI variable values from the
	 * current request. Values are encoded.
	 */
	protected StringBuilder expandTargetUrlTemplate(String targetUrl,
			Map<String, Object> model, Map<String, String> uriVariables) {

		Matcher matcher = URI_TEMPLATE_VARIABLE_PATTERN.matcher(targetUrl);
		boolean found = matcher.find();
		if (!found) {
			return new StringBuilder(targetUrl);
		}
		StringBuilder result = new StringBuilder();
		int endLastMatch = 0;
		while (found) {
			String name = matcher.group(1);
			Object value = (model.containsKey(name) ? model.get(name) : uriVariables.get(name));
			Assert.notNull(value, () -> "No value for URI variable '" + name + "'");
			result.append(targetUrl, endLastMatch, matcher.start());
			result.append(encodeUriVariable(value.toString()));
			endLastMatch = matcher.end();
			found = matcher.find();
		}
		result.append(targetUrl, endLastMatch, targetUrl.length());
		return result;
	}

	private String encodeUriVariable(String text) {
		// Strict encoding of all reserved URI characters
		return UriUtils.encode(text, StandardCharsets.UTF_8);
	}

	/**
	 * Append the query of the current request to the target redirect URL.
	 */
	protected StringBuilder appendCurrentRequestQuery(String targetUrl, ServerHttpRequest request) {
		String query = request.getURI().getRawQuery();
		if (!StringUtils.hasText(query)) {
			return new StringBuilder(targetUrl);
		}

		int index = targetUrl.indexOf('#');
		String fragment = (index > -1 ? targetUrl.substring(index) : null);

		StringBuilder result = new StringBuilder();
		result.append(index != -1 ? targetUrl.substring(0, index) : targetUrl);
		result.append(targetUrl.indexOf('?') < 0 ? '?' : '&').append(query);

		if (fragment != null) {
			result.append(fragment);
		}

		return result;
	}

	/**
	 * Send a redirect back to the HTTP client.
	 * @param targetUrl the target URL to redirect to
	 * @param exchange current exchange
	 */
	protected Mono<Void> sendRedirect(String targetUrl, ServerWebExchange exchange) {
		String transformedUrl = (isRemoteHost(targetUrl) ? targetUrl : exchange.transformUrl(targetUrl));
		ServerHttpResponse response = exchange.getResponse();
		response.getHeaders().setLocation(URI.create(transformedUrl));
		response.setStatusCode(getStatusCode());
		return Mono.empty();
	}

	/**
	 * Whether the given targetUrl has a host that is a "foreign" system in which
	 * case {@link jakarta.servlet.http.HttpServletResponse#encodeRedirectURL} will not be applied.
	 * This method returns {@code true} if the {@link #setHosts(String[])}
	 * property is configured and the target URL has a host that does not match.
	 * @param targetUrl the target redirect URL
	 * @return {@code true} the target URL has a remote host, {@code false} if it
	 * the URL does not have a host or the "host" property is not configured.
	 */
	protected boolean isRemoteHost(String targetUrl) {
		if (ObjectUtils.isEmpty(this.hosts)) {
			return false;
		}
		String targetHost = UriComponentsBuilder.fromUriString(targetUrl).build().getHost();
		if (!StringUtils.hasLength(targetHost)) {
			return false;
		}
		for (String host : this.hosts) {
			if (targetHost.equals(host)) {
				return false;
			}
		}
		return true;
	}

}
