/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * A controller method return value type for asynchronous request processing
 * where one or more objects are written to the response.
 *
 * <p>While {@link org.springframework.web.context.request.async.DeferredResult}
 * is used to produce a single result, a {@code ResponseBodyEmitter} can be used
 * to send multiple objects where each object is written with a compatible
 * {@link org.springframework.http.converter.HttpMessageConverter}.
 *
 * <p>Supported as a return type on its own as well as within a
 * {@link org.springframework.http.ResponseEntity}.
 *
 * <pre>
 * &#064;RequestMapping(value="/stream", method=RequestMethod.GET)
 * public ResponseBodyEmitter handle() {
 * 	   ResponseBodyEmitter emitter = new ResponseBodyEmitter();
 * 	   // Pass the emitter to another component...
 * 	   return emitter;
 * }
 *
 * // in another thread
 * emitter.send(foo1);
 *
 * // and again
 * emitter.send(foo2);
 *
 * // and done
 * emitter.complete();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.2
 */
public class ResponseBodyEmitter {

	private final Long timeout;

	private final Set<DataWithMediaType> earlySendAttempts = new LinkedHashSet<DataWithMediaType>(8);

	private Handler handler;

	private boolean complete;

	private Throwable failure;

	private final DefaultCallback timeoutCallback = new DefaultCallback();

	private final DefaultCallback completionCallback = new DefaultCallback();


	/**
	 * Create a new ResponseBodyEmitter instance.
	 */
	public ResponseBodyEmitter() {
		this.timeout = null;
	}

	/**
	 * Create a ResponseBodyEmitter with a custom timeout value.
	 * <p>By default not set in which case the default configured in the MVC
	 * Java Config or the MVC namespace is used, or if that's not set, then the
	 * timeout depends on the default of the underlying server.
	 * @param timeout timeout value in milliseconds
	 */
	public ResponseBodyEmitter(Long timeout) {
		this.timeout = timeout;
	}


	/**
	 * Return the configured timeout value, if any.
	 */
	public Long getTimeout() {
		return this.timeout;
	}


	synchronized void initialize(Handler handler) throws IOException {
		this.handler = handler;

		for (DataWithMediaType sendAttempt : this.earlySendAttempts) {
			sendInternal(sendAttempt.getData(), sendAttempt.getMediaType());
		}
		this.earlySendAttempts.clear();

		if (this.complete) {
			if (this.failure != null) {
				this.handler.completeWithError(this.failure);
			}
			else {
				this.handler.complete();
			}
		}
		else {
			this.handler.onTimeout(this.timeoutCallback);
			this.handler.onCompletion(this.completionCallback);
		}
	}

	/**
	 * Invoked after the response is updated with the status code and headers,
	 * if the ResponseBodyEmitter is wrapped in a ResponseEntity, but before the
	 * response is committed, i.e. before the response body has been written to.
	 * <p>The default implementation is empty.
	 */
	protected void extendResponse(ServerHttpResponse outputMessage) {
	}

	/**
	 * Write the given object to the response.
	 * <p>If any exception occurs a dispatch is made back to the app server where
	 * Spring MVC will pass the exception through its exception handling mechanism.
	 * @param object the object to write
	 * @throws IOException raised when an I/O error occurs
	 * @throws java.lang.IllegalStateException wraps any other errors
	 */
	public void send(Object object) throws IOException {
		send(object, null);
	}

	/**
	 * Write the given object to the response also using a MediaType hint.
	 * <p>If any exception occurs a dispatch is made back to the app server where
	 * Spring MVC will pass the exception through its exception handling mechanism.
	 * @param object the object to write
	 * @param mediaType a MediaType hint for selecting an HttpMessageConverter
	 * @throws IOException raised when an I/O error occurs
	 * @throws java.lang.IllegalStateException wraps any other errors
	 */
	public synchronized void send(Object object, MediaType mediaType) throws IOException {
		Assert.state(!this.complete, "ResponseBodyEmitter is already set complete");
		sendInternal(object, mediaType);
	}

	private void sendInternal(Object object, MediaType mediaType) throws IOException {
		if (object != null) {
			if (this.handler != null) {
				try {
					this.handler.send(object, mediaType);
				}
				catch (IOException ex) {
					throw ex;
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Failed to send " + object, ex);
				}
			}
			else {
				this.earlySendAttempts.add(new DataWithMediaType(object, mediaType));
			}
		}
	}

	/**
	 * Complete request processing.
	 * <p>A dispatch is made into the app server where Spring MVC completes
	 * asynchronous request processing.
	 * <p><strong>Note:</strong> you do not need to call this method after an
	 * {@link IOException} from any of the {@code send} methods. The Servlet
	 * container will generate an error notification that Spring MVC will process
	 * and handle through the exception resolver mechanism and then complete.
	 */
	public synchronized void complete() {
		this.complete = true;
		if (this.handler != null) {
			this.handler.complete();
		}
	}

	/**
	 * Complete request processing with an error.
	 * <p>A dispatch is made into the app server where Spring MVC will pass the
	 * exception through its exception handling mechanism.
	 */
	public synchronized void completeWithError(Throwable ex) {
		this.complete = true;
		this.failure = ex;
		if (this.handler != null) {
			this.handler.completeWithError(ex);
		}
	}

	/**
	 * Register code to invoke when the async request times out. This method is
	 * called from a container thread when an async request times out.
	 */
	public synchronized void onTimeout(Runnable callback) {
		this.timeoutCallback.setDelegate(callback);
	}

	/**
	 * Register code to invoke when the async request completes. This method is
	 * called from a container thread when an async request completed for any
	 * reason including timeout and network error. This method is useful for
	 * detecting that a {@code ResponseBodyEmitter} instance is no longer usable.
	 */
	public synchronized void onCompletion(Runnable callback) {
		this.completionCallback.setDelegate(callback);
	}


	/**
	 * Handle sent objects and complete request processing.
	 */
	interface Handler {

		void send(Object data, MediaType mediaType) throws IOException;

		void complete();

		void completeWithError(Throwable failure);

		void onTimeout(Runnable callback);

		void onCompletion(Runnable callback);
	}


	/**
	 * A simple holder of data to be written along with a MediaType hint for
	 * selecting a message converter to write with.
	 */
	public static class DataWithMediaType {

		private final Object data;

		private final MediaType mediaType;

		public DataWithMediaType(Object data, MediaType mediaType) {
			this.data = data;
			this.mediaType = mediaType;
		}

		public Object getData() {
			return this.data;
		}

		public MediaType getMediaType() {
			return this.mediaType;
		}
	}


	private class DefaultCallback implements Runnable {

		private Runnable delegate;

		public void setDelegate(Runnable delegate) {
			this.delegate = delegate;
		}

		@Override
		public void run() {
			ResponseBodyEmitter.this.complete = true;
			if (this.delegate != null) {
				this.delegate.run();
			}
		}
	}

}
