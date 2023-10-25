/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.resource;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Raised when {@link ResourceWebHandler} is mapped to the request but can not
 * find a matching resource.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
@SuppressWarnings("serial")
public class NoResourceFoundException extends ResponseStatusException {


	public NoResourceFoundException(String resourcePath) {
		super(HttpStatus.NOT_FOUND, "No static resource " + resourcePath + ".");
		setDetail(getReason());
	}

}
