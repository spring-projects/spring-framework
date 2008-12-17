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

package org.springframework.web.portlet.handler;

import javax.portlet.PortletSecurityException;

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;

/**
 * @author Mark Fisher
 */
public class UserRoleAuthorizationInterceptorTests extends TestCase {

	public void testAuthorizedUser() throws Exception {
		UserRoleAuthorizationInterceptor interceptor = new UserRoleAuthorizationInterceptor();
		String validRole = "allowed";
		interceptor.setAuthorizedRoles(new String[] {validRole});
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		Object handler = new Object();
		request.addUserRole(validRole);
		assertTrue(request.isUserInRole(validRole));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
	}

	public void testAuthorizedUserWithMultipleRoles() throws Exception {
		UserRoleAuthorizationInterceptor interceptor = new UserRoleAuthorizationInterceptor();
		String validRole1 = "allowed1";
		String validRole2 = "allowed2";
		interceptor.setAuthorizedRoles(new String[] {validRole1, validRole2});
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		Object handler = new Object();
		request.addUserRole(validRole2);
		request.addUserRole("someOtherRole");
		assertFalse(request.isUserInRole(validRole1));
		assertTrue(request.isUserInRole(validRole2));
		boolean shouldProceed = interceptor.preHandle(request, response, handler);
		assertTrue(shouldProceed);
	}

	public void testUnauthorizedUser() throws Exception {
		UserRoleAuthorizationInterceptor interceptor = new UserRoleAuthorizationInterceptor();
		String validRole = "allowed";
		interceptor.setAuthorizedRoles(new String[] {validRole});
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		Object handler = new Object();
		request.addUserRole("someOtherRole");
		assertFalse(request.isUserInRole(validRole));
		try {
			interceptor.preHandle(request, response, handler);
			fail("should have thrown PortletSecurityException");
		}
		catch (PortletSecurityException ex) {
			// expected
		}
	}

	public void testRequestWithNoUserRoles() throws Exception {
		UserRoleAuthorizationInterceptor interceptor = new UserRoleAuthorizationInterceptor();
		String validRole = "allowed";
		interceptor.setAuthorizedRoles(new String[] {validRole});
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		Object handler = new Object();
		assertFalse(request.isUserInRole(validRole));
		try {
			interceptor.preHandle(request, response, handler);
			fail("should have thrown PortletSecurityException");
		}
		catch (PortletSecurityException ex) {
			// expected
		}
	}
	
	public void testInterceptorWithNoAuthorizedRoles() throws Exception {
		UserRoleAuthorizationInterceptor interceptor = new UserRoleAuthorizationInterceptor();
		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		Object handler = new Object();
		request.addUserRole("someRole");
		try {
			interceptor.preHandle(request, response, handler);
			fail("should have thrown PortletSecurityException");
		}
		catch (PortletSecurityException ex) {
			// expected
		}
	}

}
