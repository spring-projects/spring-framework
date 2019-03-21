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

package org.springframework.mock.web.portlet;

import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;

/**
 * Mock implementation of the {@link javax.portlet.EventRequest} interface.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MockEventRequest extends MockPortletRequest implements EventRequest {

	private final Event event;

	private String method;


	/**
	 * Create a new MockEventRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param event the event that this request wraps
	 * @see MockEvent
	 */
	public MockEventRequest(Event event) {
		super();
		this.event = event;
	}

	/**
	 * Create a new MockEventRequest with a default {@link MockPortalContext}.
	 * @param event the event that this request wraps
	 * @param portletContext the PortletContext that the request runs in
	 * @see MockEvent
	 */
	public MockEventRequest(Event event, PortletContext portletContext) {
		super(portletContext);
		this.event = event;
	}

	/**
	 * Create a new MockEventRequest.
	 * @param event the event that this request wraps
	 * @param portalContext the PortletContext that the request runs in
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockEventRequest(Event event, PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
		this.event = event;
	}


	@Override
	protected String getLifecyclePhase() {
		return EVENT_PHASE;
	}

	@Override
	public Event getEvent() {
		return this.event;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	@Override
	public String getMethod() {
		return this.method;
	}

}