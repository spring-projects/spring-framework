/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.task;

import java.util.concurrent.ThreadFactory;

/**
 * A {@link TaskExecutor} implementation based on virtual threads in JDK 21+.
 * The only configuration option is a thread name prefix.
 *
 * <p>For additional features such as concurrency limiting or task decoration,
 * consider using {@link SimpleAsyncTaskExecutor#setVirtualThreads} instead.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see SimpleAsyncTaskExecutor#setVirtualThreads
 */
public class VirtualThreadTaskExecutor implements AsyncTaskExecutor {

	private final ThreadFactory virtualThreadFactory;


	/**
	 * Create a new {@code VirtualThreadTaskExecutor} without thread naming.
	 */
	public VirtualThreadTaskExecutor() {
		this.virtualThreadFactory = new VirtualThreadDelegate().virtualThreadFactory();
	}

	/**
	 * Create a new {@code VirtualThreadTaskExecutor} with thread names based
	 * on the given thread name prefix followed by a counter (e.g. "test-0").
	 * @param threadNamePrefix the prefix for thread names (e.g. "test-")
	 */
	public VirtualThreadTaskExecutor(String threadNamePrefix) {
		this.virtualThreadFactory = new VirtualThreadDelegate().virtualThreadFactory(threadNamePrefix);
	}


	/**
	 * Return the underlying virtual {@link ThreadFactory}.
	 * Can also be used for custom thread creation elsewhere.
	 */
	public final ThreadFactory getVirtualThreadFactory() {
		return this.virtualThreadFactory;
	}

	@Override
	public void execute(Runnable task) {
		this.virtualThreadFactory.newThread(task).start();
	}

}
