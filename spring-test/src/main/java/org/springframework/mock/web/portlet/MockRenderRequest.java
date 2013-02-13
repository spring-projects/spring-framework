/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;
import javax.portlet.RenderRequest;
import javax.portlet.WindowState;

/**
 * Mock implementation of the {@link javax.portlet.RenderRequest} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockRenderRequest extends MockPortletRequest implements RenderRequest {

	/**
	 * Create a new MockRenderRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @see MockPortalContext
	 * @see MockPortletContext
	 */
	public MockRenderRequest() {
		super();
	}

	/**
	 * Create a new MockRenderRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param portletMode the mode that the portlet runs in
	 */
	public MockRenderRequest(PortletMode portletMode) {
		super();
		setPortletMode(portletMode);
	}

	/**
	 * Create a new MockRenderRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param portletMode the mode that the portlet runs in
	 * @param windowState the window state to run the portlet in
	 */
	public MockRenderRequest(PortletMode portletMode, WindowState windowState) {
		super();
		setPortletMode(portletMode);
		setWindowState(windowState);
	}

	/**
	 * Create a new MockRenderRequest with a default {@link MockPortalContext}.
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockRenderRequest(PortletContext portletContext) {
		super(portletContext);
	}

	/**
	 * Create a new MockRenderRequest.
	 * @param portalContext the PortletContext that the request runs in
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockRenderRequest(PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
	}


	@Override
	protected String getLifecyclePhase() {
		return RENDER_PHASE;
	}

	public String getETag() {
		return getProperty(RenderRequest.ETAG);
	}

}
