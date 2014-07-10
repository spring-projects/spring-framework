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
 * Encapsulates information required to create a status controller for 3xx redirect requests.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class RedirectStatusControllerRegistration extends AbstractStatusControllerRegistration {

	private final String redirectPath;

	private boolean useQueryString = false;


	/**
	 * Creates a {@link RedirectStatusControllerRegistration} with the given URL path and status code.
	 * When a request matches to the given URL path, this status controller will process it and
	 * redirect the request to the specified redirect path.
	 */
	public RedirectStatusControllerRegistration(String urlPath, HttpStatus statusCode, String redirectPath) {
		super(urlPath, statusCode);
		this.redirectPath = redirectPath;
	}


	/**
	 * The initial request query string will be appended to the redirect path.
	 */
	public RedirectStatusControllerRegistration useQueryString() {
		this.useQueryString = true;
		return this;
	}

	protected String getRedirectPath() {
		return redirectPath;
	}

	protected boolean isUseQueryString() {
		return useQueryString;
	}

	@Override
	protected Object getStatusController() {
		StatusController controller = new StatusController(this.statusCode);
		controller.setRedirectPath(this.redirectPath);
		controller.setUseQueryString(this.useQueryString);
		return controller;
	}
}
