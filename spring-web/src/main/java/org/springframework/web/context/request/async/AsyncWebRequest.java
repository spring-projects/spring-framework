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
 * Extend {@link NativeWebRequest} with methods for async request processing.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface AsyncWebRequest extends NativeWebRequest {

	/**
	 * Set the timeout for asynchronous request processing in milliseconds.
	 * In Servlet 3 async request processing, the timeout begins when the
	 * main processing thread has exited.
	 */
	void setTimeout(Long timeout);

	/**
	 * Mark the start of async request processing for example ensuring the
	 * request remains open in order to be completed in a separate thread.
	 * @throws IllegalStateException if async processing has started, if it is
	 * 	not supported, or if it has completed.
	 */
	void startAsync();

	/**
	 * Whether async processing is in progress and has not yet completed.
	 */
	boolean isAsyncStarted();

	/**
	 * Complete async request processing making a best effort but without any
	 * effect if async request processing has already completed for any reason
	 * including a timeout.
	 */
	void complete();

	/**
	 * Whether async processing has completed either normally via calls to
	 * {@link #complete()} or for other reasons such as a timeout likely
	 * detected in a separate thread during async request processing.
	 */
	boolean isAsyncCompleted();

	/**
	 * Send an error to the client making a best effort to do so but without any
	 * effect if async request processing has already completed, for example due
	 * to a timeout.
	 */
	void sendError(HttpStatus status, String message);

}
