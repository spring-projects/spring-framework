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

import java.util.LinkedList;
import java.util.List;

import javax.naming.NamingException;

import commonj.timers.Timer;
import commonj.timers.TimerManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.lang.Nullable;

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
 * @deprecated as of 5.1, in favor of EE 7's
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler}
 */
@Deprecated
public class TimerManagerFactoryBean extends TimerManagerAccessor
		implements FactoryBean<TimerManager>, InitializingBean, DisposableBean, Lifecycle {

	@Nullable
	private ScheduledTimerListener[] scheduledTimerListeners;

	private final List<Timer> timers = new LinkedList<>();


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

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();

		if (this.scheduledTimerListeners != null) {
			TimerManager timerManager = obtainTimerManager();
			for (ScheduledTimerListener scheduledTask : this.scheduledTimerListeners) {
				Timer timer;
				if (scheduledTask.isOneTimeTask()) {
					timer = timerManager.schedule(scheduledTask.getTimerListener(), scheduledTask.getDelay());
				}
				else {
					if (scheduledTask.isFixedRate()) {
						timer = timerManager.scheduleAtFixedRate(
								scheduledTask.getTimerListener(), scheduledTask.getDelay(), scheduledTask.getPeriod());
					}
					else {
						timer = timerManager.schedule(
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

	@Override
	@Nullable
	public TimerManager getObject() {
		return getTimerManager();
	}

	@Override
	public Class<? extends TimerManager> getObjectType() {
		TimerManager timerManager = getTimerManager();
		return (timerManager != null ? timerManager.getClass() : TimerManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
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
	@Override
	public void destroy() {
		// Cancel all registered timers.
		for (Timer timer : this.timers) {
			try {
				timer.cancel();
			}
			catch (Throwable ex) {
				logger.debug("Could not cancel CommonJ Timer", ex);
			}
		}
		this.timers.clear();

		// Stop the TimerManager itself.
		super.destroy();
	}

}
