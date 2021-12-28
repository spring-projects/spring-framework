/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.multipart.support;

import jakarta.servlet.ServletException;

import org.springframework.web.multipart.MultipartResolver;

/**
 * Raised when the part of a "multipart/form-data" request identified by its
 * name cannot be found.
 *
 * <p>This may be because the request is not a multipart/form-data request,
 * because the part is not present in the request, or because the web
 * application is not configured correctly for processing multipart requests,
 * e.g. no {@link MultipartResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class MissingServletRequestPartException extends ServletException {

	private final String requestPartName;


	/**
	 * Constructor for MissingServletRequestPartException.
	 * @param requestPartName the name of the missing part of the multipart request
	 */
	public MissingServletRequestPartException(String requestPartName) {
		super("Required request part '" + requestPartName + "' is not present");
		this.requestPartName = requestPartName;
	}


	/**
	 * Return the name of the offending part of the multipart request.
	 */
	public String getRequestPartName() {
		return this.requestPartName;
	}

}
