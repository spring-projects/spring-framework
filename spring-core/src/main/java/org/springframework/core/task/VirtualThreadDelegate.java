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
 * Internal delegate for virtual thread handling on JDK 21.
 * This is a dummy version for reachability on JDK <21.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see VirtualThreadTaskExecutor
 */
final class VirtualThreadDelegate {

	public VirtualThreadDelegate() {
		throw new UnsupportedOperationException("Virtual threads not supported on JDK <21");
	}

	public ThreadFactory virtualThreadFactory() {
		throw new UnsupportedOperationException();
	}

	public ThreadFactory virtualThreadFactory(String threadNamePrefix) {
		throw new UnsupportedOperationException();
	}

	public Thread newVirtualThread(String name, Runnable task) {
		throw new UnsupportedOperationException();
	}

}
