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

import javax.portlet.ActionRequest;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;

/**
 * Mock implementation of the {@link javax.portlet.ActionRequest} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockActionRequest extends MockClientDataRequest implements ActionRequest {

	/**
	 * Create a new MockActionRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @see org.springframework.mock.web.portlet.MockPortalContext
	 * @see org.springframework.mock.web.portlet.MockPortletContext
	 */
	public MockActionRequest() {
		super();
	}

	/**
	 * Create a new MockActionRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param actionName the name of the action to trigger
	 */
	public MockActionRequest(String actionName) {
		super();
		setParameter(ActionRequest.ACTION_NAME, actionName);
	}

	/**
	 * Create a new MockActionRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param portletMode the mode that the portlet runs in
	 */
	public MockActionRequest(PortletMode portletMode) {
		super();
		setPortletMode(portletMode);
	}

	/**
	 * Create a new MockActionRequest with a default {@link MockPortalContext}.
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockActionRequest(PortletContext portletContext) {
		super(portletContext);
	}

	/**
	 * Create a new MockActionRequest.
	 * @param portalContext the PortalContext that the request runs in
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockActionRequest(PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
	}


	@Override
	protected String getLifecyclePhase() {
		return ACTION_PHASE;
	}

}
