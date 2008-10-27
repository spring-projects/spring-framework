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

package org.springframework.mock.web.portlet;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.portlet.PortletContext;
import javax.portlet.PortletSession;

/**
 * Mock implementation of the {@link javax.portlet.PortletSession} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockPortletSession implements PortletSession {

	private static int nextId = 1;


	private final String id = Integer.toString(nextId++);

	private final long creationTime = System.currentTimeMillis();

	private int maxInactiveInterval;

	private long lastAccessedTime = System.currentTimeMillis();

	private final PortletContext portletContext;

	private final Hashtable portletAttributes = new Hashtable();

	private final Hashtable applicationAttributes = new Hashtable();

	private boolean invalid = false;

	private boolean isNew = true;


	/**
	 * Create a new MockPortletSession with a default {@link MockPortletContext}.
	 * @see MockPortletContext
	 */
	public MockPortletSession() {
		this(null);
	}

	/**
	 * Create a new MockPortletSession.
	 * @param portletContext the PortletContext that the session runs in
	 */
	public MockPortletSession(PortletContext portletContext) {
		this.portletContext = (portletContext != null ? portletContext : new MockPortletContext());
	}

	
	public Object getAttribute(String name) {
		return this.portletAttributes.get(name);
	}

	public Object getAttribute(String name, int scope) {
		if (scope == PortletSession.PORTLET_SCOPE) {
			return this.portletAttributes.get(name);
		}
		else if (scope == PortletSession.APPLICATION_SCOPE) {
			return this.applicationAttributes.get(name);
		}
		return null;
	}

	public Enumeration getAttributeNames() {
		return this.portletAttributes.keys();
	}

	public Enumeration getAttributeNames(int scope) {
		if (scope == PortletSession.PORTLET_SCOPE) {
			return this.portletAttributes.keys();
		}
		else if (scope == PortletSession.APPLICATION_SCOPE) {
			return this.applicationAttributes.keys();
		}
		return null;
	}

	public long getCreationTime() {
		return this.creationTime;
	}

	public String getId() {
		return this.id;
	}

	public void access() {
		this.lastAccessedTime = System.currentTimeMillis();
		setNew(false);
	}

	public long getLastAccessedTime() {
		return this.lastAccessedTime;
	}

	public int getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	public void invalidate() {
		this.invalid = true;
		this.portletAttributes.clear();
		this.applicationAttributes.clear();
	}

	public boolean isInvalid() {
		return invalid;
	}

	public void setNew(boolean value) {
		this.isNew = value;
	}

	public boolean isNew() {
		return this.isNew;
	}

	public void removeAttribute(String name) {
		this.portletAttributes.remove(name);
	}

	public void removeAttribute(String name, int scope) {
		if (scope == PortletSession.PORTLET_SCOPE) {
			this.portletAttributes.remove(name);
		}
		else if (scope == PortletSession.APPLICATION_SCOPE) {
			this.applicationAttributes.remove(name);
		}
	}

	public void setAttribute(String name, Object value) {
		if (value != null) {
			this.portletAttributes.put(name, value);
		}
		else {
			this.portletAttributes.remove(name);
		}
	}

	public void setAttribute(String name, Object value, int scope) {
		if (scope == PortletSession.PORTLET_SCOPE) {
			if (value != null) {
				this.portletAttributes.put(name, value);
			}
			else {
				this.portletAttributes.remove(name);
			}
		}
		else if (scope == PortletSession.APPLICATION_SCOPE) {
			if (value != null) {
				this.applicationAttributes.put(name, value);
			}
			else {
				this.applicationAttributes.remove(name);
			}
		}
	}

	public void setMaxInactiveInterval(int interval) {
		this.maxInactiveInterval = interval;
	}

	public PortletContext getPortletContext() {
		return portletContext;
	}

}
