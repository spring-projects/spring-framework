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

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * A Runnable for invoking a chain of Callable instances and completing async
 * request processing while also dealing with any unhandled exceptions.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see AsyncExecutionChain#startCallableChainProcessing()
 * @see AsyncExecutionChain#startDeferredResultProcessing(DeferredResult)
 */
public class AsyncExecutionChainRunnable implements Runnable {

	private static final Log logger = LogFactory.getLog(AsyncExecutionChainRunnable.class);

	private final AsyncWebRequest asyncWebRequest;

	private final Callable<?> callable;

	/**
	 * Class constructor.
	 * @param asyncWebRequest the async request
	 * @param callable the async execution chain
	 */
	public AsyncExecutionChainRunnable(AsyncWebRequest asyncWebRequest, Callable<?> callable) {
		Assert.notNull(asyncWebRequest, "An AsyncWebRequest is required");
		Assert.notNull(callable, "A Callable is required");
		this.asyncWebRequest = asyncWebRequest;
		this.callable = callable;
	}

	/**
	 * Run the async execution chain and complete the async request.
	 * <p>A {@link StaleAsyncWebRequestException} is logged at debug level and
	 * absorbed while any other unhandled {@link Exception} results in a 500
	 * response code.
	 */
	public void run() {
		try {
			this.callable.call();
		}
		catch (StaleAsyncWebRequestException ex) {
			logger.trace("Could not complete async request", ex);
		}
		catch (Exception ex) {
			logger.trace("Could not complete async request", ex);
			this.asyncWebRequest.sendError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
		}
		finally {
			logger.debug("Completing async request processing");
			this.asyncWebRequest.complete();
		}
	}

}
