/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.mock.web.portlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;

import javax.portlet.ActionResponse;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;

import org.springframework.core.CollectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Mock implementation of the {@link javax.portlet.ActionResponse} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockActionResponse extends MockPortletResponse implements ActionResponse {

	private WindowState windowState;

	private PortletMode portletMode;

	private String redirectedUrl;

	private final Map renderParameters = new LinkedHashMap(16);


	/**
	 * Create a new MockActionResponse with a default {@link MockPortalContext}.
	 * @see MockPortalContext
	 */
	public MockActionResponse() {
		super();
	}

	/**
	 * Create a new MockActionResponse.
	 * @param portalContext the PortalContext defining the supported
	 * PortletModes and WindowStates
	 */
	public MockActionResponse(PortalContext portalContext) {
		super(portalContext);
	}


	public void setWindowState(WindowState windowState) throws WindowStateException {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set WindowState after sendRedirect has been called");
		}
		if (!CollectionUtils.contains(getPortalContext().getSupportedWindowStates(), windowState)) {
			throw new WindowStateException("WindowState not supported", windowState);
		}
		this.windowState = windowState;
	}

	public WindowState getWindowState() {
		return windowState;
	}

	public void setPortletMode(PortletMode portletMode) throws PortletModeException {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set PortletMode after sendRedirect has been called");
		}
		if (!CollectionUtils.contains(getPortalContext().getSupportedPortletModes(), portletMode)) {
			throw new PortletModeException("PortletMode not supported", portletMode);
		}
		this.portletMode = portletMode;
	}

	public PortletMode getPortletMode() {
		return portletMode;
	}

	public void sendRedirect(String url) throws IOException {
		if (this.windowState != null || this.portletMode != null || !this.renderParameters.isEmpty()) {
			throw new IllegalStateException(
					"Cannot call sendRedirect after windowState, portletMode, or renderParameters have been set");
		}
		Assert.notNull(url, "Redirect URL must not be null");
		this.redirectedUrl = url;
	}

	public String getRedirectedUrl() {
		return redirectedUrl;
	}

	public void setRenderParameters(Map parameters) {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set render parameters after sendRedirect has been called");
		}
		Assert.notNull(parameters, "Parameters Map must not be null");
		this.renderParameters.clear();
		for (Iterator it = parameters.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			Assert.isTrue(entry.getKey() instanceof String, "Key must be of type String");
			Assert.isTrue(entry.getValue() instanceof String[], "Value must be of type String[]");
			this.renderParameters.put(entry.getKey(), entry.getValue());
		}
	}

	public void setRenderParameter(String key, String value) {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set render parameters after sendRedirect has been called");
		}
		Assert.notNull(key, "Parameter key must not be null");
		Assert.notNull(value, "Parameter value must not be null");
		this.renderParameters.put(key, new String[] {value});
	}

	public String getRenderParameter(String name) {
		String[] arr = (String[]) this.renderParameters.get(name);
		return (arr != null && arr.length > 0 ? arr[0] : null);
	}

	public void setRenderParameter(String key, String[] values) {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set render parameters after sendRedirect has been called");
		}
		Assert.notNull(key, "Parameter key must not be null");
		Assert.notNull(values, "Parameter values must not be null");
		this.renderParameters.put(key, values);
	}

	public String[] getRenderParameterValues(String key) {
		Assert.notNull(key, "Parameter key must not be null");
		return (String[]) this.renderParameters.get(key);
	}

	public Iterator getRenderParameterNames() {
		return this.renderParameters.keySet().iterator();
	}

	public Map getRenderParameterMap() {
		return Collections.unmodifiableMap(this.renderParameters);
	}


}
