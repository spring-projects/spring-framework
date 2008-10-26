/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NamingException;

import commonj.timers.Timer;
import commonj.timers.TimerManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.jndi.JndiLocatorSupport;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that retrieves a
 * CommonJ {@link commonj.timers.TimerManager} and exposes it for bean references.
 *
 * <p><b>This is the central convenience class for setting up a
 * CommonJ TimerManager in a Spring context.</b>
 *
 * <p>Allows for registration of ScheduledTimerListeners. This is the main
 * purpose of this class; the TimerManager itself could also be fetched
 * from JNDI via {@link org.springframework.jndi.JndiObjectFactoryBean}.
 * In scenarios that just require static registration of tasks at startup,
 * there is no need to access the TimerManager itself in application code.
 *
 * <p>Note that the TimerManager uses a TimerListener instance that is
 * shared between repeated executions, in contrast to Quartz which
 * instantiates a new Job for each execution.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ScheduledTimerListener
 * @see commonj.timers.TimerManager
 * @see commonj.timers.TimerListener
 */
public class TimerManagerFactoryBean extends JndiLocatorSupport
		implements FactoryBean, InitializingBean, DisposableBean, Lifecycle {

	private TimerManager timerManager;

	private String timerManagerName;

	private boolean shared = false;

	private ScheduledTimerListener[] scheduledTimerListeners;

	private final List timers = new LinkedList();


	/**
	 * Specify the CommonJ TimerManager to delegate to.
	 * <p>Note that the given TimerManager's lifecycle will be managed
	 * by this FactoryBean.
	 * <p>Alternatively (and typically), you can specify the JNDI name
	 * of the target TimerManager.
	 * @see #setTimerManagerName
	 */
	public void setTimerManager(TimerManager timerManager) {
		this.timerManager = timerManager;
	}

	/**
	 * Set the JNDI name of the CommonJ TimerManager.
	 * <p>This can either be a fully qualified JNDI name, or the JNDI name relative
	 * to the current environment naming context if "resourceRef" is set to "true".
	 * @see #setTimerManager
	 * @see #setResourceRef
	 */
	public void setTimerManagerName(String timerManagerName) {
		this.timerManagerName = timerManagerName;
	}

	/**
	 * Specify whether the TimerManager obtained by this FactoryBean
	 * is a shared instance ("true") or an independent instance ("false").
	 * The lifecycle of the former is supposed to be managed by the application
	 * server, while the lifecycle of the latter is up to the application.
	 * <p>Default is "false", i.e. managing an independent TimerManager instance.
	 * This is what the CommonJ specification suggests that application servers
	 * are supposed to offer via JNDI lookups, typically declared as a
	 * <code>resource-ref</code> of type <code>commonj.timers.TimerManager</code>
	 * in <code>web.xml<code>, with <code>res-sharing-scope</code> set to 'Unshareable'.
	 * <p>Switch this flag to "true" if you are obtaining a shared TimerManager,
	 * typically through specifying the JNDI location of a TimerManager that
	 * has been explicitly declared as 'Shareable'. Note that WebLogic's
	 * cluster-aware Job Scheduler is a shared TimerManager too.
	 * <p>The sole difference between this FactoryBean being in shared or
	 * non-shared mode is that it will only attempt to suspend / resume / stop
	 * the underlying TimerManager in case of an independent (non-shared) instance.
	 * This only affects the {@link org.springframework.context.Lifecycle} support
	 * as well as application context shutdown.
	 * @see #stop()
	 * @see #start()
	 * @see #destroy()
	 * @see commonj.timers.TimerManager
	 */
	public void setShared(boolean shared) {
		this.shared = shared;
	}

	/**
	 * Register a list of ScheduledTimerListener objects with the TimerManager
	 * that this FactoryBean creates. Depending on each ScheduledTimerListener's settings,
	 * it will be registered via one of TimerManager's schedule methods.
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long)
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long, long)
	 * @see commonj.timers.TimerManager#scheduleAtFixedRate(commonj.timers.TimerListener, long, long)
	 */
	public void setScheduledTimerListeners(ScheduledTimerListener[] scheduledTimerListeners) {
		this.scheduledTimerListeners = scheduledTimerListeners;
	}


	//---------------------------------------------------------------------
	// Implementation of InitializingBean interface
	//---------------------------------------------------------------------

	public void afterPropertiesSet() throws NamingException {
		if (this.timerManager == null) {
			if (this.timerManagerName == null) {
				throw new IllegalArgumentException("Either 'timerManager' or 'timerManagerName' must be specified");
			}
			this.timerManager = (TimerManager) lookup(this.timerManagerName, TimerManager.class);
		}

		if (this.scheduledTimerListeners != null) {
			for (int i = 0; i < this.scheduledTimerListeners.length; i++) {
				ScheduledTimerListener scheduledTask = this.scheduledTimerListeners[i];
				Timer timer = null;
				if (scheduledTask.isOneTimeTask()) {
					timer = this.timerManager.schedule(scheduledTask.getTimerListener(), scheduledTask.getDelay());
				}
				else {
					if (scheduledTask.isFixedRate()) {
						timer = this.timerManager.scheduleAtFixedRate(
								scheduledTask.getTimerListener(), scheduledTask.getDelay(), scheduledTask.getPeriod());
					}
					else {
						timer = this.timerManager.schedule(
								scheduledTask.getTimerListener(), scheduledTask.getDelay(), scheduledTask.getPeriod());
					}
				}
				this.timers.add(timer);
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of FactoryBean interface
	//---------------------------------------------------------------------

	public Object getObject() {
		return this.timerManager;
	}

	public Class getObjectType() {
		return (this.timerManager != null ? this.timerManager.getClass() : TimerManager.class);
	}

	public boolean isSingleton() {
		return true;
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	/**
	 * Resumes the underlying TimerManager (if not shared).
	 * @see commonj.timers.TimerManager#resume()
	 */
	public void start() {
		if (!this.shared) {
			this.timerManager.resume();
		}
	}

	/**
	 * Suspends the underlying TimerManager (if not shared).
	 * @see commonj.timers.TimerManager#suspend()
	 */
	public void stop() {
		if (!this.shared) {
			this.timerManager.suspend();
		}
	}

	/**
	 * Considers the underlying TimerManager as running if it is
	 * neither suspending nor stopping.
	 * @see commonj.timers.TimerManager#isSuspending()
	 * @see commonj.timers.TimerManager#isStopping()
	 */
	public boolean isRunning() {
		return (!this.timerManager.isSuspending() && !this.timerManager.isStopping());
	}


	//---------------------------------------------------------------------
	// Implementation of DisposableBean interface
	//---------------------------------------------------------------------

	/**
	 * Cancels all statically registered Timers on shutdown,
	 * and stops the underlying TimerManager (if not shared).
	 * @see commonj.timers.Timer#cancel()
	 * @see commonj.timers.TimerManager#stop()
	 */
	public void destroy() {
		// Cancel all registered timers.
		for (Iterator it = this.timers.iterator(); it.hasNext();) {
			Timer timer = (Timer) it.next();
			try {
				timer.cancel();
			}
			catch (Throwable ex) {
				logger.warn("Could not cancel CommonJ Timer", ex);
			}
		}
		this.timers.clear();

		// Stop the entire TimerManager, if necessary.
		if (!this.shared) {
			// May return early, but at least we already cancelled all known Timers.
			this.timerManager.stop();
		}
	}

}
