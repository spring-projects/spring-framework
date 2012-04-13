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


/**
 * Invokes the next Callable in a chain and then checks if the AsyncWebRequest
 * provided to the constructor has ended before returning. Since a timeout or a
 * (client) error may occur in a separate thread while async request processing
 * is still in progress in its own thread, inserting this Callable in the chain
 * protects against use of stale async requests.
 *
 * <p>If an async request was terminated while the next Callable was still
 * processing, a {@link StaleAsyncWebRequestException} is raised.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class StaleAsyncRequestCheckingCallable extends AbstractDelegatingCallable {

	private final AsyncWebRequest asyncWebRequest;

	public StaleAsyncRequestCheckingCallable(AsyncWebRequest asyncWebRequest) {
		this.asyncWebRequest = asyncWebRequest;
	}

	public Object call() throws Exception {
		Object result = getNextCallable().call();
		if (this.asyncWebRequest.isAsyncCompleted()) {
			throw new StaleAsyncWebRequestException(
					"Async request no longer available due to a timed out or a (client) error");
		}
		return result;
	}

}