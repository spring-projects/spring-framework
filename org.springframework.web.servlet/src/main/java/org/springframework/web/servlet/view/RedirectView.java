/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

/**
 * <p>View that redirects to an absolute, context relative, or current request
 * relative URL. The URL may be a URI template in which case the URI template 
 * variables will be replaced with values available in the model. By default 
 * all primitive model attributes (or collections thereof) are exposed as HTTP 
 * query parameters (assuming they've not been used as URI template variables), 
 * but this behavior can be changed by overriding the 
 * {@link #isEligibleProperty(String, Object)} method.
 * 
 * <p>A URL for this view is supposed to be a HTTP redirect URL, i.e.
 * suitable for HttpServletResponse's <code>sendRedirect</code> method, which
 * is what actually does the redirect if the HTTP 1.0 flag is on, or via sending
 * back an HTTP 303 code - if the HTTP 1.0 compatibility flag is off.
 *
 * <p>Note that while the default value for the "contextRelative" flag is off,
 * you will probably want to almost always set it to true. With the flag off,
 * URLs starting with "/" are considered relative to the web server root, while
 * with the flag on, they are considered relative to the web application root.
 * Since most web applications will never know or care what their context path
 * actually is, they are much better off setting this flag to true, and submitting
 * paths which are to be considered relative to the web application root.
 *
 * <p><b>NOTE when using this redirect view in a Portlet environment:</b> Make sure
 * that your controller respects the Portlet <code>sendRedirect</code> constraints.
 * When e.g. using {@link org.springframework.web.portlet.mvc.SimpleFormController},
 * make sure to set your controller's
 * {@link org.springframework.web.portlet.mvc.AbstractFormController#setRedirectAction "redirectAction"}
 * property to "true", in order to make the controller base class behave accordingly.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Sam Brannen
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see #setContextRelative
 * @see #setHttp10Compatible
 * @see #setExposeModelAttributes
 * @see javax.servlet.http.HttpServletResponse#sendRedirect
 */
public class RedirectView extends AbstractUrlBasedView implements SmartView {

	private static final Pattern URI_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

	private boolean contextRelative = false;

	private boolean http10Compatible = true;

	private boolean exposeModelAttributes = true;

	private String encodingScheme;

	private HttpStatus statusCode;


	/**
	 * Constructor for use as a bean.
	 */
	public RedirectView() {
		setExposePathVariables(false);
	}

	/**
	 * Create a new RedirectView with the given URL.
	 * <p>The given URL will be considered as relative to the web server,
	 * not as relative to the current ServletContext.
	 * @param url the URL to redirect to
	 * @see #RedirectView(String, boolean)
	 */
	public RedirectView(String url) {
		super(url);
		setExposePathVariables(false);
	}

	/**
	 * Create a new RedirectView with the given URL.
	 * @param url the URL to redirect to
	 * @param contextRelative whether to interpret the given URL as
	 * relative to the current ServletContext
	 */
	public RedirectView(String url, boolean contextRelative) {
		super(url);
		this.contextRelative = contextRelative;
		setExposePathVariables(false);
	}

	/**
	 * Create a new RedirectView with the given URL.
	 * @param url the URL to redirect to
	 * @param contextRelative whether to interpret the given URL as
	 * relative to the current ServletContext
	 * @param http10Compatible whether to stay compatible with HTTP 1.0 clients
	 */
	public RedirectView(String url, boolean contextRelative, boolean http10Compatible) {
		super(url);
		this.contextRelative = contextRelative;
		this.http10Compatible = http10Compatible;
		setExposePathVariables(false);
	}

	/**
	 * Create a new RedirectView with the given URL.
	 * @param url the URL to redirect to
	 * @param contextRelative whether to interpret the given URL as
	 * relative to the current ServletContext
	 * @param http10Compatible whether to stay compatible with HTTP 1.0 clients
	 * @param exposeModelAttributes whether or not model attributes should be
	 * exposed as query parameters
	 */
	public RedirectView(String url, boolean contextRelative, boolean http10Compatible, boolean exposeModelAttributes) {
		super(url);
		this.contextRelative = contextRelative;
		this.http10Compatible = http10Compatible;
		this.exposeModelAttributes = exposeModelAttributes;
		setExposePathVariables(false);
	}


