/*
 * Copyright 2002-2017 the original author or authors.
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

import javax.naming.NamingException;

import commonj.timers.TimerManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class for classes that are accessing a CommonJ {@link commonj.timers.TimerManager}
 * Defines common configuration settings and common lifecycle handling.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see commonj.timers.TimerManager
 */
public abstract class TimerManagerAccessor extends JndiLocatorSupport
		implements InitializingBean, DisposableBean, Lifecycle {

	@Nullable
	private TimerManager timerManager;

	@Nullable
	private String timerManagerName;

	private boolean shared = false;


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
	 * {@code resource-ref} of type {@code commonj.timers.TimerManager}
	 * in {@code web.xml}, with {@code res-sharing-scope} set to 'Unshareable'.
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


	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.timerManager == null) {
			if (this.timerManagerName == null) {
				throw new IllegalArgumentException("Either 'timerManager' or 'timerManagerName' must be specified");
			}
			this.timerManager = lookup(this.timerManagerName, TimerManager.class);
		}
	}

	/**
	 * Return the configured TimerManager, if any.
	 * @return the TimerManager, or {@code null} if not available
	 */
	@Nullable
	protected final TimerManager getTimerManager() {
		return this.timerManager;
	}

	/**
	 * Obtain the TimerManager for actual use.
	 * @return the TimerManager (never {@code null})
	 * @throws IllegalStateException in case of no TimerManager set
	 * @since 5.0
	 */
	protected TimerManager obtainTimerManager() {
		Assert.notNull(this.timerManager, "No TimerManager set");
		return this.timerManager;
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	/**
	 * Resumes the underlying TimerManager (if not shared).
	 * @see commonj.timers.TimerManager#resume()
	 */
	@Override
	public void start() {
		if (!this.shared) {
			obtainTimerManager().resume();
		}
	}

	/**
	 * Suspends the underlying TimerManager (if not shared).
	 * @see commonj.timers.TimerManager#suspend()
	 */
	@Override
	public void stop() {
		if (!this.shared) {
			obtainTimerManager().suspend();
		}
	}

	/**
	 * Considers the underlying TimerManager as running if it is
	 * neither suspending nor stopping.
	 * @see commonj.timers.TimerManager#isSuspending()
	 * @see commonj.timers.TimerManager#isStopping()
	 */
	@Override
	public boolean isRunning() {
		TimerManager tm = obtainTimerManager();
		return (!tm.isSuspending() && !tm.isStopping());
	}


	//---------------------------------------------------------------------
	// Implementation of DisposableBean interface
	//---------------------------------------------------------------------

	/**
	 * Stops the underlying TimerManager (if not shared).
	 * @see commonj.timers.TimerManager#stop()
	 */
	@Override
	public void destroy() {
		// Stop the entire TimerManager, if necessary.
		if (this.timerManager != null && !this.shared) {
			// May return early, but at least we already cancelled all known Timers.
			this.timerManager.stop();
		}
	}

}
