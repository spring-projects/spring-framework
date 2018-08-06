/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jca.work;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.naming.NamingException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.core.task.TaskTimeoutException;
import org.springframework.jca.context.BootstrapContextAware;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * {@link org.springframework.core.task.TaskExecutor} implementation
 * that delegates to a JCA 1.7 WorkManager, implementing the
 * {@link javax.resource.spi.work.WorkManager} interface.
 *
 * <p>This is mainly intended for use within a JCA ResourceAdapter implementation,
 * but may also be used in a standalone environment, delegating to a locally
 * embedded WorkManager implementation (such as Geronimo's).
 *
 * <p>Also implements the JCA 1.7 WorkManager interface itself, delegating all
 * calls to the target WorkManager. Hence, a caller can choose whether it wants
 * to talk to this executor through the Spring TaskExecutor interface or the
 * WorkManager interface.
 *
 * <p>This adapter is also capable of obtaining a JCA WorkManager from JNDI.
 * This is for example appropriate on the Geronimo application server, where
 * WorkManager GBeans (e.g. Geronimo's default "DefaultWorkManager" GBean)
 * can be linked into the Java EE environment through "gbean-ref" entries
 * in the {@code geronimo-web.xml} deployment descriptor.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setWorkManager
 * @see javax.resource.spi.work.WorkManager#scheduleWork
 */
public class WorkManagerTaskExecutor extends JndiLocatorSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, WorkManager, BootstrapContextAware, InitializingBean {

	@Nullable
	private WorkManager workManager;

	@Nullable
	private String workManagerName;

	private boolean blockUntilStarted = false;

	private boolean blockUntilCompleted = false;

	@Nullable
	private WorkListener workListener;

	@Nullable
	private TaskDecorator taskDecorator;


	/**
	 * Create a new WorkManagerTaskExecutor, expecting bean-style configuration.
	 * @see #setWorkManager
	 */
	public WorkManagerTaskExecutor() {
	}

	/**
	 * Create a new WorkManagerTaskExecutor for the given WorkManager.
	 * @param workManager the JCA WorkManager to delegate to
	 */
	public WorkManagerTaskExecutor(WorkManager workManager) {
		setWorkManager(workManager);
	}


	/**
	 * Specify the JCA WorkManager instance to delegate to.
	 */
	public void setWorkManager(WorkManager workManager) {
		Assert.notNull(workManager, "WorkManager must not be null");
		this.workManager = workManager;
	}

	/**
	 * Set the JNDI name of the JCA WorkManager.
	 * <p>This can either be a fully qualified JNDI name,
	 * or the JNDI name relative to the current environment
	 * naming context if "resourceRef" is set to "true".
	 * @see #setWorkManager
	 * @see #setResourceRef
	 */
	public void setWorkManagerName(String workManagerName) {
		this.workManagerName = workManagerName;
	}

	/**
	 * Specify the JCA BootstrapContext that contains the
	 * WorkManager to delegate to.
	 */
	@Override
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		Assert.notNull(bootstrapContext, "BootstrapContext must not be null");
		this.workManager = bootstrapContext.getWorkManager();
	}

	/**
	 * Set whether to let {@link #execute} block until the work
	 * has been actually started.
	 * <p>Uses the JCA {@code startWork} operation underneath,
	 * instead of the default {@code scheduleWork}.
	 * @see javax.resource.spi.work.WorkManager#startWork
	 * @see javax.resource.spi.work.WorkManager#scheduleWork
	 */
	public void setBlockUntilStarted(boolean blockUntilStarted) {
		this.blockUntilStarted = blockUntilStarted;
	}

	/**
	 * Set whether to let {@link #execute} block until the work
	 * has been completed.
	 * <p>Uses the JCA {@code doWork} operation underneath,
	 * instead of the default {@code scheduleWork}.
	 * @see javax.resource.spi.work.WorkManager#doWork
	 * @see javax.resource.spi.work.WorkManager#scheduleWork
	 */
	public void setBlockUntilCompleted(boolean blockUntilCompleted) {
		this.blockUntilCompleted = blockUntilCompleted;
	}

	/**
	 * Specify a JCA WorkListener to apply, if any.
	 * <p>This shared WorkListener instance will be passed on to the
	 * WorkManager by all {@link #execute} calls on this TaskExecutor.
	 */
	public void setWorkListener(@Nullable WorkListener workListener) {
		this.workListener = workListener;
	}

	/**
	 * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
	 * about to be executed.
	 * <p>Note that such a decorator is not necessarily being applied to the
	 * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
	 * execution callback (which may be a wrapper around the user-supplied task).
	 * <p>The primary use case is to set some execution context around the task's
	 * invocation, or to provide some monitoring/statistics for task execution.
	 * @since 4.3
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.workManager == null) {
			if (this.workManagerName != null) {
				this.workManager = lookup(this.workManagerName, WorkManager.class);
			}
			else {
				this.workManager = getDefaultWorkManager();
			}
		}
	}

	/**
	 * Obtain a default WorkManager to delegate to.
	 * Called if no explicit WorkManager or WorkManager JNDI name has been specified.
	 * <p>The default implementation returns a {@link SimpleTaskWorkManager}.
	 * Can be overridden in subclasses.
	 */
	protected WorkManager getDefaultWorkManager() {
		return new SimpleTaskWorkManager();
	}

	private WorkManager obtainWorkManager() {
		Assert.state(this.workManager != null, "No WorkManager specified");
		return this.workManager;
	}


	//-------------------------------------------------------------------------
	// Implementation of the Spring SchedulingTaskExecutor interface
	//-------------------------------------------------------------------------

	@Override
	public void execute(Runnable task) {
		execute(task, TIMEOUT_INDEFINITE);
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		Work work = new DelegatingWork(this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		try {
			if (this.blockUntilCompleted) {
				if (startTimeout != TIMEOUT_INDEFINITE || this.workListener != null) {
					obtainWorkManager().doWork(work, startTimeout, null, this.workListener);
				}
				else {
					obtainWorkManager().doWork(work);
				}
			}
			else if (this.blockUntilStarted) {
				if (startTimeout != TIMEOUT_INDEFINITE || this.workListener != null) {
					obtainWorkManager().startWork(work, startTimeout, null, this.workListener);
				}
				else {
					obtainWorkManager().startWork(work);
				}
			}
			else {
				if (startTimeout != TIMEOUT_INDEFINITE || this.workListener != null) {
					obtainWorkManager().scheduleWork(work, startTimeout, null, this.workListener);
				}
				else {
					obtainWorkManager().scheduleWork(work);
				}
			}
		}
		catch (WorkRejectedException ex) {
			if (WorkException.START_TIMED_OUT.equals(ex.getErrorCode())) {
				throw new TaskTimeoutException("JCA WorkManager rejected task because of timeout: " + task, ex);
			}
			else {
				throw new TaskRejectedException("JCA WorkManager rejected task: " + task, ex);
			}
		}
		catch (WorkException ex) {
			throw new SchedulingException("Could not schedule task on JCA WorkManager", ex);
		}
	}

	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}


	//-------------------------------------------------------------------------
	// Implementation of the JCA WorkManager interface
	//-------------------------------------------------------------------------

	@Override
	public void doWork(Work work) throws WorkException {
		obtainWorkManager().doWork(work);
	}

	@Override
	public void doWork(Work work, long delay, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		obtainWorkManager().doWork(work, delay, executionContext, workListener);
	}

	@Override
	public long startWork(Work work) throws WorkException {
		return obtainWorkManager().startWork(work);
	}

	@Override
	public long startWork(Work work, long delay, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		return obtainWorkManager().startWork(work, delay, executionContext, workListener);
	}

	@Override
	public void scheduleWork(Work work) throws WorkException {
		obtainWorkManager().scheduleWork(work);
	}

	@Override
	public void scheduleWork(Work work, long delay, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		obtainWorkManager().scheduleWork(work, delay, executionContext, workListener);
	}

}
