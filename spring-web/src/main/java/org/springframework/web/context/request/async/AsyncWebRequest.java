/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.context.request.async;

import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.NativeWebRequest;


/**
 * Extends {@link NativeWebRequest} with methods for starting, completing, and
 * configuring async request processing. Abstract underlying mechanisms such as
 * the Servlet 3.0 AsyncContext.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface AsyncWebRequest extends NativeWebRequest {

	/**
	 * Set the timeout for asynchronous request processing. When the timeout
	 * begins depends on the underlying technology. With the Servlet 3 async
	 * support the timeout begins after the main processing thread has exited
	 * and has been returned to the container pool.
	 */
	void setTimeout(Long timeout);

	/**
	 * Marks the start of async request processing for example. Ensures the
	 * request remains open to be completed in a separate thread.
	 */
	void startAsync();

	/**
	 * Return {@code true} if async processing has started following a call to
	 * {@link #startAsync()} and before it has completed.
	 */
	boolean isAsyncStarted();

	/**
	 * Complete async request processing finalizing the underlying request.
	 */
	void complete();

	/**
	 * Send an error to the client.
	 */
	void sendError(HttpStatus status, String message);

	/**
	 * Return {@code true} if async processing completed either normally or for
	 * any other reason such as a timeout or an error. Note that a timeout or a
	 * (client) error may occur in a separate thread while async processing is
	 * still in progress in its own thread. Hence the underlying async request
	 * may become stale and this method may return {@code true} even if
	 * {@link #complete()} was never actually called.
	 * @see StaleAsyncWebRequestException
	 */
	boolean isAsyncCompleted();

}
