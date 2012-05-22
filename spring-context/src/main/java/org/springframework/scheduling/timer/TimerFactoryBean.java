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

package org.springframework.scheduling.timer;

import java.util.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * FactoryBean that sets up a {@link java.util.Timer} and exposes it for bean references.
 *
 * <p>Allows for registration of {@link ScheduledTimerTask ScheduledTimerTasks},
 * automatically starting the {@link Timer} on initialization and cancelling it
 * on destruction of the context. In scenarios that just require static registration
 * of tasks at startup, there is no need to access the {@link Timer} instance itself
 * in application code at all.
 *
 * <p>Note that the {@link Timer} mechanism uses a {@link java.util.TimerTask}
 * instance that is shared between repeated executions, in contrast to Quartz
 * which creates a new Job instance for each execution.
 *
 * @author Juergen Hoeller
 * @since 19.02.2004
 * @see ScheduledTimerTask
 * @see java.util.Timer
 * @see java.util.TimerTask
 * @deprecated as of Spring 3.0, in favor of the <code>scheduling.concurrent</code>
 * package which is based on Java 5's <code>java.util.concurrent.ExecutorService</code>
 */
@Deprecated
public class TimerFactoryBean implements FactoryBean<Timer>, BeanNameAware, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ScheduledTimerTask[] scheduledTimerTasks;

	private boolean daemon = false;

	private String beanName;

	private Timer timer;


	/**
	 * Register a list of ScheduledTimerTask objects with the Timer that
	 * this FactoryBean creates. Depending on each SchedulerTimerTask's
	 * settings, it will be registered via one of Timer's schedule methods.
	 * @see java.util.Timer#schedule(java.util.TimerTask, long)
	 * @see java.util.Timer#schedule(java.util.TimerTask, long, long)
	 * @see java.util.Timer#scheduleAtFixedRate(java.util.TimerTask, long, long)
	 */
	public void setScheduledTimerTasks(ScheduledTimerTask[] scheduledTimerTasks) {
		this.scheduledTimerTasks = scheduledTimerTasks;
	}

	/**
	 * Set whether the timer should use a daemon thread,
	 * just executing as long as the application itself is running.
	 * <p>Default is "false": The timer will automatically get cancelled on
	 * destruction of this FactoryBean. Hence, if the application shuts down,
	 * tasks will by default finish their execution. Specify "true" for eager
	 * shutdown of threads that execute tasks.
	 * @see java.util.Timer#Timer(boolean)
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	public void afterPropertiesSet() {
		logger.info("Initializing Timer");
		this.timer = createTimer(this.beanName, this.daemon);

		// Register specified ScheduledTimerTasks, if necessary.
		if (!ObjectUtils.isEmpty(this.scheduledTimerTasks)) {
			registerTasks(this.scheduledTimerTasks, this.timer);
		}
	}

	/**
	 * Create a new Timer instance. Called by <code>afterPropertiesSet</code>.
	 * Can be overridden in subclasses to provide custom Timer subclasses.
	 * @param name the desired name of the Timer's associated thread
	 * @param daemon whether to create a Timer that runs as daemon thread
	 * @return a new Timer instance
	 * @see #afterPropertiesSet()
	 * @see java.util.Timer#Timer(boolean)
	 */
	protected Timer createTimer(String name, boolean daemon) {
		if (StringUtils.hasText(name)) {
			return new Timer(name, daemon);
		}
		else {
			return new Timer(daemon);
		}
	}

	/**
	 * Register the specified {@link ScheduledTimerTask ScheduledTimerTasks}
	 * on the given {@link Timer}.
	 * @param tasks the specified ScheduledTimerTasks (never empty)
	 * @param timer the Timer to register the tasks on.
	 */
	protected void registerTasks(ScheduledTimerTask[] tasks, Timer timer) {
		for (ScheduledTimerTask task : tasks) {
			if (task.isOneTimeTask()) {
				timer.schedule(task.getTimerTask(), task.getDelay());
			}
			else {
				if (task.isFixedRate()) {
					timer.scheduleAtFixedRate(task.getTimerTask(), task.getDelay(), task.getPeriod());
				}
				else {
					timer.schedule(task.getTimerTask(), task.getDelay(), task.getPeriod());
				}
			}
		}
	}


	public Timer getObject() {
		return this.timer;
	}

	public Class<? extends Timer> getObjectType() {
		return Timer.class;
	}

	public boolean isSingleton() {
		return true;
	}


	/**
	 * Cancel the Timer on bean factory shutdown, stopping all scheduled tasks.
	 * @see java.util.Timer#cancel()
	 */
	public void destroy() {
		logger.info("Cancelling Timer");
		this.timer.cancel();
	}

}