	/**
	 * Set whether to interpret a given URL that starts with a slash ("/")
	 * as relative to the current ServletContext, i.e. as relative to the
	 * web application root.
	 * <p>Default is "false": A URL that starts with a slash will be interpreted
	 * as absolute, i.e. taken as-is. If "true", the context path will be
	 * prepended to the URL in such a case.
	 * @see javax.servlet.http.HttpServletRequest#getContextPath
	 */
	public void setContextRelative(boolean contextRelative) {
		this.contextRelative = contextRelative;
	}

	/**
	 * Set whether to stay compatible with HTTP 1.0 clients.
	 * <p>In the default implementation, this will enforce HTTP status code 302
	 * in any case, i.e. delegate to <code>HttpServletResponse.sendRedirect</code>.
	 * Turning this off will send HTTP status code 303, which is the correct
	 * code for HTTP 1.1 clients, but not understood by HTTP 1.0 clients.
	 * <p>Many HTTP 1.1 clients treat 302 just like 303, not making any
	 * difference. However, some clients depend on 303 when redirecting
	 * after a POST request; turn this flag off in such a scenario.
	 * @see javax.servlet.http.HttpServletResponse#sendRedirect
	 */
	public void setHttp10Compatible(boolean http10Compatible) {
		this.http10Compatible = http10Compatible;
	}

	/**
	 * Set the <code>exposeModelAttributes</code> flag which denotes whether
	 * or not model attributes should be exposed as HTTP query parameters.
	 * <p>Defaults to <code>true</code>.
	 */
	public void setExposeModelAttributes(final boolean exposeModelAttributes) {
		this.exposeModelAttributes = exposeModelAttributes;
	}

	/**
	 * Set the encoding scheme for this view.
	 * <p>Default is the request's encoding scheme
	 * (which is ISO-8859-1 if not specified otherwise).
	 */
	public void setEncodingScheme(String encodingScheme) {
		this.encodingScheme = encodingScheme;
	}

	/**
	 * Set the status code for this view.
	 * <p>Default is to send 302/303, depending on the value of the
	 * {@link #setHttp10Compatible(boolean) http10Compatible} flag.
	 */
	public void setStatusCode(HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Returns "true" indicating this view performs a redirect.
	 */
	public boolean isRedirectView() {
		return true;
	}

	/**
	 * An ApplicationContext is not strictly required for RedirectView.
	 */
	@Override
	protected boolean isContextRequired() {
		return false;
	}

	/**
	 * Convert model to request parameters and redirect to the given URL.
	 * @see #appendQueryProperties
	 * @see #sendRedirect
	 */
	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		String targetUrl = createTargetUrl(model, request);

		targetUrl = updateTargetUrl(targetUrl, model, request, response);
		
		FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
		if (!CollectionUtils.isEmpty(flashMap)) {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(targetUrl).build();
			flashMap.setTargetRequestPath(uriComponents.getPath());
			flashMap.addTargetRequestParams(uriComponents.getQueryParams());
		}
		
		sendRedirect(request, response, targetUrl.toString(), this.http10Compatible);
	}

	/**
	 * Creates the target URL by checking if the redirect string is a URI template first, 
	 * expanding it with the given model, and then optionally appending simple type model 
	 * attributes as query String parameters.
	 */
	protected final String createTargetUrl(Map<String, Object> model, HttpServletRequest request)
			throws UnsupportedEncodingException {
		
		// Prepare target URL.
		StringBuilder targetUrl = new StringBuilder();
		if (this.contextRelative && getUrl().startsWith("/")) {
			// Do not apply context path to relative URLs.
			targetUrl.append(request.getContextPath());
		}
		targetUrl.append(getUrl());

		String enc = this.encodingScheme;
		if (enc == null) {
			enc = request.getCharacterEncoding();
		}
		if (enc == null) {
			enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
		}

		if (StringUtils.hasText(targetUrl)) {
			Map<String, String> variables = getCurrentRequestUriVariables(request);
			targetUrl = replaceUriTemplateVariables(targetUrl.toString(), model, variables, enc);
		}
		
		if (this.exposeModelAttributes) {
			appendQueryProperties(targetUrl, model, enc);
		}

		return targetUrl.toString();
	}

