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
package org.springframework.http.server;

/**
 * A control that can put the processing of an HTTP request in asynchronous mode during
 * which the response remains open until explicitly closed.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ServerHttpAsyncRequestControl {

	/**
	 * Enable asynchronous processing after which the response remains open until a call
	 * to {@link #complete()} is made or the server times out the request. Once enabled,
	 * additional calls to this method are ignored.
	 */
	void start();

	/**
	 * A variation on {@link #start()} that allows specifying a timeout value to use to
	 * use for asynchronous processing. If {@link #complete()} is not called within the
	 * specified value, the request times out.
	 */
	void start(long timeout);

	/**
	 * Whether asynchronous request processing has been started.
	 */
	boolean isStarted();

	/**
	 * Causes asynchronous request processing to be completed.
	 */
	void complete();

	/**
	 * Whether asynchronous request processing has been completed.
	 */
	boolean isCompleted();

}
