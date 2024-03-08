/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.IOException;

/**
 * Raised when the response for an asynchronous request becomes unusable as
 * indicated by a write failure, or a Servlet container error notification, or
 * after the async request has completed.
 *
 * <p>The exception relies on response wrapping, and on {@code AsyncListener}
 * notifications, managed by {@link StandardServletAsyncWebRequest}.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.33
 */
@SuppressWarnings("serial")
public class AsyncRequestNotUsableException extends IOException {


	public AsyncRequestNotUsableException(String message) {
		super(message);
	}

	public AsyncRequestNotUsableException(String message, Throwable cause) {
		super(message, cause);
	}

}
