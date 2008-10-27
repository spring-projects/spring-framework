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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletSecurityException;
import javax.portlet.PortletURL;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Mock implementation of the {@link javax.portlet.PortletURL} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockPortletURL implements PortletURL {

	public static final String URL_TYPE_RENDER = "render";

	public static final String URL_TYPE_ACTION = "action";

	private static final String ENCODING = "UTF-8";


	private final PortalContext portalContext;

	private final String urlType;

	private WindowState windowState;

	private PortletMode portletMode;

	private final Map parameters = new LinkedHashMap(16);

	private boolean secure = false;


	/**
	 * Create a new MockPortletURL for the given URL type.
	 * @param portalContext the PortalContext defining the supported
	 * PortletModes and WindowStates
	 * @param urlType the URL type, for example "render" or "action"
	 * @see #URL_TYPE_RENDER
	 * @see #URL_TYPE_ACTION
	 */
	public MockPortletURL(PortalContext portalContext, String urlType) {
		Assert.notNull(portalContext, "PortalContext is required");
		this.portalContext = portalContext;
		this.urlType = urlType;
	}
    
    
	//---------------------------------------------------------------------
	// PortletURL methods
	//---------------------------------------------------------------------

	public void setWindowState(WindowState windowState) throws WindowStateException {
		if (!CollectionUtils.contains(this.portalContext.getSupportedWindowStates(), windowState)) {
			throw new WindowStateException("WindowState not supported", windowState);
		}
		this.windowState = windowState;
	}

	public void setPortletMode(PortletMode portletMode) throws PortletModeException {
		if (!CollectionUtils.contains(this.portalContext.getSupportedPortletModes(), portletMode)) {
			throw new PortletModeException("PortletMode not supported", portletMode);
		}
		this.portletMode = portletMode;
	}

	public void setParameter(String key, String value) {
		Assert.notNull(key, "Parameter key must be null");
		Assert.notNull(value, "Parameter value must not be null");
		this.parameters.put(key, new String[] {value});
	}

	public void setParameter(String key, String[] values) {
		Assert.notNull(key, "Parameter key must be null");
		Assert.notNull(values, "Parameter values must not be null");
		this.parameters.put(key, values);
	}

	public void setParameters(Map parameters) {
		Assert.notNull(parameters, "Parameters Map must not be null");
		this.parameters.clear();
		for (Iterator it = parameters.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			Assert.isTrue(entry.getKey() instanceof String, "Key must be of type String");
			Assert.isTrue(entry.getValue() instanceof String[], "Value must be of type String[]");
			this.parameters.put(entry.getKey(), entry.getValue());
		}
	}

	public Set getParameterNames() {
		return this.parameters.keySet();
	}

	public String getParameter(String name) {
		String[] arr = (String[]) this.parameters.get(name);
		return (arr != null && arr.length > 0 ? arr[0] : null);
	}

	public String[] getParameterValues(String name) {
		return (String[]) this.parameters.get(name);
	}

	public Map getParameterMap() {
		return Collections.unmodifiableMap(this.parameters);
	}

	public void setSecure(boolean secure) throws PortletSecurityException {
		this.secure = secure;
	}

	public boolean isSecure() {
		return secure;
	}

	public String toString() {
		StringBuffer query = new StringBuffer();
		query.append(encodeParameter("urlType", this.urlType));
		if (this.windowState != null) {
			query.append(";" + encodeParameter("windowState", this.windowState.toString()));
		}
		if (this.portletMode != null) {
			query.append(";" + encodeParameter("portletMode", this.portletMode.toString()));
		}
		for (Iterator it = this.parameters.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			String name = (String) entry.getKey();
			String[] values = (String[]) entry.getValue();
			query.append(";" + encodeParameter("param_" + name, values));
		}
		return (this.secure ? "https:" : "http:") +
				"//localhost/mockportlet?" + query.toString();
	}


	private String encodeParameter(String name, String value) {
		try {
			return URLEncoder.encode(name, ENCODING) + "=" +
					URLEncoder.encode(value, ENCODING);
		}
		catch (UnsupportedEncodingException ex) {
			return null;
		}
	}

	private String encodeParameter(String name, String[] values) {
		try {
			StringBuffer buf = new StringBuffer();
			for (int i = 0, n = values.length; i < n; i++) {
				buf.append((i > 0 ? ";" : "") +
						URLEncoder.encode(name, ENCODING) + "=" +
						URLEncoder.encode(values[i], ENCODING));
			}
			return buf.toString();
		}
		catch (UnsupportedEncodingException ex) {
			return null;
		}
	}

}
