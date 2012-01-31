/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.portlet.handler;

import java.io.IOException;

import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSecurityException;

/**
 * Interceptor that checks the authorization of the current user via the
 * user's roles, as evaluated by PortletRequest's isUserInRole method.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.portlet.PortletRequest#isUserInRole
 */
public class UserRoleAuthorizationInterceptor extends HandlerInterceptorAdapter {

	private String[] authorizedRoles;


	/**
	 * Set the roles that this interceptor should treat as authorized.
	 * @param authorizedRoles array of role names
	 */
	public final void setAuthorizedRoles(String[] authorizedRoles) {
		this.authorizedRoles = authorizedRoles;
	}


	@Override
	public final boolean preHandle(PortletRequest request, PortletResponse response, Object handler)
			throws PortletException, IOException {

		if (this.authorizedRoles != null) {
			for (String role : this.authorizedRoles) {
				if (request.isUserInRole(role)) {
					return true;
				}
			}
		}
		handleNotAuthorized(request, response, handler);
		return false;
	}

	/**
	 * Handle a request that is not authorized according to this interceptor.
	 * Default implementation throws a new PortletSecurityException.
	 * <p>This method can be overridden to write a custom message, forward or
	 * redirect to some error page or login page, or throw a PortletException.
	 * @param request current portlet request
	 * @param response current portlet response
	 * @param handler chosen handler to execute, for type and/or instance evaluation
	 * @throws javax.portlet.PortletException if there is an internal error
	 * @throws java.io.IOException in case of an I/O error when writing the response
	 */
	protected void handleNotAuthorized(PortletRequest request, PortletResponse response, Object handler)
			throws PortletException, IOException {

		throw new PortletSecurityException("Request not authorized");
	}

}
