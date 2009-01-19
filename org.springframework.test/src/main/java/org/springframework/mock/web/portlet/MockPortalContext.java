/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.WindowState;

/**
 * Mock implementation of the {@link javax.portlet.PortalContext} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockPortalContext implements PortalContext {

	private final Map<String, String> properties = new HashMap<String, String>();

	private final List<PortletMode> portletModes;

	private final List<WindowState> windowStates;


	/**
	 * Create a new MockPortalContext
	 * with default PortletModes (VIEW, EDIT, HELP)
	 * and default WindowStates (NORMAL, MAXIMIZED, MINIMIZED).
	 * @see javax.portlet.PortletMode
	 * @see javax.portlet.WindowState
	 */
	public MockPortalContext() {
		this.portletModes = new ArrayList<PortletMode>(3);
		this.portletModes.add(PortletMode.VIEW);
		this.portletModes.add(PortletMode.EDIT);
		this.portletModes.add(PortletMode.HELP);

		this.windowStates = new ArrayList<WindowState>(3);
		this.windowStates.add(WindowState.NORMAL);
		this.windowStates.add(WindowState.MAXIMIZED);
		this.windowStates.add(WindowState.MINIMIZED);
	}

	/**
	 * Create a new MockPortalContext with the given PortletModes and WindowStates.
	 * @param supportedPortletModes the List of supported PortletMode instances
	 * @param supportedWindowStates the List of supported WindowState instances
	 * @see javax.portlet.PortletMode
	 * @see javax.portlet.WindowState
	 */
	public MockPortalContext(List<PortletMode> supportedPortletModes, List<WindowState> supportedWindowStates) {
		this.portletModes = new ArrayList<PortletMode>(supportedPortletModes);
		this.windowStates = new ArrayList<WindowState>(supportedWindowStates);
	}


	public String getPortalInfo() {
		return "MockPortal/1.0";
	}

	public void setProperty(String name, String value) {
		this.properties.put(name, value);
	}

	public String getProperty(String name) {
		return this.properties.get(name);
	}

	public Enumeration<String> getPropertyNames() {
		return Collections.enumeration(this.properties.keySet());
	}

	public Enumeration<PortletMode> getSupportedPortletModes() {
		return Collections.enumeration(this.portletModes);
	}

	public Enumeration<WindowState> getSupportedWindowStates() {
		return Collections.enumeration(this.windowStates);
	}

}
