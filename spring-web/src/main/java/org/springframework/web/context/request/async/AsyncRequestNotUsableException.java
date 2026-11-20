/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
