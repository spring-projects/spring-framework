/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.portlet.util;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.security.Principal;

import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.WindowState;

import org.springframework.util.Assert;

/**
 * Simple wrapper for a {@link javax.portlet.PortletRequest}, delegating all
 * calls to the underlying request.
 * 
 * <p>(In the style of the Servlet API's {@link javax.servlet.ServletRequestWrapper}.)
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ActionRequestWrapper
 * @see javax.servlet.ServletRequestWrapper
 */
public class PortletRequestWrapper implements PortletRequest {

	/** Original request that we're delegating to */
	private final PortletRequest portletRequest;


	/**
	 * Create a PortletRequestWrapper for the given {@link javax.portlet.PortletRequest}.
	 * @param request the original {@link javax.portlet.PortletRequest} to wrap
	 * @throws IllegalArgumentException if the supplied <code>request</code> is <code>null</code>
	 */
	public PortletRequestWrapper(PortletRequest request) {
		Assert.notNull(request, "Request is required");
		this.portletRequest = request;
	}


	public boolean isWindowStateAllowed(WindowState state) {
		return this.portletRequest.isWindowStateAllowed(state);
	}

	public boolean isPortletModeAllowed(PortletMode mode) {
		return this.portletRequest.isPortletModeAllowed(mode);
	}

	public PortletMode getPortletMode() {
		return this.portletRequest.getPortletMode();
	}

	public WindowState getWindowState() {
		return this.portletRequest.getWindowState();
	}

	public PortletPreferences getPreferences() {
		return this.portletRequest.getPreferences();
	}

	public PortletSession getPortletSession() {
		return this.portletRequest.getPortletSession();
	}

	public PortletSession getPortletSession(boolean create) {
		return this.portletRequest.getPortletSession(create);
	}

	public String getProperty(String name) {
		return this.portletRequest.getProperty(name);
	}

	public Enumeration getProperties(String name) {
		return this.portletRequest.getProperties(name);
	}

	public Enumeration getPropertyNames() {
		return this.portletRequest.getPropertyNames();
	}

	public PortalContext getPortalContext() {
		return this.portletRequest.getPortalContext();
	}

	public String getAuthType() {
		return this.portletRequest.getAuthType();
	}

	public String getContextPath() {
		return this.portletRequest.getContextPath();
	}

	public String getRemoteUser() {
		return this.portletRequest.getRemoteUser();
	}

	public Principal getUserPrincipal() {
		return this.portletRequest.getUserPrincipal();
	}

	public boolean isUserInRole(String role) {
		return this.portletRequest.isUserInRole(role);
	}

	public Object getAttribute(String name) {
		return this.portletRequest.getAttribute(name);
	}

	public Enumeration getAttributeNames() {
		return this.portletRequest.getAttributeNames();
	}

	public String getParameter(String name) {
		return this.portletRequest.getParameter(name);
	}

	public Enumeration getParameterNames() {
		return this.portletRequest.getParameterNames();
	}

	public String[] getParameterValues(String name) {
		return this.portletRequest.getParameterValues(name);
	}

	public Map getParameterMap() {
		return this.portletRequest.getParameterMap();
	}

	public boolean isSecure() {
		return this.portletRequest.isSecure();
	}

	public void setAttribute(String name, Object value) {
		this.portletRequest.setAttribute(name, value);
	}

	public void removeAttribute(String name) {
		this.portletRequest.removeAttribute(name);
	}

	public String getRequestedSessionId() {
		return this.portletRequest.getRequestedSessionId();
	}

	public boolean isRequestedSessionIdValid() {
		return this.portletRequest.isRequestedSessionIdValid();
	}

	public String getResponseContentType() {
		return this.portletRequest.getResponseContentType();
	}

	public Enumeration getResponseContentTypes() {
		return this.portletRequest.getResponseContentTypes();
	}

	public Locale getLocale() {
		return this.portletRequest.getLocale();
	}

	public Enumeration getLocales() {
		return this.portletRequest.getLocales();
	}

	public String getScheme() {
		return this.portletRequest.getScheme();
	}

	public String getServerName() {
		return this.portletRequest.getServerName();
	}

	public int getServerPort() {
		return this.portletRequest.getServerPort();
	}

}
