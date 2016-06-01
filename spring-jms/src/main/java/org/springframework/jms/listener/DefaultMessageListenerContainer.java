/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jms.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.core.Constants;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.JmsException;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.CachingDestinationResolver;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Message listener container variant that uses plain JMS client APIs, specifically
 * a loop of {@code MessageConsumer.receive()} calls that also allow for
 * transactional reception of messages (registering them with XA transactions).
 * Designed to work in a native JMS environment as well as in a Java EE environment,
 * with only minimal differences in configuration.
 *
 * <p>This is a simple but nevertheless powerful form of message listener container.
 * On startup, it obtains a fixed number of JMS Sessions to invoke the listener,
 * and optionally allows for dynamic adaptation at runtime (up to a maximum number).
 * Like {@link SimpleMessageListenerContainer}, its main advantage is its low level
 * of runtime complexity, in particular the minimal requirements on the JMS provider:
 * not even the JMS {@code ServerSessionPool} facility is required. Beyond that, it is
 * fully self-recovering in case the broker is temporarily unavailable, and allows
 * for stops/restarts as well as runtime changes to its configuration.
 *
 * <p>Actual {@code MessageListener} execution happens in asynchronous work units which are
 * created through Spring's {@link org.springframework.core.task.TaskExecutor TaskExecutor}
 * abstraction. By default, the specified number of invoker tasks will be created
 * on startup, according to the {@link #setConcurrentConsumers "concurrentConsumers"}
 * setting. Specify an alternative {@code TaskExecutor} to integrate with an existing
 * thread pool facility (such as a Java EE server's), for example using a
 * {@link org.springframework.scheduling.commonj.WorkManagerTaskExecutor CommonJ WorkManager}.
 * With a native JMS setup, each of those listener threads is going to use a
 * cached JMS {@code Session} and {@code MessageConsumer} (only refreshed in case
 * of failure), using the JMS provider's resources as efficiently as possible.
 *
 * <p>Message reception and listener execution can automatically be wrapped
 * in transactions by passing a Spring
 * {@link org.springframework.transaction.PlatformTransactionManager} into the
 * {@link #setTransactionManager "transactionManager"} property. This will usually
 * be a {@link org.springframework.transaction.jta.JtaTransactionManager} in a
 * Java EE environment, in combination with a JTA-aware JMS {@code ConnectionFactory}
 * obtained from JNDI (check your Java EE server's documentation). Note that this
 * listener container will automatically reobtain all JMS handles for each transaction
 * in case an external transaction manager is specified, for compatibility with
 * all Java EE servers (in particular JBoss). This non-caching behavior can be
 * overridden through the {@link #setCacheLevel "cacheLevel"} /
 * {@link #setCacheLevelName "cacheLevelName"} property, enforcing caching of
 * the {@code Connection} (or also {@code Session} and {@code MessageConsumer})
 * even if an external transaction manager is involved.
 *
 * <p>Dynamic scaling of the number of concurrent invokers can be activated
 * by specifying a {@link #setMaxConcurrentConsumers "maxConcurrentConsumers"}
 * value that is higher than the {@link #setConcurrentConsumers "concurrentConsumers"}
 * value. Since the latter's default is 1, you can also simply specify a
 * "maxConcurrentConsumers" of e.g. 5, which will lead to dynamic scaling up to
 * 5 concurrent consumers in case of increasing message load, as well as dynamic
 * shrinking back to the standard number of consumers once the load decreases.
 * Consider adapting the {@link #setIdleTaskExecutionLimit "idleTaskExecutionLimit"}
 * setting to control the lifespan of each new task, to avoid frequent scaling up
 * and down, in particular if the {@code ConnectionFactory} does not pool JMS
 * {@code Sessions} and/or the {@code TaskExecutor} does not pool threads (check
 * your configuration!). Note that dynamic scaling only really makes sense for a
 * queue in the first place; for a topic, you will typically stick with the default
 * number of 1 consumer, otherwise you'd receive the same message multiple times on
 * the same node.
 *
 * <p><b>Note: Don't use Spring's {@link org.springframework.jms.connection.CachingConnectionFactory}
 * in combination with dynamic scaling.</b> Ideally, don't use it with a message
 * listener container at all, since it is generally preferable to let the
 * listener container itself handle appropriate caching within its lifecycle.
 * Also, stopping and restarting a listener container will only work with an
 * independent, locally cached Connection - not with an externally cached one.
 *
 * <p><b>It is strongly recommended to either set {@link #setSessionTransacted
 * "sessionTransacted"} to "true" or specify an external {@link #setTransactionManager
 * "transactionManager"}.</b> See the {@link AbstractMessageListenerContainer}
 * javadoc for details on acknowledge modes and native transaction options, as
 * well as the {@link AbstractPollingMessageListenerContainer} javadoc for details
 * on configuring an external transaction manager. Note that for the default
 * "AUTO_ACKNOWLEDGE" mode, this container applies automatic message acknowledgment
 * before listener execution, with no redelivery in case of an exception.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setTransactionManager
 * @see #setCacheLevel
 * @see javax.jms.MessageConsumer#receive(long)
 * @see SimpleMessageListenerContainer
 * @see org.springframework.jms.listener.endpoint.JmsMessageEndpointManager
 */
public class DefaultMessageListenerContainer extends AbstractPollingMessageListenerContainer {

	/**
	 * Default thread name prefix: "DefaultMessageListenerContainer-".
	 */
	public static final String DEFAULT_THREAD_NAME_PREFIX =
			ClassUtils.getShortName(DefaultMessageListenerContainer.class) + "-";

	/**
	 * The default recovery interval: 5000 ms = 5 seconds.
	 */
	public static final long DEFAULT_RECOVERY_INTERVAL = 5000;


	/**
	 * Constant that indicates to cache no JMS resources at all.
	 * @see #setCacheLevel
	 */
	public static final int CACHE_NONE = 0;

	/**
	 * Constant that indicates to cache a shared JMS {@code Connection} for each
	 * listener thread.
	 * @see #setCacheLevel
	 */
	public static final int CACHE_CONNECTION = 1;

	/**
	 * Constant that indicates to cache a shared JMS {@code Connection} and a JMS
	 * {@code Session} for each listener thread.
	 * @see #setCacheLevel
	 */
	public static final int CACHE_SESSION = 2;

	/**
	 * Constant that indicates to cache a shared JMS {@code Connection}, a JMS
	 * {@code Session}, and a JMS MessageConsumer for each listener thread.
	 * @see #setCacheLevel
	 */
	public static final int CACHE_CONSUMER = 3;

	/**
	 * Constant that indicates automatic choice of an appropriate caching level
	 * (depending on the transaction management strategy).
	 * @see #setCacheLevel
	 */
	public static final int CACHE_AUTO = 4;


	private static final Constants constants = new Constants(DefaultMessageListenerContainer.class);


	private Executor taskExecutor;

	private BackOff backOff = new FixedBackOff(DEFAULT_RECOVERY_INTERVAL, Long.MAX_VALUE);

	private int cacheLevel = CACHE_AUTO;

	private int concurrentConsumers = 1;

	private int maxConcurrentConsumers = 1;

	private int maxMessagesPerTask = Integer.MIN_VALUE;

	private int idleConsumerLimit = 1;

	private int idleTaskExecutionLimit = 1;

	private final Set<AsyncMessageListenerInvoker> scheduledInvokers = new HashSet<AsyncMessageListenerInvoker>();

	private int activeInvokerCount = 0;

	private int registeredWithDestination = 0;

	private volatile boolean recovering = false;

	private volatile boolean interrupted = false;

	private Runnable stopCallback;

	private Object currentRecoveryMarker = new Object();

	private final Object recoveryMonitor = new Object();


	/**
	 * Set the Spring {@code TaskExecutor} to use for running the listener threads.
	 * <p>Default is a {@link org.springframework.core.task.SimpleAsyncTaskExecutor},
	 * starting up a number of new threads, according to the specified number
	 * of concurrent consumers.
	 * <p>Specify an alternative {@code TaskExecutor} for integration with an existing
	 * thread pool. Note that this really only adds value if the threads are
	 * managed in a specific fashion, for example within a Java EE environment.
	 * A plain thread pool does not add much value, as this listener container
	 * will occupy a number of threads for its entire lifetime.
	 * @see #setConcurrentConsumers
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Specify the {@link BackOff} instance to use to compute the interval
	 * between recovery attempts. If the {@link BackOffExecution} implementation
	 * returns {@link BackOffExecution#STOP}, this listener container will not further
	 * attempt to recover.
	 * <p>The {@link #setRecoveryInterval(long) recovery interval} is ignored
	 * when this property is set.
	 * @since 4.1
	 */
	public void setBackOff(BackOff backOff) {
		this.backOff = backOff;
	}

	/**
	 * Specify the interval between recovery attempts, in <b>milliseconds</b>.
	 * The default is 5000 ms, that is, 5 seconds. This is a convenience method
	 * to create a {@link FixedBackOff} with the specified interval.
	 * <p>For more recovery options, consider specifying a {@link BackOff}
	 * instance instead.
	 * @see #setBackOff(BackOff)
	 * @see #handleListenerSetupFailure
	 */
	public void setRecoveryInterval(long recoveryInterval) {
		this.backOff = new FixedBackOff(recoveryInterval, Long.MAX_VALUE);
	}

	/**
	 * Specify the level of caching that this listener container is allowed to apply,
	 * in the form of the name of the corresponding constant: e.g. "CACHE_CONNECTION".
	 * @see #setCacheLevel
	 */
	public void setCacheLevelName(String constantName) throws IllegalArgumentException {
		if (constantName == null || !constantName.startsWith("CACHE_")) {
			throw new IllegalArgumentException("Only cache constants allowed");
		}
		setCacheLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * Specify the level of caching that this listener container is allowed to apply.
	 * <p>Default is {@link #CACHE_NONE} if an external transaction manager has been specified
	 * (to reobtain all resources freshly within the scope of the external transaction),
	 * and {@link #CACHE_CONSUMER} otherwise (operating with local JMS resources).
	 * <p>Some Java EE servers only register their JMS resources with an ongoing XA
	 * transaction in case of a freshly obtained JMS {@code Connection} and {@code Session},
	 * which is why this listener container by default does not cache any of those.
	 * However, depending on the rules of your server with respect to the caching
	 * of transactional resources, consider switching this setting to at least
	 * {@link #CACHE_CONNECTION} or {@link #CACHE_SESSION} even in conjunction with an
	 * external transaction manager.
	 * @see #CACHE_NONE
	 * @see #CACHE_CONNECTION
	 * @see #CACHE_SESSION
	 * @see #CACHE_CONSUMER
	 * @see #setCacheLevelName
	 * @see #setTransactionManager
	 */
	public void setCacheLevel(int cacheLevel) {
		this.cacheLevel = cacheLevel;
	}

	/**
	 * Return the level of caching that this listener container is allowed to apply.
	 */
	public int getCacheLevel() {
		return this.cacheLevel;
	}


	/**
	 * Specify concurrency limits via a "lower-upper" String, e.g. "5-10", or a simple
	 * upper limit String, e.g. "10" (the lower limit will be 1 in this case).
	 * <p>This listener container will always hold on to the minimum number of consumers
	 * ({@link #setConcurrentConsumers}) and will slowly scale up to the maximum number
	 * of consumers {@link #setMaxConcurrentConsumers} in case of increasing load.
	 */
	@Override
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setConcurrentConsumers(Integer.parseInt(concurrency.substring(0, separatorIndex)));
				setMaxConcurrentConsumers(Integer.parseInt(concurrency.substring(separatorIndex + 1, concurrency.length())));
			}
			else {
				setConcurrentConsumers(1);
				setMaxConcurrentConsumers(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported.");
		}
	}

	/**
	 * Specify the number of concurrent consumers to create. Default is 1.
	 * <p>Specifying a higher value for this setting will increase the standard
	 * level of scheduled concurrent consumers at runtime: This is effectively
	 * the minimum number of concurrent consumers which will be scheduled
	 * at any given time. This is a static setting; for dynamic scaling,
	 * consider specifying the "maxConcurrentConsumers" setting instead.
	 * <p>Raising the number of concurrent consumers is recommendable in order
	 * to scale the consumption of messages coming in from a queue. However,
	 * note that any ordering guarantees are lost once multiple consumers are
	 * registered. In general, stick with 1 consumer for low-volume queues.
	 * <p><b>Do not raise the number of concurrent consumers for a topic,
	 * unless vendor-specific setup measures clearly allow for it.</b>
	 * With regular setup, this would lead to concurrent consumption
	 * of the same message, which is hardly ever desirable.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @see #setMaxConcurrentConsumers
	 */
	public void setConcurrentConsumers(int concurrentConsumers) {
		Assert.isTrue(concurrentConsumers > 0, "'concurrentConsumers' value must be at least 1 (one)");
		synchronized (this.lifecycleMonitor) {
			this.concurrentConsumers = concurrentConsumers;
			if (this.maxConcurrentConsumers < concurrentConsumers) {
				this.maxConcurrentConsumers = concurrentConsumers;
			}
		}
	}

	/**
	 * Return the "concurrentConsumer" setting.
	 * <p>This returns the currently configured "concurrentConsumers" value;
	 * the number of currently scheduled/active consumers might differ.
	 * @see #getScheduledConsumerCount()
	 * @see #getActiveConsumerCount()
	 */
	public final int getConcurrentConsumers() {
		synchronized (this.lifecycleMonitor) {
			return this.concurrentConsumers;
		}
	}

	/**
	 * Specify the maximum number of concurrent consumers to create. Default is 1.
	 * <p>If this setting is higher than "concurrentConsumers", the listener container
	 * will dynamically schedule new consumers at runtime, provided that enough
	 * incoming messages are encountered. Once the load goes down again, the number of
	 * consumers will be reduced to the standard level ("concurrentConsumers") again.
	 * <p>Raising the number of concurrent consumers is recommendable in order
	 * to scale the consumption of messages coming in from a queue. However,
	 * note that any ordering guarantees are lost once multiple consumers are
	 * registered. In general, stick with 1 consumer for low-volume queues.
	 * <p><b>Do not raise the number of concurrent consumers for a topic,
	 * unless vendor-specific setup measures clearly allow for it.</b>
	 * With regular setup, this would lead to concurrent consumption
	 * of the same message, which is hardly ever desirable.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @see #setConcurrentConsumers
	 */
	public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
		Assert.isTrue(maxConcurrentConsumers > 0, "'maxConcurrentConsumers' value must be at least 1 (one)");
		synchronized (this.lifecycleMonitor) {
			this.maxConcurrentConsumers =
					(maxConcurrentConsumers > this.concurrentConsumers ? maxConcurrentConsumers : this.concurrentConsumers);
		}
	}

	/**
	 * Return the "maxConcurrentConsumer" setting.
	 * <p>This returns the currently configured "maxConcurrentConsumers" value;
	 * the number of currently scheduled/active consumers might differ.
	 * @see #getScheduledConsumerCount()
	 * @see #getActiveConsumerCount()
	 */
	public final int getMaxConcurrentConsumers() {
		synchronized (this.lifecycleMonitor) {
			return this.maxConcurrentConsumers;
		}
	}

	/**
	 * Specify the maximum number of messages to process in one task.
	 * More concretely, this limits the number of message reception attempts
	 * per task, which includes receive iterations that did not actually
	 * pick up a message until they hit their timeout (see the
	 * {@link #setReceiveTimeout "receiveTimeout"} property).
	 * <p>Default is unlimited (-1) in case of a standard TaskExecutor,
	 * reusing the original invoker threads until shutdown (at the
	 * expense of limited dynamic scheduling).
	 * <p>In case of a SchedulingTaskExecutor indicating a preference for
	 * short-lived tasks, the default is 10 instead. Specify a number
	 * of 10 to 100 messages to balance between rather long-lived and
	 * rather short-lived tasks here.
	 * <p>Long-lived tasks avoid frequent thread context switches through
	 * sticking with the same thread all the way through, while short-lived
	 * tasks allow thread pools to control the scheduling. Hence, thread
	 * pools will usually prefer short-lived tasks.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @see #setTaskExecutor
	 * @see #setReceiveTimeout
	 * @see org.springframework.scheduling.SchedulingTaskExecutor#prefersShortLivedTasks()
	 */
	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask != 0, "'maxMessagesPerTask' must not be 0");
		synchronized (this.lifecycleMonitor) {
			this.maxMessagesPerTask = maxMessagesPerTask;
		}
	}

	/**
	 * Return the maximum number of messages to process in one task.
	 */
	public final int getMaxMessagesPerTask() {
		synchronized (this.lifecycleMonitor) {
			return this.maxMessagesPerTask;
		}
	}

	/**
	 * Specify the limit for the number of consumers that are allowed to be idle
	 * at any given time.
	 * <p>This limit is used by the {@link #scheduleNewInvokerIfAppropriate} method
	 * to determine if a new invoker should be created. Increasing the limit causes
	 * invokers to be created more aggressively. This can be useful to ramp up the
	 * number of invokers faster.
	 * <p>The default is 1, only scheduling a new invoker (which is likely to
	 * be idle initially) if none of the existing invokers is currently idle.
	 */
	public void setIdleConsumerLimit(int idleConsumerLimit) {
		Assert.isTrue(idleConsumerLimit > 0, "'idleConsumerLimit' must be 1 or higher");
		synchronized (this.lifecycleMonitor) {
			this.idleConsumerLimit = idleConsumerLimit;
		}
	}

	/**
	 * Return the limit for the number of idle consumers.
	 */
	public final int getIdleConsumerLimit() {
		synchronized (this.lifecycleMonitor) {
			return this.idleConsumerLimit;
		}
	}

	/**
	 * Specify the limit for idle executions of a consumer task, not having
	 * received any message within its execution. If this limit is reached,
	 * the task will shut down and leave receiving to other executing tasks.
	 * <p>The default is 1, closing idle resources early once a task didn't
	 * receive a message. This applies to dynamic scheduling only; see the
	 * {@link #setMaxConcurrentConsumers "maxConcurrentConsumers"} setting.
	 * The minimum number of consumers
	 * (see {@link #setConcurrentConsumers "concurrentConsumers"})
	 * will be kept around until shutdown in any case.
	 * <p>Within each task execution, a number of message reception attempts
	 * (according to the "maxMessagesPerTask" setting) will each wait for an incoming
	 * message (according to the "receiveTimeout" setting). If all of those receive
	 * attempts in a given task return without a message, the task is considered
	 * idle with respect to received messages. Such a task may still be rescheduled;
	 * however, once it reached the specified "idleTaskExecutionLimit", it will
	 * shut down (in case of dynamic scaling).
	 * <p>Raise this limit if you encounter too frequent scaling up and down.
	 * With this limit being higher, an idle consumer will be kept around longer,
	 * avoiding the restart of a consumer once a new load of messages comes in.
	 * Alternatively, specify a higher "maxMessagesPerTask" and/or "receiveTimeout" value,
	 * which will also lead to idle consumers being kept around for a longer time
	 * (while also increasing the average execution time of each scheduled task).
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @see #setMaxMessagesPerTask
	 * @see #setReceiveTimeout
	 */
	public void setIdleTaskExecutionLimit(int idleTaskExecutionLimit) {
		Assert.isTrue(idleTaskExecutionLimit > 0, "'idleTaskExecutionLimit' must be 1 or higher");
		synchronized (this.lifecycleMonitor) {
			this.idleTaskExecutionLimit = idleTaskExecutionLimit;
		}
	}

	/**
	 * Return the limit for idle executions of a consumer task.
	 */
	public final int getIdleTaskExecutionLimit() {
		synchronized (this.lifecycleMonitor) {
			return this.idleTaskExecutionLimit;
		}
	}


	//-------------------------------------------------------------------------
	// Implementation of AbstractMessageListenerContainer's template methods
	//-------------------------------------------------------------------------

	@Override
	public void initialize() {
		// Adapt default cache level.
		if (this.cacheLevel == CACHE_AUTO) {
			this.cacheLevel = (getTransactionManager() != null ? CACHE_NONE : CACHE_CONSUMER);
		}

		// Prepare taskExecutor and maxMessagesPerTask.
		synchronized (this.lifecycleMonitor) {
			if (this.taskExecutor == null) {
				this.taskExecutor = createDefaultTaskExecutor();
			}
			else if (this.taskExecutor instanceof SchedulingTaskExecutor &&
					((SchedulingTaskExecutor) this.taskExecutor).prefersShortLivedTasks() &&
					this.maxMessagesPerTask == Integer.MIN_VALUE) {
				// TaskExecutor indicated a preference for short-lived tasks. According to
				// setMaxMessagesPerTask javadoc, we'll use 10 message per task in this case
				// unless the user specified a custom value.
				this.maxMessagesPerTask = 10;
			}
		}

		// Proceed with actual listener initialization.
		super.initialize();
	}

	/**
	 * Creates the specified number of concurrent consumers,
	 * in the form of a JMS Session plus associated MessageConsumer
	 * running in a separate thread.
	 * @see #scheduleNewInvoker
	 * @see #setTaskExecutor
	 */
	@Override
	protected void doInitialize() throws JMSException {
		synchronized (this.lifecycleMonitor) {
			for (int i = 0; i < this.concurrentConsumers; i++) {
				scheduleNewInvoker();
			}
		}
	}

	/**
	 * Destroy the registered JMS Sessions and associated MessageConsumers.
	 */
	@Override
	protected void doShutdown() throws JMSException {
		logger.debug("Waiting for shutdown of message listener invokers");
		try {
			synchronized (this.lifecycleMonitor) {
				// Waiting for AsyncMessageListenerInvokers to deactivate themselves...
				while (this.activeInvokerCount > 0) {
					if (logger.isDebugEnabled()) {
						logger.debug("Still waiting for shutdown of " + this.activeInvokerCount +
								" message listener invokers");
					}
					long timeout = getReceiveTimeout();
					if (timeout > 0) {
						this.lifecycleMonitor.wait(timeout);
					}
					else {
						this.lifecycleMonitor.wait();
					}
				}
				// Clear remaining scheduled invokers, possibly left over as paused tasks...
				for (AsyncMessageListenerInvoker scheduledInvoker : this.scheduledInvokers) {
					scheduledInvoker.clearResources();
				}
				this.scheduledInvokers.clear();
			}
		}
		catch (InterruptedException ex) {
			// Re-interrupt current thread, to allow other threads to react.
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Overridden to reset the stop callback, if any.
	 */
	@Override
	public void start() throws JmsException {
		synchronized (this.lifecycleMonitor) {
			this.stopCallback = null;
		}
		super.start();
	}

	/**
	 * Stop this listener container, invoking the specific callback
	 * once all listener processing has actually stopped.
	 * <p>Note: Further {@code stop(runnable)} calls (before processing
	 * has actually stopped) will override the specified callback. Only the
	 * latest specified callback will be invoked.
	 * <p>If a subsequent {@link #start()} call restarts the listener container
	 * before it has fully stopped, the callback will not get invoked at all.
	 * @param callback the callback to invoke once listener processing
	 * has fully stopped
	 * @throws JmsException if stopping failed
	 * @see #stop()
	 */
	@Override
	public void stop(Runnable callback) throws JmsException {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning() || this.stopCallback != null) {
				// Not started, already stopped, or previous stop attempt in progress
				// -> return immediately, no stop process to control anymore.
				callback.run();
				return;
			}
			this.stopCallback = callback;
		}
		stop();
	}

	/**
	 * Return the number of currently scheduled consumers.
	 * <p>This number will always be between "concurrentConsumers" and
	 * "maxConcurrentConsumers", but might be higher than "activeConsumerCount"
	 * (in case some consumers are scheduled but not executing at the moment).
	 * @see #getConcurrentConsumers()
	 * @see #getMaxConcurrentConsumers()
	 * @see #getActiveConsumerCount()
	 */
	public final int getScheduledConsumerCount() {
		synchronized (this.lifecycleMonitor) {
			return this.scheduledInvokers.size();
		}
	}

	/**
	 * Return the number of currently active consumers.
	 * <p>This number will always be between "concurrentConsumers" and
	 * "maxConcurrentConsumers", but might be lower than "scheduledConsumerCount"
	 * (in case some consumers are scheduled but not executing at the moment).
	 * @see #getConcurrentConsumers()
	 * @see #getMaxConcurrentConsumers()
	 * @see #getActiveConsumerCount()
	 */
	public final int getActiveConsumerCount() {
		synchronized (this.lifecycleMonitor) {
			return this.activeInvokerCount;
		}
	}

	/**
	 * Return whether at least one consumer has entered a fixed registration with the
	 * target destination. This is particularly interesting for the pub-sub case where
	 * it might be important to have an actual consumer registered that is guaranteed
	 * not to miss any messages that are just about to be published.
	 * <p>This method may be polled after a {@link #start()} call, until asynchronous
	 * registration of consumers has happened which is when the method will start returning
	 * {@code true} &ndash; provided that the listener container ever actually establishes
	 * a fixed registration. It will then keep returning {@code true} until shutdown,
	 * since the container will hold on to at least one consumer registration thereafter.
	 * <p>Note that a listener container is not bound to having a fixed registration in
	 * the first place. It may also keep recreating consumers for every invoker execution.
	 * This particularly depends on the {@link #setCacheLevel cache level} setting:
	 * only {@link #CACHE_CONSUMER} will lead to a fixed registration.
	 */
	public boolean isRegisteredWithDestination() {
		synchronized (this.lifecycleMonitor) {
			return (this.registeredWithDestination > 0);
		}
	}


	/**
	 * Create a default TaskExecutor. Called if no explicit TaskExecutor has been specified.
	 * <p>The default implementation builds a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
	 * with the specified bean name (or the class name, if no bean name specified) as thread name prefix.
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
	 */
	protected TaskExecutor createDefaultTaskExecutor() {
		String beanName = getBeanName();
		String threadNamePrefix = (beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
		return new SimpleAsyncTaskExecutor(threadNamePrefix);
	}

	/**
	 * Schedule a new invoker, increasing the total number of scheduled
	 * invokers for this listener container.
	 */
	private void scheduleNewInvoker() {
		AsyncMessageListenerInvoker invoker = new AsyncMessageListenerInvoker();
		if (rescheduleTaskIfNecessary(invoker)) {
			// This should always be true, since we're only calling this when active.
			this.scheduledInvokers.add(invoker);
		}
	}

	/**
	 * Use a shared JMS Connection depending on the "cacheLevel" setting.
	 * @see #setCacheLevel
	 * @see #CACHE_CONNECTION
	 */
	@Override
	protected final boolean sharedConnectionEnabled() {
		return (getCacheLevel() >= CACHE_CONNECTION);
	}

	/**
	 * Re-executes the given task via this listener container's TaskExecutor.
	 * @see #setTaskExecutor
	 */
	@Override
	protected void doRescheduleTask(Object task) {
		this.taskExecutor.execute((Runnable) task);
	}

	/**
	 * Tries scheduling a new invoker, since we know messages are coming in...
	 * @see #scheduleNewInvokerIfAppropriate()
	 */
	@Override
	protected void messageReceived(Object invoker, Session session) {
		((AsyncMessageListenerInvoker) invoker).setIdle(false);
		scheduleNewInvokerIfAppropriate();
	}

	/**
	 * Marks the affected invoker as idle.
	 */
	@Override
	protected void noMessageReceived(Object invoker, Session session) {
		((AsyncMessageListenerInvoker) invoker).setIdle(true);
	}

	/**
	 * Schedule a new invoker, increasing the total number of scheduled
	 * invokers for this listener container, but only if the specified
	 * "maxConcurrentConsumers" limit has not been reached yet, and only
	 * if the specified "idleConsumerLimit" has not been reached either.
	 * <p>Called once a message has been received, in order to scale up while
	 * processing the message in the invoker that originally received it.
	 * @see #setTaskExecutor
	 * @see #getMaxConcurrentConsumers()
	 * @see #getIdleConsumerLimit()
	 */
	protected void scheduleNewInvokerIfAppropriate() {
		if (isRunning()) {
			resumePausedTasks();
			synchronized (this.lifecycleMonitor) {
				if (this.scheduledInvokers.size() < this.maxConcurrentConsumers &&
						getIdleInvokerCount() < this.idleConsumerLimit) {
					scheduleNewInvoker();
					if (logger.isDebugEnabled()) {
						logger.debug("Raised scheduled invoker count: " + this.scheduledInvokers.size());
					}
				}
			}
		}
	}

	/**
	 * Determine whether the current invoker should be rescheduled,
	 * given that it might not have received a message in a while.
	 * @param idleTaskExecutionCount the number of idle executions
	 * that this invoker task has already accumulated (in a row)
	 */
	private boolean shouldRescheduleInvoker(int idleTaskExecutionCount) {
		boolean superfluous =
				(idleTaskExecutionCount >= this.idleTaskExecutionLimit && getIdleInvokerCount() > 1);
		return (this.scheduledInvokers.size() <=
				(superfluous ? this.concurrentConsumers : this.maxConcurrentConsumers));
	}

	/**
	 * Determine whether this listener container currently has more
	 * than one idle instance among its scheduled invokers.
	 */
	private int getIdleInvokerCount() {
		int count = 0;
		for (AsyncMessageListenerInvoker invoker : this.scheduledInvokers) {
			if (invoker.isIdle()) {
				count++;
			}
		}
		return count;
	}


	/**
	 * Overridden to accept a failure in the initial setup - leaving it up to the
	 * asynchronous invokers to establish the shared Connection on first access.
	 * @see #refreshConnectionUntilSuccessful()
	 */
	@Override
	protected void establishSharedConnection() {
		try {
			super.establishSharedConnection();
		}
		catch (Exception ex) {
			if (ex instanceof JMSException) {
				invokeExceptionListener((JMSException) ex);
			}
			logger.debug("Could not establish shared JMS Connection - " +
					"leaving it up to asynchronous invokers to establish a Connection as soon as possible", ex);
		}
	}

	/**
	 * This implementations proceeds even after an exception thrown from
	 * {@code Connection.start()}, relying on listeners to perform
	 * appropriate recovery.
	 */
	@Override
	protected void startSharedConnection() {
		try {
			super.startSharedConnection();
		}
		catch (Exception ex) {
			logger.debug("Connection start failed - relying on listeners to perform recovery", ex);
		}
	}

	/**
	 * This implementations proceeds even after an exception thrown from
	 * {@code Connection.stop()}, relying on listeners to perform
	 * appropriate recovery after a restart.
	 */
	@Override
	protected void stopSharedConnection() {
		try {
			super.stopSharedConnection();
		}
		catch (Exception ex) {
			logger.debug("Connection stop failed - relying on listeners to perform recovery after restart", ex);
		}
	}

	/**
	 * Handle the given exception that arose during setup of a listener.
	 * Called for every such exception in every concurrent listener.
	 * <p>The default implementation logs the exception at warn level
	 * if not recovered yet, and at debug level if already recovered.
	 * Can be overridden in subclasses.
	 * @param ex the exception to handle
	 * @param alreadyRecovered whether a previously executing listener
	 * already recovered from the present listener setup failure
	 * (this usually indicates a follow-up failure than can be ignored
	 * other than for debug log purposes)
	 * @see #recoverAfterListenerSetupFailure()
	 */
	protected void handleListenerSetupFailure(Throwable ex, boolean alreadyRecovered) {
		if (ex instanceof JMSException) {
			invokeExceptionListener((JMSException) ex);
		}
		if (ex instanceof SharedConnectionNotInitializedException) {
			if (!alreadyRecovered) {
				logger.info("JMS message listener invoker needs to establish shared Connection");
			}
		}
		else {
			// Recovery during active operation..
			if (alreadyRecovered) {
				logger.debug("Setup of JMS message listener invoker failed - already recovered by other invoker", ex);
			}
			else {
				StringBuilder msg = new StringBuilder();
				msg.append("Setup of JMS message listener invoker failed for destination '");
				msg.append(getDestinationDescription()).append("' - trying to recover. Cause: ");
				msg.append(ex instanceof JMSException ? JmsUtils.buildExceptionMessage((JMSException) ex) : ex.getMessage());
				if (logger.isDebugEnabled()) {
					logger.warn(msg, ex);
				}
				else {
					logger.warn(msg);
				}
			}
		}
	}

	/**
	 * Recover this listener container after a listener failed to set itself up,
	 * for example re-establishing the underlying Connection.
	 * <p>The default implementation delegates to DefaultMessageListenerContainer's
	 * recovery-capable {@link #refreshConnectionUntilSuccessful()} method, which will
	 * try to re-establish a Connection to the JMS provider both for the shared
	 * and the non-shared Connection case.
	 * @see #refreshConnectionUntilSuccessful()
	 * @see #refreshDestination()
	 */
	protected void recoverAfterListenerSetupFailure() {
		this.recovering = true;
		try {
			refreshConnectionUntilSuccessful();
			refreshDestination();
		}
		finally {
			this.recovering = false;
			this.interrupted = false;
		}
	}

	/**
	 * Refresh the underlying Connection, not returning before an attempt has been
	 * successful. Called in case of a shared Connection as well as without shared
	 * Connection, so either needs to operate on the shared Connection or on a
	 * temporary Connection that just gets established for validation purposes.
	 * <p>The default implementation retries until it successfully established a
	 * Connection, for as long as this message listener container is running.
	 * Applies the specified recovery interval between retries.
	 * @see #setRecoveryInterval
	 * @see #start()
	 * @see #stop()
	 */
	protected void refreshConnectionUntilSuccessful() {
		BackOffExecution execution = this.backOff.start();
		while (isRunning()) {
			try {
				if (sharedConnectionEnabled()) {
					refreshSharedConnection();
				}
				else {
					Connection con = createConnection();
					JmsUtils.closeConnection(con);
				}
				logger.info("Successfully refreshed JMS Connection");
				break;
			}
			catch (Exception ex) {
				if (ex instanceof JMSException) {
					invokeExceptionListener((JMSException) ex);
				}
				StringBuilder msg = new StringBuilder();
				msg.append("Could not refresh JMS Connection for destination '");
				msg.append(getDestinationDescription()).append("' - retrying using ");
				msg.append(execution).append(". Cause: ");
				msg.append(ex instanceof JMSException ? JmsUtils.buildExceptionMessage((JMSException) ex) : ex.getMessage());
				if (logger.isDebugEnabled()) {
					logger.error(msg, ex);
				}
				else {
					logger.error(msg);
				}
			}
			if (!applyBackOffTime(execution)) {
				StringBuilder msg = new StringBuilder();
				msg.append("Stopping container for destination '")
						.append(getDestinationDescription())
						.append("': back-off policy does not allow ").append("for further attempts.");
				logger.error(msg.toString());
				stop();
			}
		}
	}

	/**
	 * Refresh the JMS destination that this listener container operates on.
	 * <p>Called after listener setup failure, assuming that a cached Destination
	 * object might have become invalid (a typical case on WebLogic JMS).
	 * <p>The default implementation removes the destination from a
	 * DestinationResolver's cache, in case of a CachingDestinationResolver.
	 * @see #setDestinationName
	 * @see org.springframework.jms.support.destination.CachingDestinationResolver
	 */
	protected void refreshDestination() {
		String destName = getDestinationName();
		if (destName != null) {
			DestinationResolver destResolver = getDestinationResolver();
			if (destResolver instanceof CachingDestinationResolver) {
				((CachingDestinationResolver) destResolver).removeFromCache(destName);
			}
		}
	}

	/**
	 * Apply the next back-off time using the specified {@link BackOffExecution}.
	 * <p>Return {@code true} if the back-off period has been applied and a new
	 * attempt to recover should be made, {@code false} if no further attempt
	 * should be made.
	 * @since 4.1
	 */
	protected boolean applyBackOffTime(BackOffExecution execution) {
		if (this.recovering && this.interrupted) {
			// Interrupted right before and still failing... give up.
			return false;
		}
		long interval = execution.nextBackOff();
		if (interval == BackOffExecution.STOP) {
			return false;
		}
		else {
			try {
				synchronized (this.lifecycleMonitor) {
					this.lifecycleMonitor.wait(interval);
				}
			}
			catch (InterruptedException interEx) {
				// Re-interrupt current thread, to allow other threads to react.
				Thread.currentThread().interrupt();
				if (this.recovering) {
					this.interrupted = true;
				}
			}
			return true;
		}
	}

	/**
	 * Return whether this listener container is currently in a recovery attempt.
	 * <p>May be used to detect recovery phases but also the end of a recovery phase,
	 * with {@code isRecovering()} switching to {@code false} after having been found
	 * to return {@code true} before.
	 * @see #recoverAfterListenerSetupFailure()
	 */
	public final boolean isRecovering() {
		return this.recovering;
	}


	//-------------------------------------------------------------------------
	// Inner classes used as internal adapters
	//-------------------------------------------------------------------------

	/**
	 * Runnable that performs looped {@code MessageConsumer.receive()} calls.
	 */
	private class AsyncMessageListenerInvoker implements SchedulingAwareRunnable {

		private Session session;

		private MessageConsumer consumer;

		private Object lastRecoveryMarker;

		private boolean lastMessageSucceeded;

		private int idleTaskExecutionCount = 0;

		private volatile boolean idle = true;

		@Override
		public void run() {
			synchronized (lifecycleMonitor) {
				activeInvokerCount++;
				lifecycleMonitor.notifyAll();
			}
			boolean messageReceived = false;
			try {
				if (maxMessagesPerTask < 0) {
					messageReceived = executeOngoingLoop();
				}
				else {
					int messageCount = 0;
					while (isRunning() && messageCount < maxMessagesPerTask) {
						messageReceived = (invokeListener() || messageReceived);
						messageCount++;
					}
				}
			}
			catch (Throwable ex) {
				clearResources();
				if (!this.lastMessageSucceeded) {
					// We failed more than once in a row or on startup -
					// wait before first recovery attempt.
					waitBeforeRecoveryAttempt();
				}
				this.lastMessageSucceeded = false;
				boolean alreadyRecovered = false;
				synchronized (recoveryMonitor) {
					if (this.lastRecoveryMarker == currentRecoveryMarker) {
						handleListenerSetupFailure(ex, false);
						recoverAfterListenerSetupFailure();
						currentRecoveryMarker = new Object();
					}
					else {
						alreadyRecovered = true;
					}
				}
				if (alreadyRecovered) {
					handleListenerSetupFailure(ex, true);
				}
			}
			finally {
				synchronized (lifecycleMonitor) {
					decreaseActiveInvokerCount();
					lifecycleMonitor.notifyAll();
				}
				if (!messageReceived) {
					this.idleTaskExecutionCount++;
				}
				else {
					this.idleTaskExecutionCount = 0;
				}
				synchronized (lifecycleMonitor) {
					if (!shouldRescheduleInvoker(this.idleTaskExecutionCount) || !rescheduleTaskIfNecessary(this)) {
						// We're shutting down completely.
						scheduledInvokers.remove(this);
						if (logger.isDebugEnabled()) {
							logger.debug("Lowered scheduled invoker count: " + scheduledInvokers.size());
						}
						lifecycleMonitor.notifyAll();
						clearResources();
					}
					else if (isRunning()) {
						int nonPausedConsumers = getScheduledConsumerCount() - getPausedTaskCount();
						if (nonPausedConsumers < 1) {
							logger.error("All scheduled consumers have been paused, probably due to tasks having been rejected. " +
									"Check your thread pool configuration! Manual recovery necessary through a start() call.");
						}
						else if (nonPausedConsumers < getConcurrentConsumers()) {
							logger.warn("Number of scheduled consumers has dropped below concurrentConsumers limit, probably " +
									"due to tasks having been rejected. Check your thread pool configuration! Automatic recovery " +
									"to be triggered by remaining consumers.");
						}
					}
				}
			}
		}

		private boolean executeOngoingLoop() throws JMSException {
			boolean messageReceived = false;
			boolean active = true;
			while (active) {
				synchronized (lifecycleMonitor) {
					boolean interrupted = false;
					boolean wasWaiting = false;
					while ((active = isActive()) && !isRunning()) {
						if (interrupted) {
							throw new IllegalStateException("Thread was interrupted while waiting for " +
									"a restart of the listener container, but container is still stopped");
						}
						if (!wasWaiting) {
							decreaseActiveInvokerCount();
						}
						wasWaiting = true;
						try {
							lifecycleMonitor.wait();
						}
						catch (InterruptedException ex) {
							// Re-interrupt current thread, to allow other threads to react.
							Thread.currentThread().interrupt();
							interrupted = true;
						}
					}
					if (wasWaiting) {
						activeInvokerCount++;
					}
					if (scheduledInvokers.size() > maxConcurrentConsumers) {
						active = false;
					}
				}
				if (active) {
					messageReceived = (invokeListener() || messageReceived);
				}
			}
			return messageReceived;
		}

		private boolean invokeListener() throws JMSException {
			initResourcesIfNecessary();
			boolean messageReceived = receiveAndExecute(this, this.session, this.consumer);
			this.lastMessageSucceeded = true;
			return messageReceived;
		}

		private void decreaseActiveInvokerCount() {
			activeInvokerCount--;
			if (stopCallback != null && activeInvokerCount == 0) {
				stopCallback.run();
				stopCallback = null;
			}
		}

		private void initResourcesIfNecessary() throws JMSException {
			if (getCacheLevel() <= CACHE_CONNECTION) {
				updateRecoveryMarker();
			}
			else {
				if (this.session == null && getCacheLevel() >= CACHE_SESSION) {
					updateRecoveryMarker();
					this.session = createSession(getSharedConnection());
				}
				if (this.consumer == null && getCacheLevel() >= CACHE_CONSUMER) {
					this.consumer = createListenerConsumer(this.session);
					synchronized (lifecycleMonitor) {
						registeredWithDestination++;
					}
				}
			}
		}

		private void updateRecoveryMarker() {
			synchronized (recoveryMonitor) {
				this.lastRecoveryMarker = currentRecoveryMarker;
			}
		}

		private void clearResources() {
			if (sharedConnectionEnabled()) {
				synchronized (sharedConnectionMonitor) {
					JmsUtils.closeMessageConsumer(this.consumer);
					JmsUtils.closeSession(this.session);
				}
			}
			else {
				JmsUtils.closeMessageConsumer(this.consumer);
				JmsUtils.closeSession(this.session);
			}
			if (this.consumer != null) {
				synchronized (lifecycleMonitor) {
					registeredWithDestination--;
				}
			}
			this.consumer = null;
			this.session = null;
		}

		/**
		 * Apply the back-off time once. In a regular scenario, the back-off is only applied if we
		 * failed to recover with the broker. This additional wait period avoids a burst retry
		 * scenario when the broker is actually up but something else if failing (i.e. listener
		 * specific).
		 */
		private void waitBeforeRecoveryAttempt() {
			BackOffExecution execution = DefaultMessageListenerContainer.this.backOff.start();
			applyBackOffTime(execution);
		}

		@Override
		public boolean isLongLived() {
			return (maxMessagesPerTask < 0);
		}

		public void setIdle(boolean idle) {
			this.idle = idle;
		}

		public boolean isIdle() {
			return this.idle;
		}
	}

}
