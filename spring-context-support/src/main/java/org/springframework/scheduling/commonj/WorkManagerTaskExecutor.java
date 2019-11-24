/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.scheduling.commonj;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.naming.NamingException;

import commonj.work.Work;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkListener;
import commonj.work.WorkManager;
import commonj.work.WorkRejectedException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * TaskExecutor implementation that delegates to a CommonJ WorkManager,
 * implementing the {@link commonj.work.WorkManager} interface,
 * which either needs to be specified as reference or through the JNDI name.
 *
 * <p><b>This is the central convenience class for setting up a
 * CommonJ WorkManager in a Spring context.</b>
 *
 * <p>Also implements the CommonJ WorkManager interface itself, delegating all
 * calls to the target WorkManager. Hence, a caller can choose whether it wants
 * to talk to this executor through the Spring TaskExecutor interface or the
 * CommonJ WorkManager interface.
 *
 * <p>The CommonJ WorkManager will usually be retrieved from the application
 * server's JNDI environment, as defined in the server's management console.
 *
 * <p>Note: On the upcoming EE 7 compliant versions of WebLogic and WebSphere, a
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor}
 * should be preferred, following JSR-236 support in Java EE 7.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of 5.1, in favor of EE 7's
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor}
 */
@Deprecated
public class WorkManagerTaskExecutor extends JndiLocatorSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, WorkManager, InitializingBean {

	@Nullable
	private WorkManager workManager;

	@Nullable
	private String workManagerName;

	@Nullable
	private WorkListener workListener;

	@Nullable
	private TaskDecorator taskDecorator;


	/**
	 * Specify the CommonJ WorkManager to delegate to.
	 * <p>Alternatively, you can also specify the JNDI name of the target WorkManager.
	 * @see #setWorkManagerName
	 */
	public void setWorkManager(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * Set the JNDI name of the CommonJ WorkManager.
	 * <p>This can either be a fully qualified JNDI name, or the JNDI name relative
	 * to the current environment naming context if "resourceRef" is set to "true".
	 * @see #setWorkManager
	 * @see #setResourceRef
	 */
	public void setWorkManagerName(String workManagerName) {
		this.workManagerName = workManagerName;
	}

	/**
	 * Specify a CommonJ WorkListener to apply, if any.
	 * <p>This shared WorkListener instance will be passed on to the
	 * WorkManager by all {@link #execute} calls on this TaskExecutor.
	 */
	public void setWorkListener(WorkListener workListener) {
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
			if (this.workManagerName == null) {
				throw new IllegalArgumentException("Either 'workManager' or 'workManagerName' must be specified");
			}
			this.workManager = lookup(this.workManagerName, WorkManager.class);
		}
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
		Work work = new DelegatingWork(this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		try {
			if (this.workListener != null) {
				obtainWorkManager().schedule(work, this.workListener);
			}
			else {
				obtainWorkManager().schedule(work);
			}
		}
		catch (WorkRejectedException ex) {
			throw new TaskRejectedException("CommonJ WorkManager did not accept task: " + task, ex);
		}
		catch (WorkException ex) {
			throw new SchedulingException("Could not schedule task on CommonJ WorkManager", ex);
		}
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future);
		return future;
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
		execute(future);
		return future;
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
		execute(future);
		return future;
	}


	//-------------------------------------------------------------------------
	// Implementation of the CommonJ WorkManager interface
	//-------------------------------------------------------------------------

	@Override
	public WorkItem schedule(Work work) throws WorkException, IllegalArgumentException {
		return obtainWorkManager().schedule(work);
	}

	@Override
	public WorkItem schedule(Work work, WorkListener workListener) throws WorkException {
		return obtainWorkManager().schedule(work, workListener);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean waitForAll(Collection workItems, long timeout) throws InterruptedException {
		return obtainWorkManager().waitForAll(workItems, timeout);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Collection waitForAny(Collection workItems, long timeout) throws InterruptedException {
		return obtainWorkManager().waitForAny(workItems, timeout);
	}

}
