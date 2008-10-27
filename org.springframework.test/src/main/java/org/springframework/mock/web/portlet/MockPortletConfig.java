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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.portlet.PortletConfig} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockPortletConfig implements PortletConfig {

	private final PortletContext portletContext;

	private final String portletName;

	private final HashMap resourceBundles = new HashMap();

	private final Properties initParameters = new Properties();


	/**
	 * Create a new MockPortletConfig with a default {@link MockPortletContext}.
	 */
	public MockPortletConfig() {
		this(null, "");
	}

	/**
	 * Create a new MockPortletConfig with a default {@link MockPortletContext}.
	 * @param portletName the name of the portlet
	 */
	public MockPortletConfig(String portletName) {
		this(null, portletName);
	}

	/**
	 * Create a new MockPortletConfig.
	 * @param portletContext the PortletContext that the portlet runs in
	 */
	public MockPortletConfig(PortletContext portletContext) {
		this(portletContext, "");
	}

	/**
	 * Create a new MockPortletConfig.
	 * @param portletContext the PortletContext that the portlet runs in
	 * @param portletName the name of the portlet
	 */
	public MockPortletConfig(PortletContext portletContext, String portletName) {
		this.portletContext = (portletContext != null ? portletContext : new MockPortletContext());
		this.portletName = portletName;
	}

	
	public String getPortletName() {
		return this.portletName;
	}
	
	public PortletContext getPortletContext() {
		return this.portletContext;
	}
	
	public void setResourceBundle(Locale locale, ResourceBundle resourceBundle) {
		Assert.notNull(locale, "Locale must not be null");
		this.resourceBundles.put(locale, resourceBundle);
	}

	public ResourceBundle getResourceBundle(Locale locale) {
		Assert.notNull(locale, "Locale must not be null");
		return (ResourceBundle) this.resourceBundles.get(locale);
	}

	public void addInitParameter(String name, String value) {
		Assert.notNull(name, "Parameter name must not be null");
		this.initParameters.setProperty(name, value);
	}

	public String getInitParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		return this.initParameters.getProperty(name);
	}

	public Enumeration getInitParameterNames() {
		return this.initParameters.keys();
	}

}
