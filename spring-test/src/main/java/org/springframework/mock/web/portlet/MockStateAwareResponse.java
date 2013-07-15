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

package org.springframework.mock.web.portlet;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.StateAwareResponse;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import javax.xml.namespace.QName;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Mock implementation of the {@link javax.portlet.StateAwareResponse} interface.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MockStateAwareResponse extends MockPortletResponse implements StateAwareResponse {

	private WindowState windowState;

	private PortletMode portletMode;

	private final Map<String, String[]> renderParameters = new LinkedHashMap<String, String[]>();

	private final Map<QName, Serializable> events = new HashMap<QName, Serializable>();


	/**
	 * Create a new MockActionResponse with a default {@link MockPortalContext}.
	 * @see org.springframework.mock.web.portlet.MockPortalContext
	 */
	public MockStateAwareResponse() {
		super();
	}

	/**
	 * Create a new MockActionResponse.
	 * @param portalContext the PortalContext defining the supported
	 * PortletModes and WindowStates
	 */
	public MockStateAwareResponse(PortalContext portalContext) {
		super(portalContext);
	}


	@Override
	public void setWindowState(WindowState windowState) throws WindowStateException {
		if (!CollectionUtils.contains(getPortalContext().getSupportedWindowStates(), windowState)) {
			throw new WindowStateException("WindowState not supported", windowState);
		}
		this.windowState = windowState;
	}

	@Override
	public WindowState getWindowState() {
		return this.windowState;
	}

	@Override
	public void setPortletMode(PortletMode portletMode) throws PortletModeException {
		if (!CollectionUtils.contains(getPortalContext().getSupportedPortletModes(), portletMode)) {
			throw new PortletModeException("PortletMode not supported", portletMode);
		}
		this.portletMode = portletMode;
	}

	@Override
	public PortletMode getPortletMode() {
		return this.portletMode;
	}

	@Override
	public void setRenderParameters(Map<String, String[]> parameters) {
		Assert.notNull(parameters, "Parameters Map must not be null");
		this.renderParameters.clear();
		this.renderParameters.putAll(parameters);
	}

	@Override
	public void setRenderParameter(String key, String value) {
		Assert.notNull(key, "Parameter key must not be null");
		Assert.notNull(value, "Parameter value must not be null");
		this.renderParameters.put(key, new String[] {value});
	}

	@Override
	public void setRenderParameter(String key, String[] values) {
		Assert.notNull(key, "Parameter key must not be null");
		Assert.notNull(values, "Parameter values must not be null");
		this.renderParameters.put(key, values);
	}

	public String getRenderParameter(String key) {
		Assert.notNull(key, "Parameter key must not be null");
		String[] arr = this.renderParameters.get(key);
		return (arr != null && arr.length > 0 ? arr[0] : null);
	}

	public String[] getRenderParameterValues(String key) {
		Assert.notNull(key, "Parameter key must not be null");
		return this.renderParameters.get(key);
	}

	public Iterator<String> getRenderParameterNames() {
		return this.renderParameters.keySet().iterator();
	}

	@Override
	public Map<String, String[]> getRenderParameterMap() {
		return Collections.unmodifiableMap(this.renderParameters);
	}

	@Override
	public void removePublicRenderParameter(String name) {
		this.renderParameters.remove(name);
	}

	@Override
	public void setEvent(QName name, Serializable value) {
		this.events.put(name, value);
	}

	@Override
	public void setEvent(String name, Serializable value) {
		this.events.put(new QName(name), value);
	}

	public Iterator<QName> getEventNames() {
		return this.events.keySet().iterator();
	}

	public Serializable getEvent(QName name) {
		return this.events.get(name);
	}

	public Serializable getEvent(String name) {
		return this.events.get(new QName(name));
	}

}
