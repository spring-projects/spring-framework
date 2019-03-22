/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import org.springframework.core.NestedRuntimeException;

/**
 * Abstract base class for exception published by {@link WebClient} in case of errors.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class WebClientException extends NestedRuntimeException {

	private static final long serialVersionUID = 472776714118912855L;

	/**
	 * Construct a new instance of {@code WebClientException} with the given message.
	 * @param msg the message
	 */
	public WebClientException(String msg) {
		super(msg);
	}

	/**
	 * Construct a new instance of {@code WebClientException} with the given message
	 * and exception.
	 * @param msg the message
	 * @param ex the exception
	 */
	public WebClientException(String msg, Throwable ex) {
		super(msg, ex);
	}

}
