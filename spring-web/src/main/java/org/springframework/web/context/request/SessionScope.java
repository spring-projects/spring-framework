/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.context.request;

import org.springframework.beans.factory.ObjectFactory;

/**
 * Session-backed {@link org.springframework.beans.factory.config.Scope}
 * implementation.
 *
 * <p>Relies on a thread-bound {@link RequestAttributes} instance, which
 * can be exported through {@link RequestContextListener},
 * {@link org.springframework.web.filter.RequestContextFilter} or
 * {@link org.springframework.web.servlet.DispatcherServlet}.
 *
 * <p>This {@code Scope} will also work for Portlet environments,
 * through an alternate {@code RequestAttributes} implementation
 * (as exposed out-of-the-box by Spring's
 * {@link org.springframework.web.portlet.DispatcherPortlet}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see RequestContextHolder#currentRequestAttributes()
 * @see RequestAttributes#SCOPE_SESSION
 * @see RequestAttributes#SCOPE_GLOBAL_SESSION
 * @see RequestContextListener
 * @see org.springframework.web.filter.RequestContextFilter
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.portlet.DispatcherPortlet
 */
public class SessionScope extends AbstractRequestAttributesScope {

	private final int scope;


	/**
	 * Create a new SessionScope, storing attributes in a locally
	 * isolated session (or default session, if there is no distinction
	 * between a global session and a component-specific session).
	 */
	public SessionScope() {
		this.scope = RequestAttributes.SCOPE_SESSION;
	}

	/**
	 * Create a new SessionScope, specifying whether to store attributes
	 * in the global session, provided that such a distinction is available.
	 * <p>This distinction is important for Portlet environments, where there
	 * are two notions of a session: "portlet scope" and "application scope".
	 * If this flag is on, objects will be put into the "application scope" session;
	 * else they will end up in the "portlet scope" session (the typical default).
	 * <p>In a Servlet environment, this flag is effectively ignored.
	 * @param globalSession {@code true} in case of the global session as target;
	 * {@code false} in case of a component-specific session as target
	 * @see org.springframework.web.portlet.context.PortletRequestAttributes
	 * @see ServletRequestAttributes
	 */
	public SessionScope(boolean globalSession) {
		this.scope = (globalSession ? RequestAttributes.SCOPE_GLOBAL_SESSION : RequestAttributes.SCOPE_SESSION);
	}


	@Override
	protected int getScope() {
		return this.scope;
	}

	@Override
	public String getConversationId() {
		return RequestContextHolder.currentRequestAttributes().getSessionId();
	}

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
		synchronized (mutex) {
			return super.get(name, objectFactory);
		}
	}

	@Override
	public Object remove(String name) {
		Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
		synchronized (mutex) {
			return super.remove(name);
		}
	}

}
