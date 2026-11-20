/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.task;

import java.util.concurrent.ThreadFactory;

/**
 * Internal delegate for virtual thread handling on JDK 21.
 * This is the actual version compiled against JDK 21.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see VirtualThreadTaskExecutor
 */
final class VirtualThreadDelegate {

	private final Thread.Builder threadBuilder = Thread.ofVirtual();

	public VirtualThreadDelegate() {
		// Matching constructor in dummy version, avoiding jar verification issues.
	}

	public ThreadFactory virtualThreadFactory() {
		return this.threadBuilder.factory();
	}

	public ThreadFactory virtualThreadFactory(String threadNamePrefix) {
		return this.threadBuilder.name(threadNamePrefix, 0).factory();
	}

	public Thread newVirtualThread(String name, Runnable task) {
		return this.threadBuilder.name(name).unstarted(task);
	}

}