	/**
	 * Replace URI template variables in the target URL with encoded model 
	 * attributes or URI variables from the current request. Model attributes
	 * referenced in the URL are removed from the model. 
	 * @param targetUrl the redirect URL
	 * @param model Map that contains model attributes
	 * @param currentUriVariables current request URI variables to use
	 * @param encodingScheme the encoding scheme to use
	 * @throws UnsupportedEncodingException if string encoding failed
	 */
	protected StringBuilder replaceUriTemplateVariables(
			String targetUrl, Map<String, Object> model, Map<String, String> currentUriVariables, String encodingScheme)
			throws UnsupportedEncodingException {
		
		StringBuilder result = new StringBuilder();
		Matcher m = URI_TEMPLATE_VARIABLE_PATTERN.matcher(targetUrl);
		int endLastMatch = 0;
		while (m.find()) {
			String name = m.group(1);
			Object value = model.containsKey(name) ? model.remove(name) : currentUriVariables.get(name);
			Assert.notNull(value, "Model has no value for '" + name + "'");
			result.append(targetUrl.substring(endLastMatch, m.start()));
			result.append(UriUtils.encodePathSegment(value.toString(), encodingScheme));
			endLastMatch = m.end();
		}
		result.append(targetUrl.substring(endLastMatch, targetUrl.length()));
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getCurrentRequestUriVariables(HttpServletRequest request) {
		Map<String, String> uriVars = 
			(Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		return (uriVars != null) ? uriVars : Collections.<String, String> emptyMap();
	}
	
	/**
	 * Append query properties to the redirect URL.
	 * Stringifies, URL-encodes and formats model attributes as query properties.
	 * @param targetUrl the StringBuilder to append the properties to
	 * @param model Map that contains model attributes
	 * @param encodingScheme the encoding scheme to use
	 * @throws UnsupportedEncodingException if string encoding failed
	 * @see #queryProperties
	 */
	protected void appendQueryProperties(StringBuilder targetUrl, Map<String, Object> model, String encodingScheme)
			throws UnsupportedEncodingException {

		// Extract anchor fragment, if any.
		String fragment = null;
		int anchorIndex = targetUrl.indexOf("#");
		if (anchorIndex > -1) {
			fragment = targetUrl.substring(anchorIndex);
			targetUrl.delete(anchorIndex, targetUrl.length());
		}

		// If there aren't already some parameters, we need a "?".
		boolean first = (targetUrl.toString().indexOf('?') < 0);
		for (Map.Entry<String, Object> entry : queryProperties(model).entrySet()) {
			Object rawValue = entry.getValue();
			Iterator<Object> valueIter;
			if (rawValue != null && rawValue.getClass().isArray()) {
				valueIter = Arrays.asList(ObjectUtils.toObjectArray(rawValue)).iterator();
			}
			else if (rawValue instanceof Collection) {
				valueIter = ((Collection) rawValue).iterator();
			}
			else {
				valueIter = Collections.singleton(rawValue).iterator();
			}
			while (valueIter.hasNext()) {
				Object value = valueIter.next();
				if (first) {
					targetUrl.append('?');
					first = false;
				}
				else {
					targetUrl.append('&');
				}
				String encodedKey = urlEncode(entry.getKey(), encodingScheme);
				String encodedValue = (value != null ? urlEncode(value.toString(), encodingScheme) : "");
				targetUrl.append(encodedKey).append('=').append(encodedValue);
			}
		}

		// Append anchor fragment, if any, to end of URL.
		if (fragment != null) {
			targetUrl.append(fragment);
		}
	}

	/**
	 * Determine name-value pairs for query strings, which will be stringified,
	 * URL-encoded and formatted by {@link #appendQueryProperties}.
	 * <p>This implementation filters the model through checking
	 * {@link #isEligibleProperty(String, Object)} for each element,
	 * by default accepting Strings, primitives and primitive wrappers only.
	 * @param model the original model Map
	 * @return the filtered Map of eligible query properties
	 * @see #isEligibleProperty(String, Object)
	 */
	protected Map<String, Object> queryProperties(Map<String, Object> model) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			if (isEligibleProperty(entry.getKey(), entry.getValue())) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Determine whether the given model element should be exposed
	 * as a query property.
	 * <p>The default implementation considers Strings and primitives
	 * as eligible, and also arrays and Collections/Iterables with
	 * corresponding elements. This can be overridden in subclasses.
	 * @param key the key of the model element
	 * @param value the value of the model element
	 * @return whether the element is eligible as query property
	 */
	protected boolean isEligibleProperty(String key, Object value) {
		if (value == null) {
			return false;
		}
		if (isEligibleValue(value)) {
			return true;
		}

		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			if (length == 0) {
				return false;
			}
			for (int i = 0; i < length; i++) {
				Object element = Array.get(value, i);
				if (!isEligibleValue(element)) {
					return false;
				}
			}
			return true;
		}

		if (value instanceof Collection) {
			Collection coll = (Collection) value;
			if (coll.isEmpty()) {
				return false;
			}
			for (Object element : coll) {
				if (!isEligibleValue(element)) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	/**
	 * Determine whether the given model element value is eligible for exposure.
	 * <p>The default implementation considers primitives, Strings, Numbers, Dates,
	 * URIs, URLs and Locale objects as eligible. This can be overridden in subclasses.
	 * @param value the model element value
	 * @return whether the element value is eligible
	 * @see BeanUtils#isSimpleValueType
	 */
	protected boolean isEligibleValue(Object value) {
		return (value != null && BeanUtils.isSimpleValueType(value.getClass()));
	}

	/**
	 * URL-encode the given input String with the given encoding scheme.
	 * <p>The default implementation uses <code>URLEncoder.encode(input, enc)</code>.
	 * @param input the unencoded input String
	 * @param encodingScheme the encoding scheme
	 * @return the encoded output String
	 * @throws UnsupportedEncodingException if thrown by the JDK URLEncoder
	 * @see java.net.URLEncoder#encode(String, String)
	 * @see java.net.URLEncoder#encode(String)
	 */
	protected String urlEncode(String input, String encodingScheme) throws UnsupportedEncodingException {
		return (input != null ? URLEncoder.encode(input, encodingScheme) : null);
	}

	/**
	 * Find the registered {@link RequestDataValueProcessor}, if any, and allow
	 * it to update the redirect target URL.
	 * @return the updated URL or the same as URL as the one passed in
	 */
	protected String updateTargetUrl(String targetUrl, Map<String, Object> model, 
								  HttpServletRequest request, HttpServletResponse response) {
		
		RequestContext requestContext = null;
		if (getWebApplicationContext() != null) {
			requestContext = createRequestContext(request, response, model);
		}
		else {
			WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
			if (wac != null && wac.getServletContext() != null) {
				requestContext = new RequestContext(request, response, wac.getServletContext(), model);
			}
		}

		if (requestContext != null) {
			RequestDataValueProcessor processor = requestContext.getRequestDataValueProcessor();
			if (processor != null) {
				targetUrl = processor.processUrl(request, targetUrl);
			}
		}
		
		return targetUrl;
	}
	
	/**
	 * Send a redirect back to the HTTP client
	 * @param request current HTTP request (allows for reacting to request method)
	 * @param response current HTTP response (for sending response headers)
	 * @param targetUrl the target URL to redirect to
	 * @param http10Compatible whether to stay compatible with HTTP 1.0 clients
	 * @throws IOException if thrown by response methods
	 */
	protected void sendRedirect(
			HttpServletRequest request, HttpServletResponse response, String targetUrl, boolean http10Compatible)
			throws IOException {

		String encodedRedirectURL = response.encodeRedirectURL(targetUrl);
		
		if (http10Compatible) {
			if (this.statusCode != null) {
				response.setStatus(this.statusCode.value());
				response.setHeader("Location", encodedRedirectURL);
			}
			else {
				// Send status code 302 by default.
				response.sendRedirect(encodedRedirectURL);
			}
		}
		else {
			HttpStatus statusCode = getHttp11StatusCode(request, response, targetUrl);
			response.setStatus(statusCode.value());
			response.setHeader("Location", encodedRedirectURL);
		}
	}

	/**
	 * Determines the status code to use for HTTP 1.1 compatible requests.
	 * <p>The default implementation returns the {@link #setStatusCode(HttpStatus) statusCode}
	 * property if set, or the value of the {@link #RESPONSE_STATUS_ATTRIBUTE} attribute.
	 * If neither are set, it defaults to {@link HttpStatus#SEE_OTHER} (303).
	 * @param request the request to inspect
	 * @param response the servlet response
	 * @param targetUrl the target URL
	 * @return the response status
	 */
	protected HttpStatus getHttp11StatusCode(
			HttpServletRequest request, HttpServletResponse response, String targetUrl) {

		if (this.statusCode != null) {
			return this.statusCode;
		}
		HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);
		if (attributeStatusCode != null) {
			return attributeStatusCode;
		}
		return HttpStatus.SEE_OTHER;
	}

}
