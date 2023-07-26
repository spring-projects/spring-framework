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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;

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

	private final ReentrantLock pauseLock = new ReentrantLock();

	private final Condition unpaused = this.pauseLock.newCondition();

	private volatile boolean paused;

	private int executingTaskCount = 0;

	@Nullable
	private Runnable stopCallback;


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
		return (!this.executor.isShutdown() & !this.paused);
	}

	void beforeExecute(Thread thread) {
		this.pauseLock.lock();
		try {
			while (this.paused && !this.executor.isShutdown()) {
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
