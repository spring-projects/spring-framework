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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.portlet.PortalContext;
import javax.portlet.PortletResponse;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.portlet.PortletResponse} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockPortletResponse implements PortletResponse {

	private final PortalContext portalContext;

	private final Map properties = new LinkedHashMap(16);


	/**
	 * Create a new MockPortletResponse with a default {@link MockPortalContext}.
	 * @see MockPortalContext
	 */
	public MockPortletResponse() {
		this(null);
	}

	/**
	 * Create a new MockPortletResponse.
	 * @param portalContext the PortalContext defining the supported
	 * PortletModes and WindowStates
	 */
	public MockPortletResponse(PortalContext portalContext) {
		this.portalContext = (portalContext != null ? portalContext : new MockPortalContext());
	}

	/**
	 * Return the PortalContext that this MockPortletResponse runs in,
	 * defining the supported PortletModes and WindowStates.
	 */
	public PortalContext getPortalContext() {
		return portalContext;
	}


	//---------------------------------------------------------------------
	// PortletResponse methods
	//---------------------------------------------------------------------

	public void addProperty(String key, String value) {
		Assert.notNull(key, "Property key must not be null");
		String[] oldArr = (String[]) this.properties.get(key);
		if (oldArr != null) {
			String[] newArr = new String[oldArr.length + 1];
			System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
			newArr[oldArr.length] = value;
			this.properties.put(key, newArr);
		}
		else {
			this.properties.put(key, new String[] {value});
		}
	}

	public void setProperty(String key, String value) {
		Assert.notNull(key, "Property key must not be null");
		this.properties.put(key, new String[] {value});
	}

	public Set getPropertyNames() {
		return this.properties.keySet();
	}

	public String getProperty(String key) {
		Assert.notNull(key, "Property key must not be null");
		String[] arr = (String[]) this.properties.get(key);
		return (arr != null && arr.length > 0 ? arr[0] : null);
	}

	public String[] getProperties(String key) {
		Assert.notNull(key, "Property key must not be null");
		return (String[]) this.properties.get(key);
	}

	public String encodeURL(String path) {
		return path;
	}

}
