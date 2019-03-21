/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.portlet.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.AbstractRequestAttributes;
import org.springframework.web.context.request.DestructionCallbackBindingListener;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * Portlet-based implementation of the
 * {@link org.springframework.web.context.request.RequestAttributes} interface.
 *
 * <p>Accesses objects from portlet request and portlet session scope,
 * with a distinction between "session" (the PortletSession's "portlet scope")
 * and "global session" (the PortletSession's "application scope").
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.portlet.PortletRequest#getAttribute
 * @see javax.portlet.PortletSession#getAttribute
 * @see javax.portlet.PortletSession#PORTLET_SCOPE
 * @see javax.portlet.PortletSession#APPLICATION_SCOPE
 * @see RequestAttributes#SCOPE_SESSION
 * @see RequestAttributes#SCOPE_GLOBAL_SESSION
 */
public class PortletRequestAttributes extends AbstractRequestAttributes {

	/**
	 * Constant identifying the {@link String} prefixed to the name of a
	 * destruction callback when it is stored in a {@link PortletSession}.
	 */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX =
			PortletRequestAttributes.class.getName() + ".DESTRUCTION_CALLBACK.";


	private final PortletRequest request;

	private PortletResponse response;

	private volatile PortletSession session;

	private final Map<String, Object> sessionAttributesToUpdate = new ConcurrentHashMap<String, Object>(1);

	private final Map<String, Object> globalSessionAttributesToUpdate = new ConcurrentHashMap<String, Object>(1);


	/**
	 * Create a new PortletRequestAttributes instance for the given request.
	 * @param request current portlet request
	 */
	public PortletRequestAttributes(PortletRequest request) {
		Assert.notNull(request, "Request must not be null");
		this.request = request;
	}

	/**
	 * Create a new PortletRequestAttributes instance for the given request.
	 * @param request current portlet request
	 * @param response current portlet response (for optional exposure)
	 */
	public PortletRequestAttributes(PortletRequest request, PortletResponse response) {
		this(request);
		this.response = response;
	}


	/**
	 * Exposes the native {@link PortletRequest} that we're wrapping.
	 */
	public final PortletRequest getRequest() {
		return this.request;
	}

	/**
	 * Exposes the native {@link PortletResponse} that we're wrapping (if any).
	 */
	public final PortletResponse getResponse() {
		return this.response;
	}

	/**
	 * Exposes the {@link PortletSession} that we're wrapping.
	 * @param allowCreate whether to allow creation of a new session if none exists yet
	 */
	protected final PortletSession getSession(boolean allowCreate) {
		if (isRequestActive()) {
			PortletSession session = this.request.getPortletSession(allowCreate);
			this.session = session;
			return session;
		}
		else {
			// Access through stored session reference, if any...
			PortletSession session = this.session;
			if (session == null) {
				if (allowCreate) {
					throw new IllegalStateException(
							"No session found and request already completed - cannot create new session!");
				}
				else {
					session = this.request.getPortletSession(false);
					this.session = session;
				}
			}
			return session;
		}
	}


	@Override
	public Object getAttribute(String name, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot ask for request attribute - request is not active anymore!");
			}
			return this.request.getAttribute(name);
		}
		else {
			PortletSession session = getSession(false);
			if (session != null) {
				if (scope == SCOPE_GLOBAL_SESSION) {
					Object value = session.getAttribute(name, PortletSession.APPLICATION_SCOPE);
					if (value != null) {
						this.globalSessionAttributesToUpdate.put(name, value);
					}
					return value;
				}
				else {
					Object value = session.getAttribute(name);
					if (value != null) {
						this.sessionAttributesToUpdate.put(name, value);
					}
					return value;
				}
			}
			return null;
		}
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot set request attribute - request is not active anymore!");
			}
			this.request.setAttribute(name, value);
		}
		else {
			PortletSession session = getSession(true);
			if (scope == SCOPE_GLOBAL_SESSION) {
				session.setAttribute(name, value, PortletSession.APPLICATION_SCOPE);
				this.globalSessionAttributesToUpdate.remove(name);
			}
			else {
				session.setAttribute(name, value);
				this.sessionAttributesToUpdate.remove(name);
			}
		}
	}

	@Override
	public void removeAttribute(String name, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (isRequestActive()) {
				this.request.removeAttribute(name);
				removeRequestDestructionCallback(name);
			}
		}
		else {
			PortletSession session = getSession(false);
			if (session != null) {
				if (scope == SCOPE_GLOBAL_SESSION) {
					session.removeAttribute(name, PortletSession.APPLICATION_SCOPE);
					this.globalSessionAttributesToUpdate.remove(name);
				}
				else {
					session.removeAttribute(name);
					this.sessionAttributesToUpdate.remove(name);
				}
			}
		}
	}

	@Override
	public String[] getAttributeNames(int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot ask for request attributes - request is not active anymore!");
			}
			return StringUtils.toStringArray(this.request.getAttributeNames());
		}
		else {
			PortletSession session = getSession(false);
			if (session != null) {
				if (scope == SCOPE_GLOBAL_SESSION) {
					return StringUtils.toStringArray(session.getAttributeNames(PortletSession.APPLICATION_SCOPE));
				}
				else {
					return StringUtils.toStringArray(session.getAttributeNames());
				}
			}
			return new String[0];
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback, int scope) {
		if (scope == SCOPE_REQUEST) {
			registerRequestDestructionCallback(name, callback);
		}
		else {
			registerSessionDestructionCallback(name, callback);
		}
	}

	@Override
	public Object resolveReference(String key) {
		if (REFERENCE_REQUEST.equals(key)) {
			return this.request;
		}
		else if (REFERENCE_SESSION.equals(key)) {
			return getSession(true);
		}
		else {
			return null;
		}
	}

	@Override
	public String getSessionId() {
		return getSession(true).getId();
	}

	@Override
	public Object getSessionMutex() {
		return PortletUtils.getSessionMutex(getSession(true));
	}


	/**
	 * Update all accessed session attributes through {@code session.setAttribute}
	 * calls, explicitly indicating to the container that they might have been modified.
	 */
	@Override
	protected void updateAccessedSessionAttributes() {
		if (!this.sessionAttributesToUpdate.isEmpty() || !this.globalSessionAttributesToUpdate.isEmpty()) {
			PortletSession session = getSession(false);
			if (session != null) {
				try {
					for (Map.Entry<String, Object> entry : this.sessionAttributesToUpdate.entrySet()) {
						String name = entry.getKey();
						Object newValue = entry.getValue();
						Object oldValue = session.getAttribute(name);
						if (oldValue == newValue) {
							session.setAttribute(name, newValue);
						}
					}
					for (Map.Entry<String, Object> entry : this.globalSessionAttributesToUpdate.entrySet()) {
						String name = entry.getKey();
						Object newValue = entry.getValue();
						Object oldValue = session.getAttribute(name, PortletSession.APPLICATION_SCOPE);
						if (oldValue == newValue) {
							session.setAttribute(name, newValue, PortletSession.APPLICATION_SCOPE);
						}
					}
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
				}
			}
			this.sessionAttributesToUpdate.clear();
			this.globalSessionAttributesToUpdate.clear();
		}
	}

	/**
	 * Register the given callback as to be executed after session termination.
	 * <p>Note: The callback object should be serializable in order to survive
	 * web app restarts.
	 * @param name the name of the attribute to register the callback for
	 * @param callback the callback to be executed for destruction
	 */
	protected void registerSessionDestructionCallback(String name, Runnable callback) {
		PortletSession session = getSession(true);
		session.setAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name,
				new DestructionCallbackBindingListener(callback));
	}


	@Override
	public String toString() {
		return this.request.toString();
	}

}
