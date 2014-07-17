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

/**
 * Base class for information required to create a status controller.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public abstract class AbstractStatusControllerRegistration {

	protected final String urlPath;

	protected final HttpStatus statusCode;


	/**
	 * Creates a {@link AbstractStatusControllerRegistration} with the given URL path and status code.
	 * When a request matches to the given URL path, this status controller will process it.
	 */
	public AbstractStatusControllerRegistration(String urlPath, HttpStatus statusCode) {
		this.urlPath = urlPath;
		this.statusCode = statusCode;
	}


	/**
	 * Returns the URL path for the status controller.
	 */
	protected String getUrlPath() {
		return urlPath;
	}

	/**
	 * Returns the status code for the status controller.
	 */
	protected HttpStatus getStatusCode() {
		return statusCode;
	}

	protected abstract Object getStatusController();
}
