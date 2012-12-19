/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.portlet;

import javax.portlet.PortletException;
import javax.portlet.PortletRequest;

import org.springframework.core.style.StylerUtils;

/**
 * Exception to be thrown if DispatcherPortlet is unable to determine
 * a corresponding handler for an incoming portlet request.
 *
 * @author Juergen Hoeller
 * @since 3.0.5
 */
@SuppressWarnings("serial")
public class NoHandlerFoundException extends PortletException {

	/**
	 * Constructor for NoHandlerFoundException.
	 * @param msg the detail message
	 */
	public NoHandlerFoundException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for NoHandlerFoundException.
	 * @param msg the detail message
	 * @param request the current portlet request,
	 * for further context to be included in the exception message
	 */
	public NoHandlerFoundException(String msg, PortletRequest request) {
		super(msg + ": mode '" + request.getPortletMode() +
				"', phase '" + request.getAttribute(PortletRequest.LIFECYCLE_PHASE) +
				"', parameters " + StylerUtils.style(request.getParameterMap()));
	}

}
