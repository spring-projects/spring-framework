/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.portlet.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.filter.PortletRequestWrapper;
import javax.portlet.filter.PortletResponseWrapper;
import javax.servlet.http.Cookie;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * Miscellaneous utilities for portlet applications.
 * Used by various framework classes.
 *
 * @author Juergen Hoeller
 * @author William G. Thompson, Jr.
 * @author John A. Lewis
 * @since 2.0
 */
public abstract class PortletUtils {

	/**
	 * Return the temporary directory for the current web application,
	 * as provided by the portlet container.
	 * @param portletContext the portlet context of the web application
	 * @return the File representing the temporary directory
	 */
	public static File getTempDir(PortletContext portletContext) {
		Assert.notNull(portletContext, "PortletContext must not be null");
		return (File) portletContext.getAttribute(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE);
	}

	/**
	 * Return the real path of the given path within the web application,
	 * as provided by the portlet container.
	 * <p>Prepends a slash if the path does not already start with a slash,
	 * and throws a {@link java.io.FileNotFoundException} if the path cannot
	 * be resolved to a resource (in contrast to
	 * {@link javax.portlet.PortletContext#getRealPath PortletContext's {@code getRealPath}},
	 * which simply returns {@code null}).
	 * @param portletContext the portlet context of the web application
	 * @param path the relative path within the web application
	 * @return the corresponding real path
	 * @throws FileNotFoundException if the path cannot be resolved to a resource
	 * @see javax.portlet.PortletContext#getRealPath
	 */
	public static String getRealPath(PortletContext portletContext, String path) throws FileNotFoundException {
		Assert.notNull(portletContext, "PortletContext must not be null");
		// Interpret location as relative to the web application root directory.
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String realPath = portletContext.getRealPath(path);
		if (realPath == null) {
			throw new FileNotFoundException(
					"PortletContext resource [" + path + "] cannot be resolved to absolute file path - " +
					"web application archive not expanded?");
		}
		return realPath;
	}


	/**
	 * Check the given request for a session attribute of the given name under the
	 * {@link javax.portlet.PortletSession#PORTLET_SCOPE}.
	 * Returns {@code null} if there is no session or if the session has no such attribute in that scope.
	 * Does not create a new session if none has existed before!
	 * @param request current portlet request
	 * @param name the name of the session attribute
	 * @return the value of the session attribute, or {@code null} if not found
	 */
	public static Object getSessionAttribute(PortletRequest request, String name) {
		return getSessionAttribute(request, name, PortletSession.PORTLET_SCOPE);
	}

	/**
	 * Check the given request for a session attribute of the given name in the given scope.
	 * Returns {@code null} if there is no session or if the session has no such attribute in that scope.
	 * Does not create a new session if none has existed before!
	 * @param request current portlet request
	 * @param name the name of the session attribute
	 * @param scope session scope of this attribute
	 * @return the value of the session attribute, or {@code null} if not found
	 */
	public static Object getSessionAttribute(PortletRequest request, String name, int scope) {
		Assert.notNull(request, "Request must not be null");
		PortletSession session = request.getPortletSession(false);
		return (session != null ? session.getAttribute(name, scope) : null);
	}

	/**
	 * Check the given request for a session attribute of the given name
	 * under the {@link javax.portlet.PortletSession#PORTLET_SCOPE}.
	 * Throws an exception if there is no session or if the session has
	 * no such attribute in that scope.
	 * <p>Does not create a new session if none has existed before!
	 * @param request current portlet request
	 * @param name the name of the session attribute
	 * @return the value of the session attribute
	 * @throws IllegalStateException if the session attribute could not be found
	 */
	public static Object getRequiredSessionAttribute(PortletRequest request, String name)
			throws IllegalStateException {

		return getRequiredSessionAttribute(request, name, PortletSession.PORTLET_SCOPE);
	}

	/**
	 * Check the given request for a session attribute of the given name in the given scope.
	 * Throws an exception if there is no session or if the session has no such attribute
	 * in that scope.
	 * <p>Does not create a new session if none has existed before!
	 * @param request current portlet request
	 * @param name the name of the session attribute
	 * @param scope session scope of this attribute
	 * @return the value of the session attribute
	 * @throws IllegalStateException if the session attribute could not be found
	 */
	public static Object getRequiredSessionAttribute(PortletRequest request, String name, int scope)
			throws IllegalStateException {
		Object attr = getSessionAttribute(request, name, scope);
		if (attr == null) {
			throw new IllegalStateException("No session attribute '" + name + "' found");
		}
		return attr;
	}

