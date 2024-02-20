/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.lang.Nullable;

/**
 * Base class for setting up a {@link java.util.concurrent.ExecutorService}
 * (typically a {@link java.util.concurrent.ThreadPoolExecutor} or
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}).
 *
 * <p>Defines common configuration settings and common lifecycle handling,
 * inheriting thread customization options (name, priority, etc) from
 * {@link org.springframework.util.CustomizableThreadCreator}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 */
@SuppressWarnings("serial")
public abstract class ExecutorConfigurationSupport extends CustomizableThreadFactory
		implements BeanNameAware, ApplicationContextAware, InitializingBean, DisposableBean,
		SmartLifecycle, ApplicationListener<ContextClosedEvent> {

	/**
	 * The default phase for an executor {@link SmartLifecycle}: {@code Integer.MAX_VALUE / 2}.
	 * <p>This is different from the default phase {@code Integer.MAX_VALUE} associated with
	 * other {@link SmartLifecycle} implementations, putting the typically auto-started
	 * executor/scheduler beans into an earlier startup phase and a later shutdown phase while
	 * still leaving room for regular {@link Lifecycle} components with the common phase 0.
	 * @since 6.2
	 * @see #getPhase()
	 * @see SmartLifecycle#DEFAULT_PHASE
	 * @see org.springframework.context.support.DefaultLifecycleProcessor#setTimeoutPerShutdownPhase
	 */
	public static final int DEFAULT_PHASE = Integer.MAX_VALUE / 2;


	protected final Log logger = LogFactory.getLog(getClass());

	private ThreadFactory threadFactory = this;

	private boolean threadNamePrefixSet = false;

	private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

	private boolean acceptTasksAfterContextClose = false;

	private boolean waitForTasksToCompleteOnShutdown = false;

	private long awaitTerminationMillis = 0;

	private int phase = DEFAULT_PHASE;

	@Nullable
	private String beanName;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private ExecutorService executor;

	@Nullable
	private ExecutorLifecycleDelegate lifecycleDelegate;

	private volatile boolean lateShutdown;


	/**
	 * Set the ThreadFactory to use for the ExecutorService's thread pool.
	 * The default is the underlying ExecutorService's default thread factory.
	 * <p>In a Jakarta EE or other managed environment with JSR-236 support,
	 * consider specifying a JNDI-located ManagedThreadFactory: by default,
	 * to be found at "java:comp/DefaultManagedThreadFactory".
	 * Use the "jee:jndi-lookup" namespace element in XML or the programmatic
	 * {@link org.springframework.jndi.JndiLocatorDelegate} for convenient lookup.
	 * Alternatively, consider using Spring's {@link DefaultManagedAwareThreadFactory}
	 * with its fallback to local threads in case of no managed thread factory found.
	 * @see java.util.concurrent.Executors#defaultThreadFactory()
	 * @see jakarta.enterprise.concurrent.ManagedThreadFactory
	 * @see DefaultManagedAwareThreadFactory
	 */
	public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
		this.threadFactory = (threadFactory != null ? threadFactory : this);
	}

	@Override
	public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
		super.setThreadNamePrefix(threadNamePrefix);
		this.threadNamePrefixSet = true;
	}

	/**
	 * Set the RejectedExecutionHandler to use for the ExecutorService.
	 * The default is the ExecutorService's default abort policy.
	 * @see java.util.concurrent.ThreadPoolExecutor.AbortPolicy
	 */
	public void setRejectedExecutionHandler(@Nullable RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler =
				(rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
	}

	/**
	 * Set whether to accept further tasks after the application context close phase
	 * has begun.
	 * <p>The default is {@code false} as of 6.1, triggering an early soft shutdown of
	 * the executor and therefore rejecting any further task submissions. Switch this
	 * to {@code true} in order to let other components submit tasks even during their
	 * own stop and destruction callbacks, at the expense of a longer shutdown phase.
	 * The executor will not go through a coordinated lifecycle stop phase then
	 * but rather only stop tasks on its own shutdown.
	 * <p>{@code acceptTasksAfterContextClose=true} like behavior also follows from
	 * {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}
	 * which effectively is a specific variant of this flag, replacing the early soft
	 * shutdown in the concurrent managed stop phase with a serial soft shutdown in
	 * the executor's destruction step, with individual awaiting according to the
	 * {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"} property.
	 * <p>This flag will only have effect when the executor is running in a Spring
	 * application context and able to receive the {@link ContextClosedEvent}. Also,
	 * note that {@link ThreadPoolTaskExecutor} effectively accepts tasks after context
	 * close by default, in combination with a coordinated lifecycle stop, unless
	 * {@link ThreadPoolTaskExecutor#setStrictEarlyShutdown "strictEarlyShutdown"}
	 * has been specified.
	 * @since 6.1
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 * @see DisposableBean#destroy()
	 * @see #shutdown()
	 * @see #setAwaitTerminationSeconds
	 */
	public void setAcceptTasksAfterContextClose(boolean acceptTasksAfterContextClose) {
		this.acceptTasksAfterContextClose = acceptTasksAfterContextClose;
	}

	/**
	 * Set whether to wait for scheduled tasks to complete on shutdown,
	 * not interrupting running tasks and executing all tasks in the queue.
	 * <p>The default is {@code false}, with a coordinated lifecycle stop first
	 * (unless {@link #setAcceptTasksAfterContextClose "acceptTasksAfterContextClose"}
	 * has been set) and then an immediate shutdown through interrupting ongoing
	 * tasks and clearing the queue. Switch this flag to {@code true} if you
	 * prefer fully completed tasks at the expense of a longer shutdown phase.
	 * The executor will not go through a coordinated lifecycle stop phase then
	 * but rather only stop and wait for task completion on its own shutdown.
	 * <p>Note that Spring's container shutdown continues while ongoing tasks
	 * are being completed. If you want this executor to block and wait for the
	 * termination of tasks before the rest of the container continues to shut
	 * down - e.g. in order to keep up other resources that your tasks may need -,
	 * set the {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"}
	 * property instead of or in addition to this property.
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 * @see #shutdown()
	 * @see #setAwaitTerminationSeconds
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	/**
	 * Set the maximum number of seconds that this executor is supposed to block
	 * on shutdown in order to wait for remaining tasks to complete their execution
	 * before the rest of the container continues to shut down. This is particularly
	 * useful if your remaining tasks are likely to need access to other resources
	 * that are also managed by the container.
	 * <p>By default, this executor won't wait for the termination of tasks at all.
	 * It will either shut down immediately, interrupting ongoing tasks and clearing
	 * the remaining task queue - or, if the
	 * {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}
	 * flag has been set to {@code true}, it will continue to fully execute all
	 * ongoing tasks as well as all remaining tasks in the queue, in parallel to
	 * the rest of the container shutting down.
	 * <p>In either case, if you specify an await-termination period using this property,
	 * this executor will wait for the given time (max) for the termination of tasks.
	 * As a rule of thumb, specify a significantly higher timeout here if you set
	 * "waitForTasksToCompleteOnShutdown" to {@code true} at the same time,
	 * since all remaining tasks in the queue will still get executed - in contrast
	 * to the default shutdown behavior where it's just about waiting for currently
	 * executing tasks that aren't reacting to thread interruption.
	 * @see #setAwaitTerminationMillis
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#awaitTermination
	 */
	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationMillis = awaitTerminationSeconds * 1000L;
	}

	/**
	 * Variant of {@link #setAwaitTerminationSeconds} with millisecond precision.
	 * @since 5.2.4
	 * @see #setAwaitTerminationSeconds
	 */
	public void setAwaitTerminationMillis(long awaitTerminationMillis) {
		this.awaitTerminationMillis = awaitTerminationMillis;
	}

	/**
	 * Specify the lifecycle phase for pausing and resuming this executor.
	 * <p>The default for executors/schedulers is {@link #DEFAULT_PHASE} as of 6.2,
	 * for stopping after other {@link SmartLifecycle} implementations.
	 * @since 6.1
	 * @see SmartLifecycle#getPhase()
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Return the lifecycle phase for pausing and resuming this executor.
	 * @since 6.1
	 * @see #setPhase
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	/**
	 * Calls {@code initialize()} after the container applied all property values.
	 * @see #initialize()
	 */
	@Override
	public void afterPropertiesSet() {
		initialize();
	}

	/**
	 * Set up the ExecutorService.
	 */
	public void initialize() {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (!this.threadNamePrefixSet && this.beanName != null) {
			setThreadNamePrefix(this.beanName + "-");
		}
		this.executor = initializeExecutor(this.threadFactory, this.rejectedExecutionHandler);
		this.lifecycleDelegate = new ExecutorLifecycleDelegate(this.executor);
	}

	/**
	 * Create the target {@link java.util.concurrent.ExecutorService} instance.
	 * Called by {@code afterPropertiesSet}.
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ExecutorService instance
	 * @see #afterPropertiesSet()
	 */
	protected abstract ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler);


	/**
	 * Calls {@code shutdown} when the BeanFactory destroys the executor instance.
	 * @see #shutdown()
	 */
	@Override
	public void destroy() {
		shutdown();
	}

	/**
	 * Initiate a shutdown on the underlying ExecutorService,
	 * rejecting further task submissions.
	 * <p>The executor will not accept further tasks and will prevent further
	 * scheduling of periodic tasks, letting existing tasks complete still.
	 * This step is non-blocking and can be applied as an early shutdown signal
	 * before following up with a full {@link #shutdown()} call later on.
	 * <p>Automatically called for early shutdown signals on
	 * {@link #onApplicationEvent(ContextClosedEvent) context close}.
	 * Can be manually called as well, in particular outside a container.
	 * @since 6.1
	 * @see #shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 */
	public void initiateShutdown() {
		if (this.executor != null) {
			this.executor.shutdown();
		}
	}

	/**
	 * Perform a full shutdown on the underlying ExecutorService,
	 * according to the corresponding configuration settings.
	 * <p>This step potentially blocks for the configured termination period,
	 * waiting for remaining tasks to complete. For an early shutdown signal
	 * to not accept further tasks, call {@link #initiateShutdown()} first.
	 * @see #setWaitForTasksToCompleteOnShutdown
	 * @see #setAwaitTerminationMillis
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 * @see java.util.concurrent.ExecutorService#awaitTermination
	 */
	public void shutdown() {
		if (logger.isDebugEnabled()) {
			logger.debug("Shutting down ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (this.executor != null) {
			if (this.waitForTasksToCompleteOnShutdown) {
				this.executor.shutdown();
			}
			else {
				for (Runnable remainingTask : this.executor.shutdownNow()) {
					cancelRemainingTask(remainingTask);
				}
			}
			awaitTerminationIfNecessary(this.executor);
		}
	}

	/**
	 * Cancel the given remaining task which never commenced execution,
	 * as returned from {@link ExecutorService#shutdownNow()}.
	 * @param task the task to cancel (typically a {@link RunnableFuture})
	 * @since 5.0.5
	 * @see #shutdown()
	 * @see RunnableFuture#cancel(boolean)
	 */
	protected void cancelRemainingTask(Runnable task) {
		if (task instanceof Future<?> future) {
			future.cancel(true);
		}
	}

	/**
	 * Wait for the executor to terminate, according to the value of the
	 * {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"} property.
	 */
	private void awaitTerminationIfNecessary(ExecutorService executor) {
		if (this.awaitTerminationMillis > 0) {
			try {
				if (!executor.awaitTermination(this.awaitTerminationMillis, TimeUnit.MILLISECONDS)) {
					if (logger.isWarnEnabled()) {
						logger.warn("Timed out while waiting for executor" +
								(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
					}
				}
			}
			catch (InterruptedException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Interrupted while waiting for executor" +
							(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
				}
				Thread.currentThread().interrupt();
			}
		}
	}


	/**
	 * Resume this executor if paused before (otherwise a no-op).
	 * @since 6.1
	 */
	@Override
	public void start() {
		if (this.lifecycleDelegate != null) {
			this.lifecycleDelegate.start();
		}
	}

	/**
	 * Pause this executor, not waiting for tasks to complete.
	 * @since 6.1
	 */
	@Override
	public void stop() {
		if (this.lifecycleDelegate != null && !this.lateShutdown) {
			this.lifecycleDelegate.stop();
		}
	}

	/**
	 * Pause this executor, triggering the given callback
	 * once all currently executing tasks have completed.
	 * @since 6.1
	 */
	@Override
	public void stop(Runnable callback) {
		if (this.lifecycleDelegate != null && !this.lateShutdown) {
			this.lifecycleDelegate.stop(callback);
		}
		else {
			callback.run();
		}
	}

	/**
	 * Check whether this executor is not paused and has not been shut down either.
	 * @since 6.1
	 * @see #start()
	 * @see #stop()
	 */
	@Override
	public boolean isRunning() {
		return (this.lifecycleDelegate != null && this.lifecycleDelegate.isRunning());
	}

	/**
	 * A before-execute callback for framework subclasses to delegate to
	 * (for start/stop handling), and possibly also for custom subclasses
	 * to extend (making sure to call this implementation as well).
	 * @param thread the thread to run the task
	 * @param task the task to be executed
	 * @since 6.1
	 * @see ThreadPoolExecutor#beforeExecute(Thread, Runnable)
	 */
	protected void beforeExecute(Thread thread, Runnable task) {
		if (this.lifecycleDelegate != null) {
			this.lifecycleDelegate.beforeExecute(thread);
		}
	}

	/**
	 * An after-execute callback for framework subclasses to delegate to
	 * (for start/stop handling), and possibly also for custom subclasses
	 * to extend (making sure to call this implementation as well).
	 * @param task the task that has been executed
	 * @param ex the exception thrown during execution, if any
	 * @since 6.1
	 * @see ThreadPoolExecutor#afterExecute(Runnable, Throwable)
	 */
	protected void afterExecute(Runnable task, @Nullable Throwable ex) {
		if (this.lifecycleDelegate != null) {
			this.lifecycleDelegate.afterExecute();
		}
	}

	/**
	 * {@link ContextClosedEvent} handler for initiating an early shutdown.
	 * @since 6.1
	 * @see #initiateShutdown()
	 */
	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		if (event.getApplicationContext() == this.applicationContext) {
			if (this.acceptTasksAfterContextClose || this.waitForTasksToCompleteOnShutdown) {
				// Late shutdown without early stop lifecycle.
				this.lateShutdown = true;
			}
			else {
				if (this.lifecycleDelegate != null) {
					this.lifecycleDelegate.markShutdown();
				}
				initiateEarlyShutdown();
			}
		}
	}

	/**
	 * Early shutdown signal: do not trigger further tasks, let existing tasks complete
	 * before hitting the actual destruction step in the {@link #shutdown()} method.
	 * This goes along with a {@link #stop(Runnable) coordinated lifecycle stop phase}.
	 * <p>Called from {@link #onApplicationEvent(ContextClosedEvent)} if no
	 * indications for a late shutdown have been determined, that is, if the
	 * {@link #setAcceptTasksAfterContextClose "acceptTasksAfterContextClose} and
	 * {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}
	 * flags have not been set.
	 * <p>The default implementation calls {@link #initiateShutdown()}.
	 * @since 6.1.4
	 * @see #initiateShutdown()
	 */
	protected void initiateEarlyShutdown() {
		initiateShutdown();
	}

}
