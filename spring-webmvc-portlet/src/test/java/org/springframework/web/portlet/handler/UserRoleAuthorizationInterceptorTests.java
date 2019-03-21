/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.portlet.handler;

import javax.portlet.PortletSecurityException;

import org.junit.Test;

import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Sam Brannen
 */
public class UserRoleAuthorizationInterceptorTests {

	private final UserRoleAuthorizationInterceptor interceptor = new UserRoleAuthorizationInterceptor();

	private final MockRenderRequest request = new MockRenderRequest();

	private final MockRenderResponse response = new MockRenderResponse();


	@Test
	public void authorizedUser() throws Exception {
		String validRole = "allowed";
		interceptor.setAuthorizedRoles(new String[] {validRole});
		Object handler = new Object();
		request.addUserRole(validRole);
		assertTrue(request.isUserInRole(validRole));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
	}

	@Test
	public void authorizedUserWithMultipleRoles() throws Exception {
		String validRole1 = "allowed1";
		String validRole2 = "allowed2";
		interceptor.setAuthorizedRoles(new String[] {validRole1, validRole2});
		Object handler = new Object();
		request.addUserRole(validRole2);
		request.addUserRole("someOtherRole");
		assertFalse(request.isUserInRole(validRole1));
		assertTrue(request.isUserInRole(validRole2));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
	}

	@Test(expected = PortletSecurityException.class)
	public void unauthorizedUser() throws Exception {
		String validRole = "allowed";
		interceptor.setAuthorizedRoles(new String[] {validRole});
		request.addUserRole("someOtherRole");
		assertFalse(request.isUserInRole(validRole));

		interceptor.preHandle(request, response, new Object());
	}

	@Test(expected = PortletSecurityException.class)
	public void requestWithNoUserRoles() throws Exception {
		String validRole = "allowed";
		interceptor.setAuthorizedRoles(new String[] {validRole});
		assertFalse(request.isUserInRole(validRole));

		interceptor.preHandle(request, response, new Object());
	}

	@Test(expected = PortletSecurityException.class)
	public void interceptorWithNoAuthorizedRoles() throws Exception {
		request.addUserRole("someRole");
		interceptor.preHandle(request, response, new Object());
	}

}
