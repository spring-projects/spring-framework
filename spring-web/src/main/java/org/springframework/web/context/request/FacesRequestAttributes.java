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

package org.springframework.web.context.request;

import java.lang.reflect.Method;
import java.util.Map;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link RequestAttributes} adapter for a JSF {@link jakarta.faces.context.FacesContext}.
 * Used as default in a JSF environment, wrapping the current FacesContext.
 *
 * <p><b>NOTE:</b> In contrast to {@link ServletRequestAttributes}, this variant does
 * <i>not</i> support destruction callbacks for scoped attributes, neither for the
 * request scope nor for the session scope. If you rely on such implicit destruction
 * callbacks, consider defining a Spring {@link RequestContextListener} in your
 * {@code web.xml}.
 *
 * <p>Requires JSF 2.0 or higher, as of Spring 4.0.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see jakarta.faces.context.FacesContext#getExternalContext()
 * @see jakarta.faces.context.ExternalContext#getRequestMap()
 * @see jakarta.faces.context.ExternalContext#getSessionMap()
 * @see RequestContextHolder#currentRequestAttributes()
 */
public class FacesRequestAttributes implements RequestAttributes {

	/**
	 * We'll create a lot of these objects, so we don't want a new logger every time.
	 */
	private static final Log logger = LogFactory.getLog(FacesRequestAttributes.class);

	private final FacesContext facesContext;


	/**
	 * Create a new FacesRequestAttributes adapter for the given FacesContext.
	 * @param facesContext the current FacesContext
	 * @see jakarta.faces.context.FacesContext#getCurrentInstance()
	 */
	public FacesRequestAttributes(FacesContext facesContext) {
		Assert.notNull(facesContext, "FacesContext must not be null");
		this.facesContext = facesContext;
	}


	/**
	 * Return the JSF FacesContext that this adapter operates on.
	 */
	protected final FacesContext getFacesContext() {
		return this.facesContext;
	}

	/**
	 * Return the JSF ExternalContext that this adapter operates on.
	 * @see jakarta.faces.context.FacesContext#getExternalContext()
	 */
	protected final ExternalContext getExternalContext() {
		return getFacesContext().getExternalContext();
	}

	/**
	 * Return the JSF attribute Map for the specified scope.
	 * @param scope constant indicating request or session scope
	 * @return the Map representation of the attributes in the specified scope
	 * @see #SCOPE_REQUEST
	 * @see #SCOPE_SESSION
	 */
	protected Map<String, Object> getAttributeMap(int scope) {
		if (scope == SCOPE_REQUEST) {
			return getExternalContext().getRequestMap();
		}
		else {
			return getExternalContext().getSessionMap();
		}
	}


	@Override
	@Nullable
	public Object getAttribute(String name, int scope) {
		return getAttributeMap(scope).get(name);
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		getAttributeMap(scope).put(name, value);
	}

	@Override
	public void removeAttribute(String name, int scope) {
		getAttributeMap(scope).remove(name);
	}

	@Override
	public String[] getAttributeNames(int scope) {
		return StringUtils.toStringArray(getAttributeMap(scope).keySet());
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback, int scope) {
		if (logger.isWarnEnabled()) {
			logger.warn("Could not register destruction callback [" + callback + "] for attribute '" + name +
					"' because FacesRequestAttributes does not support such callbacks");
		}
	}

	@Override
	@Nullable
	public Object resolveReference(String key) {
		return switch (key) {
			case REFERENCE_REQUEST -> getExternalContext().getRequest();
			case REFERENCE_SESSION -> getExternalContext().getSession(true);
			case "application" -> getExternalContext().getContext();
			case "requestScope" -> getExternalContext().getRequestMap();
			case "sessionScope" -> getExternalContext().getSessionMap();
			case "applicationScope" -> getExternalContext().getApplicationMap();
			case "facesContext" -> getFacesContext();
			case "cookie" -> getExternalContext().getRequestCookieMap();
			case "header" -> getExternalContext().getRequestHeaderMap();
			case "headerValues" -> getExternalContext().getRequestHeaderValuesMap();
			case "param" -> getExternalContext().getRequestParameterMap();
			case "paramValues" -> getExternalContext().getRequestParameterValuesMap();
			case "initParam" -> getExternalContext().getInitParameterMap();
			case "view" -> getFacesContext().getViewRoot();
			case "viewScope" -> getFacesContext().getViewRoot().getViewMap();
			case "flash" -> getExternalContext().getFlash();
			case "resource" -> getFacesContext().getApplication().getResourceHandler();
			default -> null;
		};
	}

	@Override
	public String getSessionId() {
		Object session = getExternalContext().getSession(true);
		try {
			// HttpSession has a getId() method.
			Method getIdMethod = session.getClass().getMethod("getId");
			return String.valueOf(ReflectionUtils.invokeMethod(getIdMethod, session));
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Session object [" + session + "] does not have a getId() method");
		}
	}

	@Override
	public Object getSessionMutex() {
		// Enforce presence of a session first to allow listeners to create the mutex attribute
		ExternalContext externalContext = getExternalContext();
		Object session = externalContext.getSession(true);
		Object mutex = externalContext.getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = (session != null ? session : externalContext);
		}
		return mutex;
	}

}