	/**
	 * Set the session attribute with the given name to the given value under the {@link javax.portlet.PortletSession#PORTLET_SCOPE}.
	 * Removes the session attribute if value is {@code null}, if a session existed at all.
	 * Does not create a new session if not necessary!
	 * @param request current portlet request
	 * @param name the name of the session attribute
	 * @param value the value of the session attribute
	 */
	public static void setSessionAttribute(PortletRequest request, String name, Object value) {
		setSessionAttribute(request, name, value, PortletSession.PORTLET_SCOPE);
	}

	/**
	 * Set the session attribute with the given name to the given value in the given scope.
	 * Removes the session attribute if value is {@code null}, if a session existed at all.
	 * Does not create a new session if not necessary!
	 * @param request current portlet request
	 * @param name the name of the session attribute
	 * @param value the value of the session attribute
	 * @param scope session scope of this attribute
	 */
	public static void setSessionAttribute(PortletRequest request, String name, Object value, int scope) {
		Assert.notNull(request, "Request must not be null");
		if (value != null) {
			request.getPortletSession().setAttribute(name, value, scope);
		}
		else {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				session.removeAttribute(name, scope);
			}
		}
	}

	/**
	 * Get the specified session attribute under the {@link javax.portlet.PortletSession#PORTLET_SCOPE},
	 * creating and setting a new attribute if no existing found. The given class
	 * needs to have a public no-arg constructor.
	 * Useful for on-demand state objects in a web tier, like shopping carts.
	 * @param session current portlet session
	 * @param name the name of the session attribute
	 * @param clazz the class to instantiate for a new attribute
	 * @return the value of the session attribute, newly created if not found
	 * @throws IllegalArgumentException if the session attribute could not be instantiated
	 */
	public static Object getOrCreateSessionAttribute(PortletSession session, String name, Class<?> clazz)
			throws IllegalArgumentException {

		return getOrCreateSessionAttribute(session, name, clazz, PortletSession.PORTLET_SCOPE);
	}

	/**
	 * Get the specified session attribute in the given scope,
	 * creating and setting a new attribute if no existing found. The given class
	 * needs to have a public no-arg constructor.
	 * Useful for on-demand state objects in a web tier, like shopping carts.
	 * @param session current portlet session
	 * @param name the name of the session attribute
	 * @param clazz the class to instantiate for a new attribute
	 * @param scope the session scope of this attribute
	 * @return the value of the session attribute, newly created if not found
	 * @throws IllegalArgumentException if the session attribute could not be instantiated
	 */
	public static Object getOrCreateSessionAttribute(PortletSession session, String name, Class<?> clazz, int scope)
			throws IllegalArgumentException {

		Assert.notNull(session, "Session must not be null");
		Object sessionObject = session.getAttribute(name, scope);
		if (sessionObject == null) {
			Assert.notNull(clazz, "Class must not be null if attribute value is to be instantiated");
			try {
				sessionObject = clazz.newInstance();
			}
			catch (InstantiationException ex) {
				throw new IllegalArgumentException(
						"Could not instantiate class [" + clazz.getName() +
						"] for session attribute '" + name + "': " + ex.getMessage());
			}
			catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(
						"Could not access default constructor of class [" + clazz.getName() +
						"] for session attribute '" + name + "': " + ex.getMessage());
			}
			session.setAttribute(name, sessionObject, scope);
		}
		return sessionObject;
	}

	/**
	 * Return the best available mutex for the given session:
	 * that is, an object to synchronize on for the given session.
	 * <p>Returns the session mutex attribute if available; usually,
	 * this means that the
	 * {@link org.springframework.web.util.HttpSessionMutexListener}
	 * needs to be defined in {@code web.xml}. Falls back to the
	 * {@link javax.portlet.PortletSession} itself if no mutex attribute found.
	 * <p>The session mutex is guaranteed to be the same object during
	 * the entire lifetime of the session, available under the key defined
	 * by the {@link org.springframework.web.util.WebUtils#SESSION_MUTEX_ATTRIBUTE}
	 * constant. It serves as a safe reference to synchronize on for locking
	 * on the current session.
	 * <p>In many cases, the {@link javax.portlet.PortletSession} reference
	 * itself is a safe mutex as well, since it will always be the same
	 * object reference for the same active logical session. However, this is
	 * not guaranteed across different servlet containers; the only 100% safe
	 * way is a session mutex.
	 * @param session the HttpSession to find a mutex for
	 * @return the mutex object (never {@code null})
	 * @see org.springframework.web.util.WebUtils#SESSION_MUTEX_ATTRIBUTE
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 */
	public static Object getSessionMutex(PortletSession session) {
		Assert.notNull(session, "Session must not be null");
		Object mutex = session.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, PortletSession.APPLICATION_SCOPE);
		if (mutex == null) {
			mutex = session;
		}
		return mutex;
	}


	/**
	 * Return an appropriate request object of the specified type, if available,
	 * unwrapping the given request as far as necessary.
	 * @param request the portlet request to introspect
	 * @param requiredType the desired type of request object
	 * @return the matching request object, or {@code null} if none
	 * of that type is available
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getNativeRequest(PortletRequest request, Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(request)) {
				return (T) request;
			}
			else if (request instanceof PortletRequestWrapper) {
				return getNativeRequest(((PortletRequestWrapper) request).getRequest(), requiredType);
			}
		}
		return null;
	}

	/**
	 * Return an appropriate response object of the specified type, if available,
	 * unwrapping the given response as far as necessary.
	 * @param response the portlet response to introspect
	 * @param requiredType the desired type of response object
	 * @return the matching response object, or {@code null} if none
	 * of that type is available
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getNativeResponse(PortletResponse response, Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(response)) {
				return (T) response;
			}
			else if (response instanceof PortletResponseWrapper) {
				return getNativeResponse(((PortletResponseWrapper) response).getResponse(), requiredType);
			}
		}
		return null;
	}

	/**
	 * Expose the given Map as request attributes, using the keys as attribute names
	 * and the values as corresponding attribute values. Keys must be Strings.
	 * @param request current portlet request
	 * @param attributes the attributes Map
	 */
	public static void exposeRequestAttributes(PortletRequest request, Map<String, ?> attributes) {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(attributes, "Attributes Map must not be null");
		for (Map.Entry<String, ?> entry : attributes.entrySet()) {
			request.setAttribute(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Retrieve the first cookie with the given name. Note that multiple
	 * cookies can have the same name but different paths or domains.
	 * @param request current portlet request
	 * @param name cookie name
	 * @return the first cookie with the given name, or {@code null} if none is found
	 */
	public static Cookie getCookie(PortletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		Cookie cookies[] = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		return null;
	}

	/**
	 * Check if a specific input type="submit" parameter was sent in the request,
	 * either via a button (directly with name) or via an image (name + ".x" or
	 * name + ".y").
	 * @param request current portlet request
	 * @param name name of the parameter
	 * @return if the parameter was sent
	 * @see org.springframework.web.util.WebUtils#SUBMIT_IMAGE_SUFFIXES
	 */
	public static boolean hasSubmitParameter(PortletRequest request, String name) {
		return getSubmitParameter(request, name) != null;
	}

	/**
	 * Return the full name of a specific input type="submit" parameter
	 * if it was sent in the request, either via a button (directly with name)
	 * or via an image (name + ".x" or name + ".y").
	 * @param request current portlet request
	 * @param name name of the parameter
	 * @return the actual parameter name with suffix if needed - null if not present
	 * @see org.springframework.web.util.WebUtils#SUBMIT_IMAGE_SUFFIXES
	 */
	public static String getSubmitParameter(PortletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		if (request.getParameter(name) != null) {
			return name;
		}
		for (int i = 0; i < WebUtils.SUBMIT_IMAGE_SUFFIXES.length; i++) {
			String suffix = WebUtils.SUBMIT_IMAGE_SUFFIXES[i];
			String parameter = name + suffix;
			if (request.getParameter(parameter) != null) {
				return parameter;
			}
		}
		return null;
	}

	/**
	 * Return a map containing all parameters with the given prefix.
	 * Maps single values to String and multiple values to String array.
	 * <p>For example, with a prefix of "spring_", "spring_param1" and
	 * "spring_param2" result in a Map with "param1" and "param2" as keys.
	 * <p>Similar to portlet
	 * {@link javax.portlet.PortletRequest#getParameterMap()},
	 * but more flexible.
	 * @param request portlet request in which to look for parameters
	 * @param prefix the beginning of parameter names
	 * (if this is {@code null} or the empty string, all parameters will match)
	 * @return map containing request parameters <b>without the prefix</b>,
	 * containing either a String or a String array as values
	 * @see javax.portlet.PortletRequest#getParameterNames
	 * @see javax.portlet.PortletRequest#getParameterValues
	 * @see javax.portlet.PortletRequest#getParameterMap
	 */
	public static Map<String, Object> getParametersStartingWith(PortletRequest request, String prefix) {
		Assert.notNull(request, "Request must not be null");
		Enumeration<String> paramNames = request.getParameterNames();
		Map<String, Object> params = new TreeMap<String, Object>();
		if (prefix == null) {
			prefix = "";
		}
		while (paramNames != null && paramNames.hasMoreElements()) {
			String paramName = paramNames.nextElement();
			if ("".equals(prefix) || paramName.startsWith(prefix)) {
				String unprefixed = paramName.substring(prefix.length());
				String[] values = request.getParameterValues(paramName);
				if (values == null || values.length == 0) {
					// Do nothing, no values found at all.
				}
				else if (values.length > 1) {
					params.put(unprefixed, values);
				}
				else {
					params.put(unprefixed, values[0]);
				}
			}
		}
		return params;
	}

	/**
	 * Return the target page specified in the request.
	 * @param request current portlet request
	 * @param paramPrefix the parameter prefix to check for
	 * (e.g. "_target" for parameters like "_target1" or "_target2")
	 * @param currentPage the current page, to be returned as fallback
	 * if no target page specified
	 * @return the page specified in the request, or current page if not found
	 */
	public static int getTargetPage(PortletRequest request, String paramPrefix, int currentPage) {
		Enumeration<String> paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = paramNames.nextElement();
			if (paramName.startsWith(paramPrefix)) {
				for (int i = 0; i < WebUtils.SUBMIT_IMAGE_SUFFIXES.length; i++) {
					String suffix = WebUtils.SUBMIT_IMAGE_SUFFIXES[i];
					if (paramName.endsWith(suffix)) {
						paramName = paramName.substring(0, paramName.length() - suffix.length());
					}
				}
				return Integer.parseInt(paramName.substring(paramPrefix.length()));
			}
		}
		return currentPage;
	}


	/**
	 * Pass all the action request parameters to the render phase by putting them into
	 * the action response object. This may not be called when the action will call
	 * {@link javax.portlet.ActionResponse#sendRedirect sendRedirect}.
	 * @param request the current action request
	 * @param response the current action response
	 * @see javax.portlet.ActionResponse#setRenderParameter
	 */
	public static void passAllParametersToRenderPhase(ActionRequest request, ActionResponse response) {
		try {
			Enumeration<String> en = request.getParameterNames();
			while (en.hasMoreElements()) {
				String param = en.nextElement();
				String values[] = request.getParameterValues(param);
				response.setRenderParameter(param, values);
			}
		}
		catch (IllegalStateException ex) {
			// Ignore in case sendRedirect was already set.
		}
	}

	/**
	 * Clear all the render parameters from the {@link javax.portlet.ActionResponse}.
	 * This may not be called when the action will call
	 * {@link ActionResponse#sendRedirect sendRedirect}.
	 * @param response the current action response
	 * @see ActionResponse#setRenderParameters
	 */
	public static void clearAllRenderParameters(ActionResponse response) {
		try {
			response.setRenderParameters(new HashMap<String, String[]>(0));
		}
		catch (IllegalStateException ex) {
			// Ignore in case sendRedirect was already set.
		}
	}

	/**
	 * Serve the resource as specified in the given request to the given response,
	 * using the PortletContext's request dispatcher.
	 * <p>This is roughly equivalent to Portlet 2.0 GenericPortlet.
	 * @param request the current resource request
	 * @param response the current resource response
	 * @param context the current Portlet's PortletContext
	 * @throws PortletException propagated from Portlet API's forward method
	 * @throws IOException propagated from Portlet API's forward method
	 */
	public static void serveResource(ResourceRequest request, ResourceResponse response, PortletContext context)
			throws PortletException, IOException {

		String id = request.getResourceID();
		if (id != null) {
			if (!PortletUtils.isProtectedResource(id)) {
				PortletRequestDispatcher rd = context.getRequestDispatcher(id);
				if (rd != null) {
					rd.forward(request, response);
					return;
				}
			}
			response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "404");
		}
	}

	/**
	 * Check whether the specified path indicates a resource in the protected
	 * WEB-INF or META-INF directories.
	 * @param path the path to check
	 */
	private static boolean isProtectedResource(String path) {
		return (StringUtils.startsWithIgnoreCase(path, "/WEB-INF") ||
				StringUtils.startsWithIgnoreCase(path, "/META-INF"));
	}

}
