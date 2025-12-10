/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.server;


import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;

/**
 * Exception for errors that fit response status 413 (payload too large) for use in
 * Spring Web applications.
 *
 * @author Kim Bosung
 * @since 6.2
 * @deprecated since 7.0 in favor of {@link ContentTooLargeException}
 */
@SuppressWarnings("serial")
@Deprecated(since = "7.0")
public class PayloadTooLargeException extends ResponseStatusException {

	public PayloadTooLargeException(@Nullable Throwable cause) {
		super(HttpStatus.PAYLOAD_TOO_LARGE, null, cause);
	}

}
