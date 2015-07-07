/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.rx.web.servlet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Arjen Poutsma
 */
class AsyncContextSynchronizer {

	private static final Log logger = LogFactory.getLog(AsyncContextSynchronizer.class);

	private static final int READ_COMPLETE = 1;

	private static final int WRITE_COMPLETE = 1 << 1;

	private static final int COMPLETE = READ_COMPLETE | WRITE_COMPLETE;

	private final AsyncContext asyncContext;

	private final AtomicInteger complete = new AtomicInteger(0);

	public AsyncContextSynchronizer(AsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}

	public ServletInputStream getInputStream() throws IOException {
		return this.asyncContext.getRequest().getInputStream();
	}

	public ServletOutputStream getOutputStream() throws IOException {
		return this.asyncContext.getResponse().getOutputStream();
	}

	public void readComplete() {
		logger.debug("Read complete");
		if (complete.compareAndSet(WRITE_COMPLETE, COMPLETE)) {
			logger.debug("Complete");
			this.asyncContext.complete();
		}
		else {
			this.complete.compareAndSet(0, READ_COMPLETE);
		}
	}

	public void writeComplete() {
		logger.debug("Write complete");
		if (complete.compareAndSet(READ_COMPLETE, COMPLETE)) {
			logger.debug("Complete");
			this.asyncContext.complete();
		}
		else {
			this.complete.compareAndSet(0, WRITE_COMPLETE);
		}
	}
}
