/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * Sends a 503 (SERVICE_UNAVAILABLE) in case of a timeout if the response is not
 * already committed. As of 4.2.8 this is done indirectly by returning
 * {@link AsyncRequestTimeoutException} as the result of processing which is
 * then handled by Spring MVC's default exception handling as a 503 error.
 *
 * <p>Registered at the end, after all other interceptors and
 * therefore invoked only if no other interceptor handles the timeout.
 *
 * <p>Note that according to RFC 7231, a 503 without a 'Retry-After' header is
 * interpreted as a 500 error and the client should not retry. Applications
 * can install their own interceptor to handle a timeout and add a 'Retry-After'
 * header if necessary.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class TimeoutDeferredResultProcessingInterceptor implements DeferredResultProcessingInterceptor {

	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> result) throws Exception {
		result.setErrorResult(new AsyncRequestTimeoutException());
		return false;
	}

}
