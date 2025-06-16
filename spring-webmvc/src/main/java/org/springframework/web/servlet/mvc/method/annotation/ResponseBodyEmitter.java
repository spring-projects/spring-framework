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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

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
 * @author Brian Clozel
 * @since 4.2
 */
public class ResponseBodyEmitter {

	private final @Nullable Long timeout;

	private @Nullable Handler handler;

	/** Store send data before handler is initialized. */
	private final Set<DataWithMediaType> earlySendAttempts = new LinkedHashSet<>(8);

	/** Store successful completion before the handler is initialized. */
	private boolean complete;

	/** Store an error before the handler is initialized. */
	private @Nullable Throwable failure;

	private final DefaultCallback timeoutCallback = new DefaultCallback();

	private final ErrorCallback errorCallback = new ErrorCallback();

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
	 * @param timeout the timeout value in milliseconds
	 */
	public ResponseBodyEmitter(Long timeout) {
		this.timeout = timeout;
	}


	/**
	 * Return the configured timeout value, if any.
	 */
	public @Nullable Long getTimeout() {
		return this.timeout;
	}


	synchronized void initialize(Handler handler) throws IOException {
		this.handler = handler;

		try {
			sendInternal(this.earlySendAttempts);
		}
		finally {
			this.earlySendAttempts.clear();
		}

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
			this.handler.onError(this.errorCallback);
			this.handler.onCompletion(this.completionCallback);
		}
	}

	synchronized void initializeWithError(Throwable ex) {
		this.complete = true;
		this.failure = ex;
		this.earlySendAttempts.clear();
		this.errorCallback.accept(ex);
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
	 * <p><strong>Note:</strong> if the send fails with an IOException, you do
	 * not need to call {@link #completeWithError(Throwable)} in order to clean
	 * up. Instead the Servlet container creates a notification that results in a
	 * dispatch where Spring MVC invokes exception resolvers and completes
	 * processing.
	 * @param object the object to write
	 * @throws IOException raised when an I/O error occurs
	 * @throws java.lang.IllegalStateException wraps any other errors
	 */
	public void send(Object object) throws IOException {
		send(object, null);
	}

	/**
	 * Overloaded variant of {@link #send(Object)} that also accepts a MediaType
	 * hint for how to serialize the given Object.
	 * @param object the object to write
	 * @param mediaType a MediaType hint for selecting an HttpMessageConverter
	 * @throws IOException raised when an I/O error occurs
	 * @throws java.lang.IllegalStateException wraps any other errors
	 */
	public synchronized void send(Object object, @Nullable MediaType mediaType) throws IOException {
		Assert.state(!this.complete, () -> "ResponseBodyEmitter has already completed" +
				(this.failure != null ? " with error: " + this.failure : ""));
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

	/**
	 * Write a set of data and MediaType pairs in a batch.
	 * <p>Compared to {@link #send(Object, MediaType)}, this batches the write operations
	 * and flushes to the network at the end.
	 * @param items the object and media type pairs to write
	 * @throws IOException raised when an I/O error occurs
	 * @throws java.lang.IllegalStateException wraps any other errors
	 * @since 6.0.12
	 */
	public synchronized void send(Set<DataWithMediaType> items) throws IOException {
		Assert.state(!this.complete, () -> "ResponseBodyEmitter has already completed" +
				(this.failure != null ? " with error: " + this.failure : ""));
		sendInternal(items);
	}

	private void sendInternal(Set<DataWithMediaType> items) throws IOException {
		if (items.isEmpty()) {
			return;
		}
		if (this.handler != null) {
			try {
				this.handler.send(items);
			}
			catch (IOException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to send " + items, ex);
			}
		}
		else {
			this.earlySendAttempts.addAll(items);
		}
	}

	/**
	 * Complete request processing by performing a dispatch into the servlet
	 * container, where Spring MVC is invoked once more, and completes the
	 * request processing lifecycle.
	 * <p><strong>Note:</strong> this method should be called by the application
	 * to complete request processing. It should not be used after container
	 * related events such as an error while {@link #send(Object) sending}.
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
	 * exception through its exception handling mechanism. Note however that
	 * at this stage of request processing, the response is committed and the
	 * response status can no longer be changed.
	 * <p><strong>Note:</strong> this method should be called by the application
	 * to complete request processing with an error. It should not be used after
	 * container related events such as an error while
	 * {@link #send(Object) sending}.
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
	 * <p>As of 6.2, one can register multiple callbacks for this event.
	 */
	public synchronized void onTimeout(Runnable callback) {
		this.timeoutCallback.addDelegate(callback);
	}

	/**
	 * Register code to invoke for an error during async request processing.
	 * This method is called from a container thread when an error occurred
	 * while processing an async request.
	 * <p>As of 6.2, one can register multiple callbacks for this event.
	 * @since 5.0
	 */
	public synchronized void onError(Consumer<Throwable> callback) {
		this.errorCallback.addDelegate(callback);
	}

	/**
	 * Register code to invoke when the async request completes. This method is
	 * called from a container thread when an async request completed for any
	 * reason including timeout and network error. This method is useful for
	 * detecting that a {@code ResponseBodyEmitter} instance is no longer usable.
	 * <p>As of 6.2, one can register multiple callbacks for this event.
	 */
	public synchronized void onCompletion(Runnable callback) {
		this.completionCallback.addDelegate(callback);
	}


	@Override
	public String toString() {
		return "ResponseBodyEmitter@" + ObjectUtils.getIdentityHexString(this);
	}


	/**
	 * Contract to handle the sending of event data, the completion of event
	 * sending, and the registration of callbacks to be invoked in case of
	 * timeout, error, and completion for any reason (including from the
	 * container side).
	 */
	interface Handler {

		/**
		 * Immediately write and flush the given data to the network.
		 */
		void send(Object data, @Nullable MediaType mediaType) throws IOException;

		/**
		 * Immediately write all data items then flush to the network.
		 * @since 6.0.12
		 */
		void send(Set<DataWithMediaType> items) throws IOException;

		void complete();

		void completeWithError(Throwable failure);

		void onTimeout(Runnable callback);

		void onError(Consumer<Throwable> callback);

		void onCompletion(Runnable callback);
	}


	/**
	 * A simple holder of data to be written along with a MediaType hint for
	 * selecting a message converter to write with.
	 */
	public static class DataWithMediaType {

		private final Object data;

		private final @Nullable MediaType mediaType;

		public DataWithMediaType(Object data, @Nullable MediaType mediaType) {
			this.data = data;
			this.mediaType = mediaType;
		}

		public Object getData() {
			return this.data;
		}

		public @Nullable MediaType getMediaType() {
			return this.mediaType;
		}
	}


	private class DefaultCallback implements Runnable {

		private List<Runnable> delegates = new ArrayList<>(1);

		public void addDelegate(Runnable delegate) {
			this.delegates.add(delegate);
		}

		@Override
		public void run() {
			ResponseBodyEmitter.this.complete = true;
			for (Runnable delegate : this.delegates) {
				delegate.run();
			}
		}
	}


	private class ErrorCallback implements Consumer<Throwable> {

		private List<Consumer<Throwable>> delegates = new ArrayList<>(1);

		public void addDelegate(Consumer<Throwable> callback) {
			this.delegates.add(callback);
		}

		@Override
		public void accept(Throwable t) {
			ResponseBodyEmitter.this.complete = true;
			for(Consumer<Throwable> delegate : this.delegates) {
				delegate.accept(t);
			}
		}
	}

}
