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

package org.springframework.web.jsf;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

/**
 * Convenience methods to retrieve the root WebApplicationContext for a given
 * FacesContext. This is e.g. useful for accessing a Spring context from
 * custom JSF code.
 *
 * <p>Analogous to Spring's WebApplicationContextUtils for the ServletContext.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.web.context.ContextLoader
 * @see org.springframework.web.context.support.WebApplicationContextUtils
 */
public abstract class FacesContextUtils {

	/**
	 * Find the root WebApplicationContext for this web app, which is
	 * typically loaded via ContextLoaderListener or ContextLoaderServlet.
	 * <p>Will rethrow an exception that happened on root context startup,
	 * to differentiate between a failed context startup and no context at all.
	 * @param fc the FacesContext to find the web application context for
	 * @return the root WebApplicationContext for this web app, or <code>null</code> if none
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static WebApplicationContext getWebApplicationContext(FacesContext fc) {
		Assert.notNull(fc, "FacesContext must not be null");
		Object attr = fc.getExternalContext().getApplicationMap().get(
				WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (attr == null) {
			return null;
		}
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		if (!(attr instanceof WebApplicationContext)) {
			throw new IllegalStateException("Root context attribute is not of type WebApplicationContext: " + attr);
		}
		return (WebApplicationContext) attr;
	}

	/**
	 * Find the root WebApplicationContext for this web app, which is
	 * typically loaded via ContextLoaderListener or ContextLoaderServlet.
	 * <p>Will rethrow an exception that happened on root context startup,
	 * to differentiate between a failed context startup and no context at all.
	 * @param fc the FacesContext to find the web application context for
	 * @return the root WebApplicationContext for this web app
	 * @throws IllegalStateException if the root WebApplicationContext could not be found
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static WebApplicationContext getRequiredWebApplicationContext(FacesContext fc)
		throws IllegalStateException {

		WebApplicationContext wac = getWebApplicationContext(fc);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}
		return wac;
	}

	/**
	 * Return the best available mutex for the given session:
	 * that is, an object to synchronize on for the given session.
	 * <p>Returns the session mutex attribute if available; usually,
	 * this means that the HttpSessionMutexListener needs to be defined
	 * in <code>web.xml</code>. Falls back to the Session reference itself
	 * if no mutex attribute found.
	 * <p>The session mutex is guaranteed to be the same object during
	 * the entire lifetime of the session, available under the key defined
	 * by the <code>SESSION_MUTEX_ATTRIBUTE</code> constant. It serves as a
	 * safe reference to synchronize on for locking on the current session.
	 * <p>In many cases, the Session reference itself is a safe mutex
	 * as well, since it will always be the same object reference for the
	 * same active logical session. However, this is not guaranteed across
	 * different servlet containers; the only 100% safe way is a session mutex.
	 * @param fc the FacesContext to find the session mutex for
	 * @return the mutex object (never <code>null</code>)
	 * @see org.springframework.web.util.WebUtils#SESSION_MUTEX_ATTRIBUTE
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 */
	public static Object getSessionMutex(FacesContext fc) {
		Assert.notNull(fc, "FacesContext must not be null");
		ExternalContext ec = fc.getExternalContext();
		Object mutex = ec.getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = ec.getSession(true);
		}
		return mutex;
	}

}
