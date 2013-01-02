/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scheduling.commonj;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;

import javax.naming.NamingException;

import commonj.work.Work;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkListener;
import commonj.work.WorkManager;
import commonj.work.WorkRejectedException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;

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
 * <p><b>Note: At the time of this writing, the CommonJ WorkManager facility
 * is only supported on IBM WebSphere 6.0+ and BEA WebLogic 9.0+,
 * despite being such a crucial API for an application server.</b>
 * (There is a similar facility available on WebSphere 5.1 Enterprise,
 * though, which we will discuss below.)
 *
 * <p><b>On JBoss and GlassFish, a similar facility is available through
 * the JCA WorkManager.</b> See the
 * {@link org.springframework.jca.work.jboss.JBossWorkManagerTaskExecutor}
 * {@link org.springframework.jca.work.glassfish.GlassFishWorkManagerTaskExecutor}
 * classes which are the direct equivalent of this CommonJ adapter class.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class WorkManagerTaskExecutor extends JndiLocatorSupport
		implements SchedulingTaskExecutor, WorkManager, InitializingBean {

	private WorkManager workManager;

	private String workManagerName;

	private WorkListener workListener;


	/**
	 * Specify the CommonJ WorkManager to delegate to.
	 * <p>Alternatively, you can also specify the JNDI name
	 * of the target WorkManager.
	 * @see #setWorkManagerName
	 */
	public void setWorkManager(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * Set the JNDI name of the CommonJ WorkManager.
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
	 * Specify a CommonJ WorkListener to apply, if any.
	 * <p>This shared WorkListener instance will be passed on to the
	 * WorkManager by all {@link #execute} calls on this TaskExecutor.
	 */
	public void setWorkListener(WorkListener workListener) {
		this.workListener = workListener;
	}

	public void afterPropertiesSet() throws NamingException {
		if (this.workManager == null) {
			if (this.workManagerName == null) {
				throw new IllegalArgumentException("Either 'workManager' or 'workManagerName' must be specified");
			}
			this.workManager = lookup(this.workManagerName, WorkManager.class);
		}
	}


	//-------------------------------------------------------------------------
	// Implementation of the Spring SchedulingTaskExecutor interface
	//-------------------------------------------------------------------------

	public void execute(Runnable task) {
		Assert.state(this.workManager != null, "No WorkManager specified");
		Work work = new DelegatingWork(task);
		try {
			if (this.workListener != null) {
				this.workManager.schedule(work, this.workListener);
			}
			else {
				this.workManager.schedule(work);
			}
		}
		catch (WorkRejectedException ex) {
			throw new TaskRejectedException("CommonJ WorkManager did not accept task: " + task, ex);
		}
		catch (WorkException ex) {
			throw new SchedulingException("Could not schedule task on CommonJ WorkManager", ex);
		}
	}

	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<Object>(task, null);
		execute(future);
		return future;
	}

	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<T>(task);
		execute(future);
		return future;
	}

	/**
	 * This task executor prefers short-lived work units.
	 */
	public boolean prefersShortLivedTasks() {
		return true;
	}


	//-------------------------------------------------------------------------
	// Implementation of the CommonJ WorkManager interface
	//-------------------------------------------------------------------------

	public WorkItem schedule(Work work)
			throws WorkException, IllegalArgumentException {

		return this.workManager.schedule(work);
	}

	public WorkItem schedule(Work work, WorkListener workListener)
			throws WorkException, IllegalArgumentException {

		return this.workManager.schedule(work, workListener);
	}

	public boolean waitForAll(Collection workItems, long timeout)
			throws InterruptedException, IllegalArgumentException {

		return this.workManager.waitForAll(workItems, timeout);
	}

	public Collection waitForAny(Collection workItems, long timeout)
			throws InterruptedException, IllegalArgumentException {

		return this.workManager.waitForAny(workItems, timeout);
	}

}
