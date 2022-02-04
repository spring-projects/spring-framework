/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jca.work;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkAdapter;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple JCA 1.7 {@link javax.resource.spi.work.WorkManager} implementation that
 * delegates to a Spring {@link org.springframework.core.task.TaskExecutor}.
 * Provides simple task execution including start timeouts, but without support
 * for a JCA ExecutionContext (i.e. without support for imported transactions).
 *
 * <p>Uses a {@link org.springframework.core.task.SyncTaskExecutor} for {@link #doWork}
 * calls and a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
 * for {@link #startWork} and {@link #scheduleWork} calls, by default.
 * These default task executors can be overridden through configuration.
 *
 * <p><b>NOTE: This WorkManager does not provide thread pooling by default!</b>
 * Specify a {@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor}
 * (or any other thread-pooling TaskExecutor) as "asyncTaskExecutor" in order to
 * achieve actual thread pooling.
 *
 * <p>This WorkManager automatically detects a specified
 * {@link org.springframework.core.task.AsyncTaskExecutor} implementation
 * and uses its extended timeout functionality where appropriate.
 * JCA WorkListeners are fully supported in any case.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setSyncTaskExecutor
 * @see #setAsyncTaskExecutor
 */
public class SimpleTaskWorkManager implements WorkManager {

	@Nullable
	private TaskExecutor syncTaskExecutor = new SyncTaskExecutor();

	@Nullable
	private AsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();


	/**
	 * Specify the TaskExecutor to use for <i>synchronous</i> work execution
	 * (i.e. {@link #doWork} calls).
	 * <p>Default is a {@link org.springframework.core.task.SyncTaskExecutor}.
	 */
	public void setSyncTaskExecutor(TaskExecutor syncTaskExecutor) {
		this.syncTaskExecutor = syncTaskExecutor;
	}

	/**
	 * Specify the TaskExecutor to use for <i>asynchronous</i> work execution
	 * (i.e. {@link #startWork} and {@link #scheduleWork} calls).
	 * <p>This will typically (but not necessarily) be an
	 * {@link org.springframework.core.task.AsyncTaskExecutor} implementation.
	 * Default is a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}.
	 */
	public void setAsyncTaskExecutor(AsyncTaskExecutor asyncTaskExecutor) {
		this.asyncTaskExecutor = asyncTaskExecutor;
	}


	@Override
	public void doWork(Work work) throws WorkException {
		doWork(work, WorkManager.INDEFINITE, null, null);
	}

	@Override
	public void doWork(Work work, long startTimeout, @Nullable ExecutionContext executionContext, @Nullable WorkListener workListener)
			throws WorkException {

		Assert.state(this.syncTaskExecutor != null, "No 'syncTaskExecutor' set");
		executeWork(this.syncTaskExecutor, work, startTimeout, false, executionContext, workListener);
	}

	@Override
	public long startWork(Work work) throws WorkException {
		return startWork(work, WorkManager.INDEFINITE, null, null);
	}

	@Override
	public long startWork(Work work, long startTimeout, @Nullable ExecutionContext executionContext, @Nullable WorkListener workListener)
			throws WorkException {

		Assert.state(this.asyncTaskExecutor != null, "No 'asyncTaskExecutor' set");
		return executeWork(this.asyncTaskExecutor, work, startTimeout, true, executionContext, workListener);
	}

	@Override
	public void scheduleWork(Work work) throws WorkException {
		scheduleWork(work, WorkManager.INDEFINITE, null, null);
	}

	@Override
	public void scheduleWork(Work work, long startTimeout, @Nullable ExecutionContext executionContext, @Nullable WorkListener workListener)
			throws WorkException {

		Assert.state(this.asyncTaskExecutor != null, "No 'asyncTaskExecutor' set");
		executeWork(this.asyncTaskExecutor, work, startTimeout, false, executionContext, workListener);
	}


	/**
	 * Execute the given Work on the specified TaskExecutor.
	 * @param taskExecutor the TaskExecutor to use
	 * @param work the Work to execute
	 * @param startTimeout the time duration within which the Work is supposed to start
	 * @param blockUntilStarted whether to block until the Work has started
	 * @param executionContext the JCA ExecutionContext for the given Work
	 * @param workListener the WorkListener to clal for the given Work
	 * @return the time elapsed from Work acceptance until start of execution
	 * (or -1 if not applicable or not known)
	 * @throws WorkException if the TaskExecutor did not accept the Work
	 */
	@SuppressWarnings("deprecation")
	protected long executeWork(TaskExecutor taskExecutor, Work work, long startTimeout, boolean blockUntilStarted,
			@Nullable ExecutionContext executionContext, @Nullable WorkListener workListener) throws WorkException {

		if (executionContext != null && executionContext.getXid() != null) {
			throw new WorkException("SimpleTaskWorkManager does not supported imported XIDs: " + executionContext.getXid());
		}
		WorkListener workListenerToUse = workListener;
		if (workListenerToUse == null) {
			workListenerToUse = new WorkAdapter();
		}

		boolean isAsync = (taskExecutor instanceof AsyncTaskExecutor);
		DelegatingWorkAdapter workHandle = new DelegatingWorkAdapter(work, workListenerToUse, !isAsync);
		try {
			if (isAsync) {
				((AsyncTaskExecutor) taskExecutor).execute(workHandle, startTimeout);
			}
			else {
				taskExecutor.execute(workHandle);
			}
		}
		catch (org.springframework.core.task.TaskTimeoutException ex) {
			WorkException wex = new WorkRejectedException("TaskExecutor rejected Work because of timeout: " + work, ex);
			wex.setErrorCode(WorkException.START_TIMED_OUT);
			workListenerToUse.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, work, wex));
			throw wex;
		}
		catch (TaskRejectedException ex) {
			WorkException wex = new WorkRejectedException("TaskExecutor rejected Work: " + work, ex);
			wex.setErrorCode(WorkException.INTERNAL);
			workListenerToUse.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, work, wex));
			throw wex;
		}
		catch (Throwable ex) {
			WorkException wex = new WorkException("TaskExecutor failed to execute Work: " + work, ex);
			wex.setErrorCode(WorkException.INTERNAL);
			throw wex;
		}
		if (isAsync) {
			workListenerToUse.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
		}

		if (blockUntilStarted) {
			long acceptanceTime = System.currentTimeMillis();
			synchronized (workHandle.monitor) {
				try {
					while (!workHandle.started) {
						workHandle.monitor.wait();
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			return (System.currentTimeMillis() - acceptanceTime);
		}
		else {
			return WorkManager.UNKNOWN;
		}
	}


	/**
	 * Work adapter that supports start timeouts and WorkListener callbacks
	 * for a given Work that it delegates to.
	 */
	private static class DelegatingWorkAdapter implements Work {

		private final Work work;

		private final WorkListener workListener;

		private final boolean acceptOnExecution;

		public final Object monitor = new Object();

		public boolean started = false;

		public DelegatingWorkAdapter(Work work, WorkListener workListener, boolean acceptOnExecution) {
			this.work = work;
			this.workListener = workListener;
			this.acceptOnExecution = acceptOnExecution;
		}

		@Override
		public void run() {
			if (this.acceptOnExecution) {
				this.workListener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, this.work, null));
			}
			synchronized (this.monitor) {
				this.started = true;
				this.monitor.notify();
			}
			this.workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, this.work, null));
			try {
				this.work.run();
			}
			catch (RuntimeException | Error ex) {
				this.workListener.workCompleted(
						new WorkEvent(this, WorkEvent.WORK_COMPLETED, this.work, new WorkCompletedException(ex)));
				throw ex;
			}
			this.workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, this.work, null));
		}

		@Override
		public void release() {
			this.work.release();
		}
	}

}
