/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.core;

import org.springframework.messaging.MessagingException;

/**
 * Thrown by a {@link DestinationResolver} when it cannot resolve a destination.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DestinationResolutionException extends MessagingException {


	/**
	 * Create an instance with the given description only.
	 *
	 * @param description the description
	 */
	public DestinationResolutionException(String description) {
		super(description);
	}

	/**
	 * Create an instance with the given description and original cause.
	 *
	 * @param description the description
	 * @param cause the root cause
	 */
	public DestinationResolutionException(String description, Throwable cause) {
		super(description, cause);
	}

}
