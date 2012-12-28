/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.context.request;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Generic interface for a web request. Mainly intended for generic web
 * request interceptors, giving them access to general request metadata,
 * not for actual handling of the request.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see WebRequestInterceptor
 */
public interface WebRequest extends RequestAttributes {

	/**
	 * Return the request header of the given name, or {@code null} if none.
	 * <p>Retrieves the first header value in case of a multi-value header.
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeader(String)
	 */
	String getHeader(String headerName);

	/**
	 * Return the request header values for the given header name,
	 * or {@code null} if none.
	 * <p>A single-value header will be exposed as an array with a single element.
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
	 */
	String[] getHeaderValues(String headerName);

	/**
	 * Return a Iterator over request header names.
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
	 */
	Iterator<String> getHeaderNames();

	/**
	 * Return the request parameter of the given name, or {@code null} if none.
	 * <p>Retrieves the first parameter value in case of a multi-value parameter.
	 * @see javax.servlet.http.HttpServletRequest#getParameter(String)
	 */
	String getParameter(String paramName);

	/**
	 * Return the request parameter values for the given parameter name,
	 * or {@code null} if none.
	 * <p>A single-value parameter will be exposed as an array with a single element.
	 * @see javax.servlet.http.HttpServletRequest#getParameterValues(String)
	 */
	String[] getParameterValues(String paramName);

	/**
	 * Return a Iterator over request parameter names.
	 * @see javax.servlet.http.HttpServletRequest#getParameterNames()
	 * @since 3.0
	 */
	Iterator<String> getParameterNames();

	/**
	 * Return a immutable Map of the request parameters, with parameter names as map keys
	 * and parameter values as map values. The map values will be of type String array.
	 * <p>A single-value parameter will be exposed as an array with a single element.
	 * @see javax.servlet.http.HttpServletRequest#getParameterMap()
	 */
	Map<String, String[]> getParameterMap();

	/**
	 * Return the primary Locale for this request.
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	Locale getLocale();

	/**
	 * Return the context path for this request
	 * (usually the base path that the current web application is mapped to).
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	String getContextPath();

	/**
	 * Return the remote user for this request, if any.
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	String getRemoteUser();

	/**
	 * Return the user principal for this request, if any.
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	Principal getUserPrincipal();

	/**
	 * Determine whether the user is in the given role for this request.
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(String)
	 */
	boolean isUserInRole(String role);

	/**
	 * Return whether this request has been sent over a secure transport
	 * mechanism (such as SSL).
	 * @see javax.servlet.http.HttpServletRequest#isSecure()
	 */
	boolean isSecure();

	/**
	 * Check whether the request qualifies as not modified given the
	 * supplied last-modified timestamp (as determined by the application).
	 * <p>This will also transparently set the appropriate response headers,
	 * for both the modified case and the not-modified case.
	 * <p>Typical usage:
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest webRequest, Model model) {
	 *   long lastModified = // application-specific calculation
	 *   if (request.checkNotModified(lastModified)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p><strong>Note:</strong> that you typically want to use either
	 * this {@link #checkNotModified(long)} method; or
	 * {@link #checkNotModified(String)}, but not both.
	 * @param lastModifiedTimestamp the last-modified timestamp that
	 * the application determined for the underlying resource
	 * @return whether the request qualifies as not modified,
	 * allowing to abort request processing and relying on the response
	 * telling the client that the content has not been modified
	 */
	boolean checkNotModified(long lastModifiedTimestamp);

	/**
	 * Check whether the request qualifies as not modified given the
	 * supplied {@code ETag} (entity tag), as determined by the application.
	 * <p>This will also transparently set the appropriate response headers,
	 * for both the modified case and the not-modified case.
	 * <p>Typical usage:
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest webRequest, Model model) {
	 *   String eTag = // application-specific calculation
	 *   if (request.checkNotModified(eTag)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p><strong>Note:</strong> that you typically want to use either
	 * this {@link #checkNotModified(String)} method; or
	 * {@link #checkNotModified(long)}, but not both.
	 * @param eTag the entity tag that the application determined
	 * for the underlying resource. This parameter will be padded
	 * with quotes (") if necessary.
	 * @return whether the request qualifies as not modified,
	 * allowing to abort request processing and relying on the response
	 * telling the client that the content has not been modified
	 */
	boolean checkNotModified(String eTag);

	/**
	 * Get a short description of this request,
	 * typically containing request URI and session id.
	 * @param includeClientInfo whether to include client-specific
	 * information such as session id and user name
	 * @return the requested description as String
	 */
	String getDescription(boolean includeClientInfo);

}
