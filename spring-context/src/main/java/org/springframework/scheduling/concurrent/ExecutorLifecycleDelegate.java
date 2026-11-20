/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

import org.springframework.context.SmartLifecycle;

/**
 * An internal delegate for common {@link ExecutorService} lifecycle management
 * with pause/resume support.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see ExecutorConfigurationSupport
 * @see SimpleAsyncTaskScheduler
 */
final class ExecutorLifecycleDelegate implements SmartLifecycle {

	private final ExecutorService executor;

	private final Lock pauseLock = new ReentrantLock();

	private final Condition unpaused = this.pauseLock.newCondition();

	private volatile boolean paused;

	private volatile boolean shutdown;

	private int executingTaskCount = 0;

	private @Nullable Runnable stopCallback;


	public ExecutorLifecycleDelegate(ExecutorService executor) {
		this.executor = executor;
	}


	@Override
	public void start() {
		this.pauseLock.lock();
		try {
			this.paused = false;
			this.unpaused.signalAll();
		}
		finally {
			this.pauseLock.unlock();
		}
	}

	@Override
	public void stop() {
		this.pauseLock.lock();
		try {
			this.paused = true;
			this.stopCallback = null;
		}
		finally {
			this.pauseLock.unlock();
		}
	}

	@Override
	public void stop(Runnable callback) {
		this.pauseLock.lock();
		try {
			this.paused = true;
			if (this.executingTaskCount == 0) {
				this.stopCallback = null;
				callback.run();
			}
			else {
				this.stopCallback = callback;
			}
		}
		finally {
			this.pauseLock.unlock();
		}
	}

	@Override
	public boolean isRunning() {
		return (!this.paused && !this.executor.isTerminated());
	}

	void markShutdown() {
		this.pauseLock.lock();
		try {
			this.shutdown = true;
			this.unpaused.signalAll();
		}
		finally {
			this.pauseLock.unlock();
		}
	}

	void beforeExecute(Thread thread) {
		this.pauseLock.lock();
		try {
			while (this.paused && !this.shutdown && !this.executor.isShutdown()) {
				this.unpaused.await();
			}
		}
		catch (InterruptedException ex) {
			thread.interrupt();
		}
		finally {
			this.executingTaskCount++;
			this.pauseLock.unlock();
		}
	}

	void afterExecute() {
		this.pauseLock.lock();
		try {
			this.executingTaskCount--;
			if (this.executingTaskCount == 0) {
				Runnable callback = this.stopCallback;
				if (callback != null) {
					callback.run();
					this.stopCallback = null;
				}
			}
		}
		finally {
			this.pauseLock.unlock();
		}
	}

}
