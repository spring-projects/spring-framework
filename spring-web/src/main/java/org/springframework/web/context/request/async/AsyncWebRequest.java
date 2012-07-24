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

import org.springframework.web.context.request.NativeWebRequest;


/**
 * Extends {@link NativeWebRequest} with methods for asynchronous request processing.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface AsyncWebRequest extends NativeWebRequest {

	/**
	 * Set the time required for concurrent handling to complete.
	 * @param timeout amount of time in milliseconds
	 */
	void setTimeout(Long timeout);

	/**
	 * Provide a Runnable to invoke on timeout.
	 */
	void setTimeoutHandler(Runnable runnable);

	/**
	 * Provide a Runnable to invoke at the end of asynchronous request processing.
	 */
	void addCompletionHandler(Runnable runnable);

	/**
	 * Mark the start of asynchronous request processing so that when the main
	 * processing thread exits, the response remains open for further processing
	 * in another thread.
	 * @throws IllegalStateException if async processing has completed or is not supported
	 */
	void startAsync();

	/**
	 * Whether the request is in asynchronous mode after a call to {@link #startAsync()}.
	 * Returns "false" if asynchronous processing never started, has completed, or the
	 * request was dispatched for further processing.
	 */
	boolean isAsyncStarted();

	/**
	 * Dispatch the request to the container in order to resume processing after
	 * concurrent execution in an application thread.
	 */
	void dispatch();

	/**
	 * Whether the request was dispatched to the container.
	 */
	boolean isDispatched();

	/**
	 * Whether asynchronous processing has completed in which case the request
	 * response should no longer be used.
	 */
	boolean isAsyncComplete();

}
