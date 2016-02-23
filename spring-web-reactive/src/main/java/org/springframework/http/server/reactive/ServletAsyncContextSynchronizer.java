/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Utility class for synchronizing between the reading and writing side of an
 * {@link AsyncContext}. This class will simply call {@link AsyncContext#complete()} when
 * both {@link #readComplete()} and {@link #writeComplete()} have been called.
 *
 * @author Arjen Poutsma
 * @see AsyncContext
 */
final class ServletAsyncContextSynchronizer {

	private static final int NONE_COMPLETE = 0;

	private static final int READ_COMPLETE = 1;

	private static final int WRITE_COMPLETE = 1 << 1;

	private static final int COMPLETE = READ_COMPLETE | WRITE_COMPLETE;


	private final AsyncContext asyncContext;

	private final AtomicInteger complete = new AtomicInteger(NONE_COMPLETE);


	/**
	 * Creates a new {@code AsyncContextSynchronizer} based on the given context.
	 * @param asyncContext the context to base this synchronizer on
	 */
	public ServletAsyncContextSynchronizer(AsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}

	public ServletRequest getRequest() {
		return this.asyncContext.getRequest();
	}

	public ServletResponse getResponse() {
		return this.asyncContext.getResponse();
	}

	/**
	 * Returns the input stream of this synchronizer.
	 * @return the input stream
	 * @throws IOException if an input or output exception occurred
	 */
	public ServletInputStream getInputStream() throws IOException {
		return getRequest().getInputStream();
	}

	/**
	 * Returns the output stream of this synchronizer.
	 * @return the output stream
	 * @throws IOException if an input or output exception occurred
	 */
	public ServletOutputStream getOutputStream() throws IOException {
		return getResponse().getOutputStream();
	}

	/**
	 * Completes the reading side of the asynchronous operation. When both this method and
	 * {@link #writeComplete()} have been called, the {@code AsyncContext} will be
	 * {@linkplain AsyncContext#complete() fully completed}.
	 */
	public void readComplete() {
		if (complete.compareAndSet(WRITE_COMPLETE, COMPLETE)) {
			this.asyncContext.complete();
		}
		else {
			this.complete.compareAndSet(NONE_COMPLETE, READ_COMPLETE);
		}
	}

	/**
	 * Completes the writing side of the asynchronous operation. When both this method and
	 * {@link #readComplete()} have been called, the {@code AsyncContext} will be
	 * {@linkplain AsyncContext#complete() fully completed}.
	 */
	public void writeComplete() {
		if (complete.compareAndSet(READ_COMPLETE, COMPLETE)) {
			this.asyncContext.complete();
		}
		else {
			this.complete.compareAndSet(NONE_COMPLETE, WRITE_COMPLETE);
		}
	}

	/**
	 * Completes both the reading and writing side of the asynchronous operation.
	 */
	public void complete() {
		readComplete();
		writeComplete();
	}
}
