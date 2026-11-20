/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
