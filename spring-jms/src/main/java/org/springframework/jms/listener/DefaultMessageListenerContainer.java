/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jms.listener;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.jspecify.annotations.Nullable;

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
 * transactional receipt of messages (registering them with XA transactions).
 * Designed to work in a native JMS environment as well as in a Jakarta EE environment,
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
 * thread pool facility (such as a Jakarta EE server's). With a native JMS setup,
 * each of those listener threads is going to use a cached JMS {@code Session} and
 * {@code MessageConsumer} (only refreshed in case of failure), using the JMS provider's
 * resources as efficiently as possible.
 *
 * <p>Message receipt and listener execution can automatically be wrapped
 * in transactions by passing a Spring
 * {@link org.springframework.transaction.PlatformTransactionManager} into the
 * {@link #setTransactionManager "transactionManager"} property. This will usually
 * be a {@link org.springframework.transaction.jta.JtaTransactionManager} in a
 * Jakarta EE environment, in combination with a JTA-aware JMS {@code ConnectionFactory}
 * obtained from JNDI (check your Jakarta EE server's documentation). Note that this
 * listener container will automatically reobtain all JMS handles for each transaction
 * in case an external transaction manager is specified, for compatibility with
 * all Jakarta EE servers (in particular JBoss). This non-caching behavior can be
 * overridden through the {@link #setCacheLevel "cacheLevel"} /
 * {@link #setCacheLevelName "cacheLevelName"} property, enforcing caching of
 * the {@code Connection} (or also {@code Session} and {@code MessageConsumer})
 * even if an external transaction manager is involved.
 *
 * <p>Dynamic scaling of the number of concurrent invokers can be activated
 * by specifying a {@link #setMaxConcurrentConsumers "maxConcurrentConsumers"}
 * value that is higher than the {@link #setConcurrentConsumers "concurrentConsumers"}
 * value. Since the latter's default is 1, you can also simply specify a
 * "maxConcurrentConsumers" of, for example, 5, which will lead to dynamic scaling up to
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
 * <p><b>Note: You may use {@link org.springframework.jms.connection.CachingConnectionFactory}
 * with a listener container but it comes with limitations.</b> It is generally preferable
 * to let the listener container itself handle appropriate caching within its lifecycle.
 * Also, stopping and restarting a listener container will only work with an independent,
 * locally cached {@code Connection}, not with an externally cached one. Last but not least,
 * with {@code CachingConnectionFactory}, dynamic scaling with custom provider hints such as
 * {@link #setMaxMessagesPerTask "maxMessagesPerTask"} can result in JMS messages delivered
 * to cached consumers even when they are no longer attached to the listener container.
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
 * @author Sam Brannen
 * @since 2.0
 * @see #setTransactionManager
 * @see #setCacheLevel
 * @see jakarta.jms.MessageConsumer#receive(long)
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


	/**
	 * Map of constant names to constant values for the cache constants defined
	 * in this class.
	 */
	private static final Map<String, Integer> constants = Map.of(
			"CACHE_NONE", CACHE_NONE,
			"CACHE_CONNECTION", CACHE_CONNECTION,
			"CACHE_SESSION", CACHE_SESSION,
			"CACHE_CONSUMER", CACHE_CONSUMER,
			"CACHE_AUTO", CACHE_AUTO
		);


	private @Nullable Executor taskExecutor;

	private boolean virtualThreads = false;

	private BackOff backOff = new FixedBackOff(DEFAULT_RECOVERY_INTERVAL);

	private int cacheLevel = CACHE_AUTO;

	private int concurrentConsumers = 1;

	private int maxConcurrentConsumers = 1;

	private int maxMessagesPerTask = Integer.MIN_VALUE;

	private int idleConsumerLimit = 1;

	private int idleTaskExecutionLimit = 1;

	private int idleReceivesPerTaskLimit = Integer.MIN_VALUE;

	private final Set<AsyncMessageListenerInvoker> scheduledInvokers = new HashSet<>();

	private int activeInvokerCount = 0;

	private int registeredWithDestination = 0;

	private volatile boolean recovering;

	private volatile boolean interrupted;

	private @Nullable Runnable stopCallback;

	private Object currentRecoveryMarker = new Object();

	private final Lock recoveryLock = new ReentrantLock();


	/**
	 * Set the Spring {@code TaskExecutor} to use for running the listener threads.
	 * <p>Default is a {@link org.springframework.core.task.SimpleAsyncTaskExecutor},
	 * starting up a number of new threads, according to the specified number
	 * of concurrent consumers.
	 * <p>Specify an alternative {@code TaskExecutor} for integration with an existing
	 * thread pool. Note that this really only adds value if the threads are
	 * managed in a specific fashion, for example within a Jakarta EE environment.
	 * A plain thread pool does not add much value, as this listener container
	 * will occupy a number of threads for its entire lifetime.
	 * <p>If the specified executor is a {@link SchedulingTaskExecutor} indicating
	 * {@link SchedulingTaskExecutor#prefersShortLivedTasks() a preference for
	 * short-lived tasks}, a {@link #setMaxMessagesPerTask} default of 10 will be
	 * applied in order to provide dynamic scaling at runtime. With the default
	 * task executor or a similarly non-pooling external executor specified,
	 * a {@link #setIdleReceivesPerTaskLimit} default of 10 will apply instead.
	 * @see #setConcurrentConsumers
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Specify whether the default {@link SimpleAsyncTaskExecutor} should be
	 * configured to use virtual threads instead of platform threads, for
	 * efficient blocking behavior in listener threads on Java 21 or higher.
	 * This is off by default, setting up one platform thread per consumer.
	 * <p>Only applicable if the internal default executor is in use rather than
	 * an externally provided {@link #setTaskExecutor TaskExecutor} instance.
	 * The thread name prefix for virtual threads will be derived from the
	 * listener container's bean name, just like with default platform threads.
	 * <p>Alternatively, pass in a virtual threads based executor through
	 * {@link #setTaskExecutor} (with externally defined thread naming).
	 * <p>Consider specifying concurrency limits through {@link #setConcurrency}
	 * or {@link #setConcurrentConsumers}/{@link #setMaxConcurrentConsumers},
	 * for potential dynamic scaling. This works fine with the default executor;
	 * see {@link #setIdleReceivesPerTaskLimit} with its effective default of 10.
	 * @since 6.2
	 * @see #setTaskExecutor
	 * @see SimpleAsyncTaskExecutor#setVirtualThreads
	 */
	public void setVirtualThreads(boolean virtualThreads) {
		this.virtualThreads = virtualThreads;
	}

	/**
	 * Specify the {@link BackOff} instance to use to compute the interval
	 * between recovery attempts. If the {@link BackOffExecution} implementation
	 * returns {@link BackOffExecution#STOP}, this listener container will not further
	 * attempt to recover.
	 * <p>Note that setting the {@linkplain #setRecoveryInterval(long) recovery
	 * interval} overrides this property.
	 * @since 4.1
	 */
	public void setBackOff(BackOff backOff) {
		this.backOff = backOff;
	}

	/**
	 * Specify the interval between recovery attempts, in <b>milliseconds</b>.
	 * <p>The default is 5000 ms, that is, 5 seconds.
	 * <p>This is a convenience method to create a {@link FixedBackOff} with the
	 * specified interval. For more recovery options, consider specifying a
	 * {@link #setBackOff(BackOff) BackOff} instance instead. Note, however, that
	 * explicitly setting the {@link #setBackOff(BackOff) BackOff} overrides this
	 * property.
	 * @see #setBackOff(BackOff)
	 * @see #handleListenerSetupFailure
	 */
	public void setRecoveryInterval(long recoveryInterval) {
		this.backOff = new FixedBackOff(recoveryInterval);
	}

	/**
	 * Specify the level of caching that this listener container is allowed to apply,
	 * in the form of the name of the corresponding constant &mdash; for example,
	 * {@code "CACHE_CONNECTION"}.
	 * @see #setCacheLevel
	 * @see #CACHE_NONE
	 * @see #CACHE_CONNECTION
	 * @see #CACHE_SESSION
	 * @see #CACHE_CONSUMER
	 * @see #CACHE_AUTO
	 */
	public void setCacheLevelName(String constantName) throws IllegalArgumentException {
		Assert.hasText(constantName, "'constantName' must not be null or blank");
		Integer cacheLevel = constants.get(constantName);
		Assert.notNull(cacheLevel, "Only cache constants allowed");
		this.cacheLevel = cacheLevel;
	}

	/**
	 * Specify the level of caching that this listener container is allowed to apply.
	 * <p>Default is {@link #CACHE_NONE} if an external transaction manager has been specified
	 * (to reobtain all resources freshly within the scope of the external transaction),
	 * and {@link #CACHE_CONSUMER} otherwise (operating with local JMS resources).
	 * <p>Some Jakarta EE servers only register their JMS resources with an ongoing XA
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
	 * @see #CACHE_AUTO
	 * @see #setCacheLevelName
	 * @see #setTransactionManager
	 */
	public void setCacheLevel(int cacheLevel) {
		Assert.isTrue(constants.containsValue(cacheLevel), "Only values of cache constants allowed");
		this.cacheLevel = cacheLevel;
	}

	/**
	 * Return the level of caching that this listener container is allowed to apply.
	 */
	public int getCacheLevel() {
		return this.cacheLevel;
	}


	/**
	 * Specify concurrency limits via a "lower-upper" String, for example, "5-10", or a simple
	 * upper limit String, for example, "10" (the lower limit will be 1 in this case).
	 * <p>This listener container will always hold on to the minimum number of consumers
	 * ({@link #setConcurrentConsumers}) and will slowly scale up to the maximum number
	 * of consumers {@link #setMaxConcurrentConsumers} in case of increasing load.
	 */
	@Override
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setConcurrentConsumers(Integer.parseInt(concurrency, 0, separatorIndex, 10));
				setMaxConcurrentConsumers(Integer.parseInt(concurrency, separatorIndex + 1, concurrency.length(), 10));
			}
			else {
				setConcurrentConsumers(1);
				setMaxConcurrentConsumers(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (for example, \"5\") and minimum-maximum combo (for example, \"3-5\") supported.");
		}
	}

	/**
	 * Specify the number of core concurrent consumers to create. Default is 1.
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
		this.lifecycleLock.lock();
		try {
			this.concurrentConsumers = concurrentConsumers;
			if (this.maxConcurrentConsumers < concurrentConsumers) {
				this.maxConcurrentConsumers = concurrentConsumers;
			}
		}
		finally {
			this.lifecycleLock.unlock();
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
		this.lifecycleLock.lock();
		try {
			return this.concurrentConsumers;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Specify the maximum number of concurrent consumers to create. Default is 1.
	 * <p>If this setting is higher than "concurrentConsumers", the listener container
	 * will dynamically schedule surplus consumers at runtime, provided that enough
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
		this.lifecycleLock.lock();
		try {
			this.maxConcurrentConsumers = Math.max(maxConcurrentConsumers, this.concurrentConsumers);
		}
		finally {
			this.lifecycleLock.unlock();
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
		this.lifecycleLock.lock();
		try {
			return this.maxConcurrentConsumers;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Specify the maximum number of messages to process in one task.
	 * More concretely, this limits the number of message receipt attempts
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
		this.lifecycleLock.lock();
		try {
			this.maxMessagesPerTask = maxMessagesPerTask;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Return the maximum number of messages to process in one task.
	 */
	public final int getMaxMessagesPerTask() {
		this.lifecycleLock.lock();
		try {
			return this.maxMessagesPerTask;
		}
		finally {
			this.lifecycleLock.unlock();
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
		this.lifecycleLock.lock();
		try {
			this.idleConsumerLimit = idleConsumerLimit;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Return the limit for the number of idle consumers.
	 */
	public final int getIdleConsumerLimit() {
		this.lifecycleLock.lock();
		try {
			return this.idleConsumerLimit;
		}
		finally {
			this.lifecycleLock.unlock();
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
	 * <p>Within each task execution, a number of message receipt attempts
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
		this.lifecycleLock.lock();
		try {
			this.idleTaskExecutionLimit = idleTaskExecutionLimit;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Return the limit for idle executions of a consumer task.
	 */
	public final int getIdleTaskExecutionLimit() {
		this.lifecycleLock.lock();
		try {
			return this.idleTaskExecutionLimit;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Marks the consumer as 'idle' after the specified number of idle receives
	 * have been reached. An idle receive is counted from the moment a null message
	 * is returned by the receiver after the potential {@link #setReceiveTimeout}
	 * elapsed. This gives the opportunity to check if the idle task count exceeds
	 * {@link #setIdleTaskExecutionLimit} and based on that decide if the task needs
	 * to be re-scheduled or not, saving resources that would otherwise be held.
	 * <p>This setting differs from {@link #setMaxMessagesPerTask} where the task is
	 * released and re-scheduled after this limit is reached, no matter if the received
	 * messages were null or non-null messages. This setting alone can be inflexible
	 * if one desires to have a large enough batch for each task but requires a
	 * quick(er) release from the moment there are no more messages to process.
	 * <p>This setting differs from {@link #setIdleTaskExecutionLimit} where this limit
	 * decides after how many iterations of being marked as idle, a task is released.
	 * <p>For example: If {@link #setMaxMessagesPerTask} is set to '500' and
	 * {@code #setIdleReceivesPerTaskLimit} is set to '60' and {@link #setReceiveTimeout}
	 * is set to '1000' and {@link #setIdleTaskExecutionLimit} is set to '1', then 500
	 * messages per task would be processed unless there is a subsequent number of 60
	 * idle messages received, the task would be marked as idle and released. This also
	 * means that after the last message was processed, the task would be released after
	 * 60 seconds as long as no new messages appear.
	 * <p><b>NOTE: On its own, this idle limit does not apply to core consumers within
	 * {@link #setConcurrentConsumers} but rather just to surplus consumers up until
	 * {@link #setMaxConcurrentConsumers} (as of 6.2).</b> Only in combination with
	 * {@link #setMaxMessagesPerTask} does it have an effect on core consumers as well,
	 * as inferred for an external thread pool indicating a preference for short-lived
	 * tasks, leading to dynamic rescheduling of all consumer tasks in the thread pool.
	 * <p><b>The default for surplus consumers on a default/simple executor is 10,
	 * leading to a removal of surplus tasks after 10 idle receives in each task.</b>
	 * In combination with the default {@link #setReceiveTimeout} of 1000 ms (1 second),
	 * a surplus task will be scaled down after 10 seconds of idle receives by default.
	 * @since 5.3.5
	 * @see #setMaxMessagesPerTask
	 * @see #setReceiveTimeout
	 */
	public void setIdleReceivesPerTaskLimit(int idleReceivesPerTaskLimit) {
		Assert.isTrue(idleReceivesPerTaskLimit != 0, "'idleReceivesPerTaskLimit' must not be 0)");
		this.lifecycleLock.lock();
		try {
			this.idleReceivesPerTaskLimit = idleReceivesPerTaskLimit;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Return the maximum number of subsequent null messages to receive in a single task
	 * before marking the consumer as 'idle'.
	 * @since 5.3.5
	 */
	public int getIdleReceivesPerTaskLimit() {
		this.lifecycleLock.lock();
		try {
			return this.idleReceivesPerTaskLimit;
		}
		finally {
			this.lifecycleLock.unlock();
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

		// Prepare taskExecutor and maxMessagesPerTask/idleReceivesPerTaskLimit.
		this.lifecycleLock.lock();
		try {
			if (this.taskExecutor == null) {
				this.taskExecutor = createDefaultTaskExecutor();
			}
			if (this.taskExecutor instanceof SchedulingTaskExecutor ste && ste.prefersShortLivedTasks()) {
				if (this.maxMessagesPerTask == Integer.MIN_VALUE) {
					// TaskExecutor indicated a preference for short-lived tasks. According to
					// setMaxMessagesPerTask javadoc, we'll use 10 message per task in this case
					// unless the user specified a custom value.
					this.maxMessagesPerTask = 10;
				}
			}
			else if (this.idleReceivesPerTaskLimit == Integer.MIN_VALUE) {
				// A simple non-pooling executor: unlimited core consumer tasks
				// whereas surplus consumer tasks terminate after 10 idle receives.
				this.idleReceivesPerTaskLimit = 10;
			}
		}
		finally {
			this.lifecycleLock.unlock();
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
		this.lifecycleLock.lock();
		try {
			for (int i = 0; i < this.concurrentConsumers; i++) {
				scheduleNewInvoker();
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Destroy the registered JMS Sessions and associated MessageConsumers.
	 */
	@Override
	protected void doShutdown() throws JMSException {
		logger.debug("Waiting for shutdown of message listener invokers");
		this.lifecycleLock.lock();
		try {
			long receiveTimeout = getReceiveTimeout();
			long waitStartTime = System.currentTimeMillis();
			int waitCount = 0;
			while (this.activeInvokerCount > 0) {
				if (waitCount > 0 && !isAcceptMessagesWhileStopping() &&
						System.currentTimeMillis() - waitStartTime >= receiveTimeout) {
					// Unexpectedly some invokers are still active after the receive timeout period
					// -> interrupt remaining receive attempts since we'd reject the messages anyway
					for (AsyncMessageListenerInvoker scheduledInvoker : this.scheduledInvokers) {
						scheduledInvoker.interruptIfNecessary();
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Still waiting for shutdown of " + this.activeInvokerCount +
							" message listener invokers (iteration " + waitCount + ")");
				}
				// Wait for AsyncMessageListenerInvokers to deactivate themselves...
				if (receiveTimeout > 0) {
					this.lifecycleCondition.await(receiveTimeout, TimeUnit.MILLISECONDS);
				}
				else {
					this.lifecycleCondition.await();
				}
				waitCount++;
			}
			// Clear remaining scheduled invokers, possibly left over as paused tasks
			for (AsyncMessageListenerInvoker scheduledInvoker : this.scheduledInvokers) {
				scheduledInvoker.clearResources();
			}
			this.scheduledInvokers.clear();
		}
		catch (InterruptedException ex) {
			// Re-interrupt current thread, to allow other threads to react.
			Thread.currentThread().interrupt();
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	/**
	 * Overridden to reset the stop callback, if any.
	 */
	@Override
	public void start() throws JmsException {
		this.lifecycleLock.lock();
		try {
			this.stopCallback = null;
		}
		finally {
			this.lifecycleLock.unlock();
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
		this.lifecycleLock.lock();
		try {
			if (!isRunning() || this.stopCallback != null) {
				// Not started, already stopped, or previous stop attempt in progress
				// -> return immediately, no stop process to control anymore.
				callback.run();
				return;
			}
			this.stopCallback = callback;
		}
		finally {
			this.lifecycleLock.unlock();
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
		this.lifecycleLock.lock();
		try {
			return this.scheduledInvokers.size();
		}
		finally {
			this.lifecycleLock.unlock();
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
		this.lifecycleLock.lock();
		try {
			return this.activeInvokerCount;
		}
		finally {
			this.lifecycleLock.unlock();
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
		this.lifecycleLock.lock();
		try {
			return (this.registeredWithDestination > 0);
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}


	/**
	 * Create a default TaskExecutor. Called if no explicit TaskExecutor has been specified.
	 * <p>The default implementation builds a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
	 * with the specified bean name (or the class name, if no bean name specified) as thread name prefix.
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
	 * @see #setVirtualThreads
	 */
	protected TaskExecutor createDefaultTaskExecutor() {
		String beanName = getBeanName();
		String threadNamePrefix = (beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);

		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(threadNamePrefix);
		executor.setVirtualThreads(this.virtualThreads);
		return executor;
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
		Assert.state(this.taskExecutor != null, "No TaskExecutor available");
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
			this.lifecycleLock.lock();
			try {
				if (this.scheduledInvokers.size() < this.maxConcurrentConsumers &&
						getIdleInvokerCount() < this.idleConsumerLimit) {
					scheduleNewInvoker();
					if (logger.isDebugEnabled()) {
						logger.debug("Raised scheduled invoker count: " + this.scheduledInvokers.size());
					}
				}
			}
			finally {
				this.lifecycleLock.unlock();
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
	 * Called to determine whether this listener container currently has more
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
			if (ex instanceof JMSException jmsException) {
				invokeExceptionListener(jmsException);
			}
			logger.debug("Could not establish shared JMS Connection - " +
					"leaving it up to asynchronous invokers to establish a Connection as soon as possible", ex);
		}
	}

	/**
	 * This implementation proceeds even after an exception thrown from
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
	 * This implementation proceeds even after an exception thrown from
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
		if (ex instanceof JMSException jmsException) {
			invokeExceptionListener(jmsException);
		}
		if (ex instanceof SharedConnectionNotInitializedException) {
			if (!alreadyRecovered) {
				logger.debug("JMS message listener invoker needs to establish shared Connection");
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
				msg.append(ex instanceof JMSException jmsException ? JmsUtils.buildExceptionMessage(jmsException) :
						ex.getMessage());
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
				logger.debug("Successfully refreshed JMS Connection");
				break;
			}
			catch (Exception ex) {
				if (ex instanceof JMSException jmsException) {
					invokeExceptionListener(jmsException);
				}
				StringBuilder msg = new StringBuilder();
				msg.append("Could not refresh JMS Connection for destination '");
				msg.append(getDestinationDescription()).append("' - retrying using ");
				msg.append(execution).append(". Cause: ");
				msg.append(ex instanceof JMSException jmsException ? JmsUtils.buildExceptionMessage(jmsException) :
						ex.getMessage());
				if (logger.isDebugEnabled()) {
					logger.error(msg, ex);
				}
				else {
					logger.error(msg);
				}
			}
			if (!applyBackOffTime(execution)) {
				logger.error("Stopping container for destination '" + getDestinationDescription() +
						"': back-off policy does not allow for further attempts.");
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
			if (destResolver instanceof CachingDestinationResolver cachingResolver) {
				cachingResolver.removeFromCache(destName);
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
			this.lifecycleLock.lock();
			try {
				this.lifecycleCondition.await(interval, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException interEx) {
				// Re-interrupt current thread, to allow other threads to react.
				Thread.currentThread().interrupt();
				if (this.recovering) {
					this.interrupted = true;
				}
			}
			finally {
				this.lifecycleLock.unlock();
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

		private @Nullable Session session;

		private @Nullable MessageConsumer consumer;

		private @Nullable Object lastRecoveryMarker;

		private boolean lastMessageSucceeded;

		private int idleTaskExecutionCount = 0;

		private volatile boolean idle = true;

		private volatile @Nullable Thread currentReceiveThread;

		@Override
		public void run() {
			boolean surplus;
			lifecycleLock.lock();
			try {
				surplus = (scheduledInvokers.size() > concurrentConsumers);
				activeInvokerCount++;
				lifecycleCondition.signalAll();
			}
			finally {
				lifecycleLock.unlock();
			}
			boolean messageReceived = false;
			try {
				// For core consumers without maxMessagesPerTask, no idle limit applies since they
				// will always get rescheduled immediately anyway. Whereas for surplus consumers
				// between concurrentConsumers and maxConcurrentConsumers, an idle limit does apply.
				int messageLimit = maxMessagesPerTask;
				int idleLimit = idleReceivesPerTaskLimit;
				if (messageLimit < 0 && (!surplus || idleLimit < 0)) {
					messageReceived = executeOngoingLoop();
				}
				else {
					int messageCount = 0;
					int idleCount = 0;
					while (isRunning() && (messageLimit < 0 || messageCount < messageLimit) &&
							(idleLimit < 0 || idleCount < idleLimit)) {
						boolean currentReceived = invokeListener();
						messageReceived |= currentReceived;
						messageCount++;
						idleCount = (currentReceived ? 0 : idleCount + 1);
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
				recoveryLock.lock();
				try {
					if (this.lastRecoveryMarker == currentRecoveryMarker) {
						handleListenerSetupFailure(ex, false);
						recoverAfterListenerSetupFailure();
						currentRecoveryMarker = new Object();
					}
					else {
						alreadyRecovered = true;
					}
				}
				finally {
					recoveryLock.unlock();
				}
				if (alreadyRecovered) {
					handleListenerSetupFailure(ex, true);
				}
			}
			finally {
				lifecycleLock.lock();
				try {
					decreaseActiveInvokerCount();
					lifecycleCondition.signalAll();
				}
				finally {
					lifecycleLock.unlock();
				}
				if (!messageReceived) {
					this.idleTaskExecutionCount++;
				}
				else {
					this.idleTaskExecutionCount = 0;
				}
				lifecycleLock.lock();
				try {
					if (!shouldRescheduleInvoker(this.idleTaskExecutionCount) || !rescheduleTaskIfNecessary(this)) {
						// We're shutting down completely.
						scheduledInvokers.remove(this);
						if (logger.isDebugEnabled()) {
							logger.debug("Lowered scheduled invoker count: " + scheduledInvokers.size());
						}
						lifecycleCondition.signalAll();
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
				finally {
					lifecycleLock.unlock();
				}
			}
		}

		private boolean executeOngoingLoop() throws JMSException {
			boolean messageReceived = false;
			boolean active = true;
			while (active) {
				lifecycleLock.lock();
				try {
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
							lifecycleCondition.await();
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
				finally {
					lifecycleLock.unlock();
				}
				if (active) {
					messageReceived = (invokeListener() || messageReceived);
				}
			}
			return messageReceived;
		}

		private boolean invokeListener() throws JMSException {
			this.currentReceiveThread = Thread.currentThread();
			try {
				initResourcesIfNecessary();
				boolean messageReceived = receiveAndExecute(this, this.session, this.consumer);
				this.lastMessageSucceeded = true;
				return messageReceived;
			}
			finally {
				this.currentReceiveThread = null;
			}
		}

		private void decreaseActiveInvokerCount() {
			activeInvokerCount--;
			if (activeInvokerCount == 0) {
				if (!isRunning()) {
					// Proactively release shared Connection when stopped.
					releaseSharedConnection();
				}
				if (stopCallback != null) {
					stopCallback.run();
					stopCallback = null;
				}
			}
		}

		@SuppressWarnings("NullAway") // Dataflow analysis limitation
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
					lifecycleLock.lock();
					try {
						registeredWithDestination++;
					}
					finally {
						lifecycleLock.unlock();
					}
				}
			}
		}

		private void updateRecoveryMarker() {
			recoveryLock.lock();
			try {
				this.lastRecoveryMarker = currentRecoveryMarker;
			}
			finally {
				recoveryLock.unlock();
			}
		}

		private void interruptIfNecessary() {
			Thread currentReceiveThread = this.currentReceiveThread;
			if (currentReceiveThread != null && !currentReceiveThread.isInterrupted()) {
				currentReceiveThread.interrupt();
			}
		}

		private void clearResources() {
			if (sharedConnectionEnabled()) {
				sharedConnectionLock.lock();
				try {
					JmsUtils.closeMessageConsumer(this.consumer);
					JmsUtils.closeSession(this.session);
				}
				finally {
					sharedConnectionLock.unlock();
				}
			}
			else {
				JmsUtils.closeMessageConsumer(this.consumer);
				JmsUtils.closeSession(this.session);
			}
			if (this.consumer != null) {
				lifecycleLock.lock();
				try {
					registeredWithDestination--;
				}
				finally {
					lifecycleLock.unlock();
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
