/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.portlet.context;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * {@link org.springframework.web.context.request.WebRequest} adapter
 * for a {@link javax.portlet.PortletRequest}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class PortletWebRequest extends PortletRequestAttributes implements NativeWebRequest {

	private PortletResponse response;


	/**
	 * Create a new PortletWebRequest instance for the given request.
	 * @param request current portlet request
	 */
	public PortletWebRequest(PortletRequest request) {
		super(request);
	}

	/**
	 * Create a new PortletWebRequest instance for the given request/response pair.
	 * @param request current portlet request
	 * @param response current portlet response
	 */
	public PortletWebRequest(PortletRequest request, PortletResponse response) {
		this(request);
		this.response = response;
	}


	/**
	 * Exposes the native {@link PortletResponse} that we're wrapping (if any).
	 */
	public final PortletResponse getResponse() {
		return this.response;
	}

	@Override
	public Object getNativeRequest() {
		return getRequest();
	}

	@Override
	public Object getNativeResponse() {
		return getResponse();
	}

	@Override
	public <T> T getNativeRequest(Class<T> requiredType) {
		return PortletUtils.getNativeRequest(getRequest(), requiredType);
	}

	@Override
	public <T> T getNativeResponse(Class<T> requiredType) {
		return PortletUtils.getNativeResponse(getResponse(), requiredType);
	}


	@Override
	public String getHeader(String headerName) {
		return getRequest().getProperty(headerName);
	}

	@Override
	public String[] getHeaderValues(String headerName) {
		String[] headerValues = StringUtils.toStringArray(getRequest().getProperties(headerName));
		return (!ObjectUtils.isEmpty(headerValues) ? headerValues : null);
	}

	@Override
	public Iterator<String> getHeaderNames() {
		return CollectionUtils.toIterator(getRequest().getPropertyNames());
	}

	@Override
	public String getParameter(String paramName) {
		return getRequest().getParameter(paramName);
	}

	@Override
	public String[] getParameterValues(String paramName) {
		return getRequest().getParameterValues(paramName);
	}

	@Override
	public Iterator<String> getParameterNames() {
		return CollectionUtils.toIterator(getRequest().getParameterNames());
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return getRequest().getParameterMap();
	}

	@Override
	public Locale getLocale() {
		return getRequest().getLocale();
	}

	@Override
	public String getContextPath() {
		return getRequest().getContextPath();
	}

	@Override
	public String getRemoteUser() {
		return getRequest().getRemoteUser();
	}

	@Override
	public Principal getUserPrincipal() {
		return getRequest().getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String role) {
		return getRequest().isUserInRole(role);
	}

	@Override
	public boolean isSecure() {
		return getRequest().isSecure();
	}

	/**
	 * Last-modified handling not supported for portlet requests:
	 * As a consequence, this method always returns {@code false}.
	 */
	@Override
	public boolean checkNotModified(long lastModifiedTimestamp) {
		return false;
	}

	/**
	 * Last-modified handling not supported for portlet requests:
	 * As a consequence, this method always returns {@code false}.
	 */
	@Override
	public boolean checkNotModified(String eTag) {
		return false;
	}

	@Override
	public String getDescription(boolean includeClientInfo) {
		PortletRequest request = getRequest();
		StringBuilder result = new StringBuilder();
		result.append("context=").append(request.getContextPath());
		if (includeClientInfo) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				result.append(";session=").append(session.getId());
			}
			String user = getRequest().getRemoteUser();
			if (StringUtils.hasLength(user)) {
				result.append(";user=").append(user);
			}
		}
		return result.toString();
	}


	@Override
	public String toString() {
		return "PortletWebRequest: " + getDescription(true);
	}

}
