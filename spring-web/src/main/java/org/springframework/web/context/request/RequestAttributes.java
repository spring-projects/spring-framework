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

package org.springframework.web.context.request;

/**
 * Abstraction for accessing attribute objects associated with a request.
 * Supports access to request-scoped attributes as well as to session-scoped
 * attributes, with the optional notion of a "global session".
 *
 * <p>Can be implemented for any kind of request/session mechanism,
 * in particular for servlet requests and portlet requests.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ServletRequestAttributes
 * @see org.springframework.web.portlet.context.PortletRequestAttributes
 */
public interface RequestAttributes {

	/**
	 * Constant that indicates request scope.
	 */
	int SCOPE_REQUEST = 0;

	/**
	 * Constant that indicates session scope.
	 * <p>This preferably refers to a locally isolated session, if such
	 * a distinction is available (for example, in a Portlet environment).
	 * Else, it simply refers to the common session.
	 */
	int SCOPE_SESSION = 1;

	/**
	 * Constant that indicates global session scope.
	 * <p>This explicitly refers to a globally shared session, if such
	 * a distinction is available (for example, in a Portlet environment).
	 * Else, it simply refers to the common session.
	 */
	int SCOPE_GLOBAL_SESSION = 2;


	/**
	 * Name of the standard reference to the request object: "request".
	 * @see #resolveReference
	 */
	String REFERENCE_REQUEST = "request";

	/**
	 * Name of the standard reference to the session object: "session".
	 * @see #resolveReference
	 */
	String REFERENCE_SESSION = "session";


	/**
	 * Return the value for the scoped attribute of the given name, if any.
	 * @param name the name of the attribute
	 * @param scope the scope identifier
	 * @return the current attribute value, or {@code null} if not found
	 */
	Object getAttribute(String name, int scope);

	/**
	 * Set the value for the scoped attribute of the given name,
	 * replacing an existing value (if any).
	 * @param name the name of the attribute
	 * @param scope the scope identifier
	 * @param value the value for the attribute
	 */
	void setAttribute(String name, Object value, int scope);

	/**
	 * Remove the scoped attribute of the given name, if it exists.
	 * <p>Note that an implementation should also remove a registered destruction
	 * callback for the specified attribute, if any. It does, however, <i>not</i>
	 * need to <i>execute</i> a registered destruction callback in this case,
	 * since the object will be destroyed by the caller (if appropriate).
	 * @param name the name of the attribute
	 * @param scope the scope identifier
	 */
	void removeAttribute(String name, int scope);

	/**
	 * Retrieve the names of all attributes in the scope.
	 * @param scope the scope identifier
	 * @return the attribute names as String array
	 */
	String[] getAttributeNames(int scope);

	/**
	 * Register a callback to be executed on destruction of the
	 * specified attribute in the given scope.
	 * <p>Implementations should do their best to execute the callback
	 * at the appropriate time: that is, at request completion or session
	 * termination, respectively. If such a callback is not supported by the
	 * underlying runtime environment, the callback <i>must be ignored</i>
	 * and a corresponding warning should be logged.
	 * <p>Note that 'destruction' usually corresponds to destruction of the
	 * entire scope, not to the individual attribute having been explicitly
	 * removed by the application. If an attribute gets removed via this
	 * facade's {@link #removeAttribute(String, int)} method, any registered
	 * destruction callback should be disabled as well, assuming that the
	 * removed object will be reused or manually destroyed.
	 * <p><b>NOTE:</b> Callback objects should generally be serializable if
	 * they are being registered for a session scope. Otherwise the callback
	 * (or even the entire session) might not survive web app restarts.
	 * @param name the name of the attribute to register the callback for
	 * @param callback the destruction callback to be executed
	 * @param scope the scope identifier
	 */
	void registerDestructionCallback(String name, Runnable callback, int scope);

	/**
	 * Resolve the contextual reference for the given key, if any.
	 * <p>At a minimum: the HttpServletRequest/PortletRequest reference for key
	 * "request", and the HttpSession/PortletSession reference for key "session".
	 * @param key the contextual key
	 * @return the corresponding object, or {@code null} if none found
	 */
	Object resolveReference(String key);

	/**
	 * Return an id for the current underlying session.
	 * @return the session id as String (never {@code null}
	 */
	String getSessionId();

	/**
	 * Expose the best available mutex for the underlying session:
	 * that is, an object to synchronize on for the underlying session.
	 * @return the session mutex to use (never {@code null}
	 */
	Object getSessionMutex();

}
