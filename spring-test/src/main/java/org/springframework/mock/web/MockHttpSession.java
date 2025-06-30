/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.mock.web;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link jakarta.servlet.http.HttpSession} interface.
 *
 * <p>As of Spring 6.0, this set of mocks is designed on a Servlet 6.0 baseline.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Fisher
 * @author Sam Brannen
 * @author Vedran Pavic
 * @since 1.0.2
 */
public class MockHttpSession implements HttpSession {

	/**
	 * The session cookie name.
	 */
	public static final String SESSION_COOKIE_NAME = "JSESSION";


	private static int nextId = 1;

	private String id;

	private final long creationTime = System.currentTimeMillis();

	private int maxInactiveInterval;

	private long lastAccessedTime = System.currentTimeMillis();

	private final ServletContext servletContext;

	private final Map<String, Object> attributes = new LinkedHashMap<>();

	private boolean invalid = false;

	private boolean isNew = true;


	/**
	 * Create a new MockHttpSession with a default {@link MockServletContext}.
	 * @see MockServletContext
	 */
	public MockHttpSession() {
		this(null);
	}

	/**
	 * Create a new MockHttpSession.
	 * @param servletContext the ServletContext that the session runs in
	 */
	public MockHttpSession(@Nullable ServletContext servletContext) {
		this(servletContext, null);
	}

	/**
	 * Create a new MockHttpSession.
	 * @param servletContext the ServletContext that the session runs in
	 * @param id a unique identifier for this session
	 */
	public MockHttpSession(@Nullable ServletContext servletContext, @Nullable String id) {
		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.id = (id != null ? id : Integer.toString(nextId++));
	}


	@Override
	public long getCreationTime() {
		assertIsValid();
		return this.creationTime;
	}

	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * As of Servlet 3.1, the id of a session can be changed.
	 * @return the new session id
	 * @since 4.0.3
	 */
	public String changeSessionId() {
		this.id = Integer.toString(nextId++);
		return this.id;
	}

	public void access() {
		this.lastAccessedTime = System.currentTimeMillis();
		this.isNew = false;
	}

	@Override
	public long getLastAccessedTime() {
		assertIsValid();
		return this.lastAccessedTime;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		this.maxInactiveInterval = interval;
	}

	@Override
	public int getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	@Override
	public @Nullable Object getAttribute(String name) {
		assertIsValid();
		Assert.notNull(name, "Attribute name must not be null");
		return this.attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		assertIsValid();
		return Collections.enumeration(new LinkedHashSet<>(this.attributes.keySet()));
	}

	@Override
	public void setAttribute(String name, @Nullable Object value) {
		assertIsValid();
		Assert.notNull(name, "Attribute name must not be null");
		if (value != null) {
			Object oldValue = this.attributes.put(name, value);
			if (value != oldValue) {
				if (oldValue instanceof HttpSessionBindingListener listener) {
					listener.valueUnbound(new HttpSessionBindingEvent(this, name, oldValue));
				}
				if (value instanceof HttpSessionBindingListener listener) {
					listener.valueBound(new HttpSessionBindingEvent(this, name, value));
				}
			}
		}
		else {
			removeAttribute(name);
		}
	}

	@Override
	public void removeAttribute(String name) {
		assertIsValid();
		Assert.notNull(name, "Attribute name must not be null");
		Object value = this.attributes.remove(name);
		if (value instanceof HttpSessionBindingListener listener) {
			listener.valueUnbound(new HttpSessionBindingEvent(this, name, value));
		}
	}

	/**
	 * Clear all of this session's attributes.
	 */
	public void clearAttributes() {
		for (Iterator<Map.Entry<String, Object>> it = this.attributes.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			it.remove();
			if (value instanceof HttpSessionBindingListener listener) {
				listener.valueUnbound(new HttpSessionBindingEvent(this, name, value));
			}
		}
	}

	/**
	 * Invalidates this session then unbinds any objects bound to it.
	 * @throws IllegalStateException if this method is called on an already invalidated session
	 */
	@Override
	public void invalidate() {
		assertIsValid();
		this.invalid = true;
		clearAttributes();
	}

	public boolean isInvalid() {
		return this.invalid;
	}

	/**
	 * Convenience method for asserting that this session has not been
	 * {@linkplain #invalidate() invalidated}.
	 * @throws IllegalStateException if this session has been invalidated
	 */
	private void assertIsValid() {
		Assert.state(!isInvalid(), "The session has already been invalidated");
	}

	public void setNew(boolean value) {
		this.isNew = value;
	}

	@Override
	public boolean isNew() {
		assertIsValid();
		return this.isNew;
	}

	@Override
	public Accessor getAccessor() {
		return sessionConsumer -> sessionConsumer.accept(MockHttpSession.this);
	}


	/**
	 * Serialize the attributes of this session into an object that can be
	 * turned into a byte array with standard Java serialization.
	 * @return a representation of this session's serialized state
	 */
	public Serializable serializeState() {
		HashMap<String, Serializable> state = new HashMap<>();
		for (Iterator<Map.Entry<String, Object>> it = this.attributes.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			it.remove();
			if (value instanceof Serializable serializable) {
				state.put(name, serializable);
			}
			else {
				// Not serializable... Servlet containers usually automatically
				// unbind the attribute in this case.
				if (value instanceof HttpSessionBindingListener listener) {
					listener.valueUnbound(new HttpSessionBindingEvent(this, name, value));
				}
			}
		}
		return state;
	}

	/**
	 * Deserialize the attributes of this session from a state object created by
	 * {@link #serializeState()}.
	 * @param state a representation of this session's serialized state
	 */
	@SuppressWarnings("unchecked")
	public void deserializeState(Serializable state) {
		Assert.isTrue(state instanceof Map, "Serialized state needs to be of type [java.util.Map]");
		this.attributes.putAll((Map<String, Object>) state);
	}

}
