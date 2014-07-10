/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.StatusController;

/**
 * Encapsulates information required to create a status controller.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class StatusControllerRegistration extends AbstractStatusControllerRegistration {

	private String viewName;

	private String reason;


	/**
	 * Creates a {@link StatusControllerRegistration} with the given URL path and status code.
	 * When a request matches to the given URL path, this status controller will process it.
	 */
	public StatusControllerRegistration(String urlPath, HttpStatus statusCode) {
		super(urlPath, statusCode);
	}


	/**
	 * Sets the view name to use for this view controller.
	 * It is not possible to set both viewName and reason, you must use either one or the other.
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	/**
	 * Sets the reason to display.
	 * It is not possible to set both viewName and reason, you must use either one or the other.
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	protected Object getStatusController() {
		StatusController controller = new StatusController(this.statusCode);
		controller.setViewName(this.viewName);
		controller.setReason(this.reason);
		return controller;
	}
}
