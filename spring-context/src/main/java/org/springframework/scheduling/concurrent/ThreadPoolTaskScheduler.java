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

package org.springframework.scheduling.concurrent;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * A standard implementation of Spring's {@link TaskScheduler} interface, wrapping
 * a native {@link java.util.concurrent.ScheduledThreadPoolExecutor} and providing
 * all applicable configuration options for it. The default number of scheduler
 * threads is 1; a higher number can be configured through {@link #setPoolSize}.
 *
 * <p>This is Spring's traditional scheduler variant, staying as close as possible to
 * {@link java.util.concurrent.ScheduledExecutorService} semantics. Task execution happens
 * on the scheduler thread(s) rather than on separate execution threads. As a consequence,
 * a {@link ScheduledFuture} handle (for example, from {@link #schedule(Runnable, Instant)})
 * represents the actual completion of the provided task (or series of repeated tasks).
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 * @see #setPoolSize
 * @see #setRemoveOnCancelPolicy
 * @see #setContinueExistingPeriodicTasksAfterShutdownPolicy
 * @see #setExecuteExistingDelayedTasksAfterShutdownPolicy
 * @see #setThreadFactory
 * @see #setErrorHandler
 * @see ThreadPoolTaskExecutor
 * @see SimpleAsyncTaskScheduler
 */
@SuppressWarnings({"serial", "deprecation"})
public class ThreadPoolTaskScheduler extends ExecutorConfigurationSupport
		implements AsyncTaskExecutor, SchedulingTaskExecutor, TaskScheduler, ThreadPoolTaskSchedulerMonitoringMBean {

	private static final TimeUnit NANO = TimeUnit.NANOSECONDS;


	private volatile int poolSize = 1;

	private volatile boolean removeOnCancelPolicy;

	private volatile boolean continueExistingPeriodicTasksAfterShutdownPolicy;

	private volatile boolean executeExistingDelayedTasksAfterShutdownPolicy = true;

	private @Nullable TaskDecorator taskDecorator;

	private volatile @Nullable ErrorHandler errorHandler;

	private Clock clock = Clock.systemDefaultZone();

	private @Nullable ScheduledExecutorService scheduledExecutor;

	private final AtomicReference<ScheduledExecutorService> delayMonitorExecutor = new AtomicReference<>();

	private boolean enableDelayMonitoring = true;

	private long delayMonitoringInterval = 5000;

	private long delayWarningThreshold = 1000;

	private int maxQueueCheckSize = 100;

	private boolean adaptiveQueueCheckSize = true;

	private long warningRateLimitMs = 30000;

	// Maximum warning rate limit (24 hours)
	private static final long MAX_WARNING_RATE_LIMIT_MS = 86400000;

	// Maximum number of timestamps to keep in sliding window (prevents memory leak)
	private static final int MAX_WARNING_TIMESTAMPS = 1000;

	// Sliding window for rate limiting (stores timestamps of recent warnings)
	private final Queue<Long> warningTimestamps = new LinkedList<>();

	private final Object warningTimestampsLock = new Object();

	// CPU monitoring constants
	private static final double CPU_USAGE_WARNING_THRESHOLD = 5.0;  // 5% CPU
	private static final long CPU_CHECK_INTERVAL_MS = 30000;  // 30 seconds

	// Deprecated - kept for compatibility
	@Deprecated
	private final AtomicLong lastWarningTime = new AtomicLong(0);

	private final AtomicInteger delayedTaskWarningCount = new AtomicInteger();

	// Monitoring metrics
	private final AtomicLong maxDelayMillis = new AtomicLong(0);

	private final AtomicInteger poolExhaustionCount = new AtomicInteger(0);

	// Circuit breaker for graceful degradation
	/**
	 * Circuit breaker states.
	 */
	private enum CircuitBreakerState {
		CLOSED,    // Normal operation
		OPEN,      // Monitoring disabled due to errors
		HALF_OPEN  // Testing if errors are resolved
	}

	private final AtomicReference<CircuitBreakerState> circuitBreakerState =
			new AtomicReference<>(CircuitBreakerState.CLOSED);

	private final AtomicInteger monitoringErrorCount = new AtomicInteger(0);

	private static final int CIRCUIT_BREAKER_THRESHOLD = 5;

	private static final long CIRCUIT_BREAKER_RESET_MS = 60000;

	private static final int HALF_OPEN_MAX_CALLS = 3;  // Test 3 calls before closing

	private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);

	private final AtomicLong circuitBreakerOpenTime = new AtomicLong(0);

	// Deprecated - kept for compatibility
	@Deprecated
	private final AtomicBoolean circuitBreakerOpen = new AtomicBoolean(false);

	// CPU monitoring for monitoring thread
	private volatile long monitorThreadId = -1;

	private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

	private volatile long lastMonitorCpuTime = 0;

	private volatile long lastCpuCheckTime = 0;

	// Logging configuration
	public enum WarningLogLevel {
		DEBUG, INFO, WARN, ERROR
	}

	private volatile WarningLogLevel warningLogLevel = WarningLogLevel.WARN;

	private volatile boolean structuredLoggingEnabled = false;


	/**
	 * Enable or disable task delay monitoring.
	 * <p>When enabled (default), a separate monitoring thread will periodically check
	 * if scheduled tasks are unable to execute due to thread pool exhaustion and log warnings.
	 * <p>This helps diagnose situations where the pool size is insufficient for the workload.
	 * @param enableDelayMonitoring whether to enable delay monitoring
	 * @since 6.2
	 * @see #setDelayMonitoringInterval
	 * @see #setDelayWarningThreshold
	 */
	public void setEnableDelayMonitoring(boolean enableDelayMonitoring) {
		this.enableDelayMonitoring = enableDelayMonitoring;
	}

	/**
	 * Return whether delay monitoring is currently enabled.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @return true if monitoring is enabled, false otherwise
	 * @since 6.2
	 */
	@Override
	public boolean isDelayMonitoringEnabled() {
		return this.enableDelayMonitoring;
	}

	/**
	 * Enable or disable delay monitoring at runtime.
	 * <p>If the scheduler is already initialized, this will start or stop
	 * the monitoring thread dynamically.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @param enabled whether to enable delay monitoring
	 * @since 6.2
	 */
	@Override
	public void setDelayMonitoringEnabled(boolean enabled) {
		boolean wasEnabled = this.enableDelayMonitoring;
		this.enableDelayMonitoring = enabled;

		// If scheduler is initialized, start/stop monitoring dynamically
		if (this.scheduledExecutor != null && wasEnabled != enabled) {
			if (enabled) {
				if (logger.isInfoEnabled()) {
					logger.info("Enabling delay monitoring at runtime");
				}
				startDelayMonitor(this.scheduledExecutor);
			}
			else {
				if (logger.isInfoEnabled()) {
					logger.info("Disabling delay monitoring at runtime");
				}
				stopDelayMonitor();
			}
		}
	}

	/**
	 * Return the current monitoring interval in milliseconds.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @return the monitoring interval
	 * @since 6.2
	 */
	@Override
	public long getDelayMonitoringInterval() {
		return this.delayMonitoringInterval;
	}

	/**
	 * Set the interval for checking delayed tasks (in milliseconds).
	 * <p>Default is 5000ms (5 seconds).
	 * <p>If monitoring is active and the scheduler is initialized,
	 * this will restart the monitoring thread with the new interval.
	 * @param delayMonitoringInterval the monitoring interval in milliseconds
	 * @since 6.2
	 */
	public void setDelayMonitoringInterval(long delayMonitoringInterval) {
		Assert.isTrue(delayMonitoringInterval > 0, "delayMonitoringInterval must be positive");
		long oldInterval = this.delayMonitoringInterval;
		this.delayMonitoringInterval = delayMonitoringInterval;

		// If monitoring is active and interval changed, restart monitoring with new interval
		if (this.scheduledExecutor != null && this.enableDelayMonitoring && oldInterval != delayMonitoringInterval) {
			if (logger.isDebugEnabled()) {
				logger.debug("Restarting delay monitoring with new interval: " + delayMonitoringInterval + "ms");
			}
			stopDelayMonitor();
			startDelayMonitor(this.scheduledExecutor);
		}
	}

	/**
	 * Return the current delay warning threshold in milliseconds.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @return the warning threshold
	 * @since 6.2
	 */
	@Override
	public long getDelayWarningThreshold() {
		return this.delayWarningThreshold;
	}

	/**
	 * Set the threshold for logging warnings about delayed tasks (in milliseconds).
	 * <p>Tasks that are delayed by more than this threshold will trigger a warning.
	 * <p>Default is 1000ms (1 second).
	 * @param delayWarningThreshold the warning threshold in milliseconds
	 * @since 6.2
	 */
	public void setDelayWarningThreshold(long delayWarningThreshold) {
		Assert.isTrue(delayWarningThreshold > 0, "delayWarningThreshold must be positive");
		this.delayWarningThreshold = delayWarningThreshold;
	}

	/**
	 * Reset the rate limit timer for delay warnings.
	 * <p>This is primarily useful for testing to allow immediate warnings without waiting
	 * for the rate limit period to expire.
	 * <p>Clears both the deprecated lastWarningTime field and the sliding window queue.
	 * @since 6.2
	 */
	void resetWarningRateLimit() {
		this.lastWarningTime.set(0);
		synchronized (this.warningTimestampsLock) {
			this.warningTimestamps.clear();
		}
	}

	/**
	 * Set the maximum queue size to check in detail before skipping to summary logging.
	 * <p>This is primarily useful for testing.
	 * @param maxQueueCheckSize the maximum queue size to check in detail
	 * @since 6.2
	 */
	void setMaxQueueCheckSize(int maxQueueCheckSize) {
		Assert.isTrue(maxQueueCheckSize > 0, "maxQueueCheckSize must be positive");
		this.maxQueueCheckSize = maxQueueCheckSize;
	}

	/**
	 * Set the warning rate limit in milliseconds.
	 * <p>This is primarily useful for testing.
	 * <p>Maximum value is 24 hours (86400000ms) to prevent unbounded memory growth in sliding window.
	 * @param warningRateLimitMs the rate limit in milliseconds
	 * @since 6.2
	 */
	void setWarningRateLimitMs(long warningRateLimitMs) {
		Assert.isTrue(warningRateLimitMs >= 0 && warningRateLimitMs <= MAX_WARNING_RATE_LIMIT_MS,
				"warningRateLimitMs must be between 0 and " + MAX_WARNING_RATE_LIMIT_MS + " (24 hours)");
		this.warningRateLimitMs = warningRateLimitMs;
	}

	/**
	 * Enable or disable adaptive queue check size.
	 * <p>When enabled, the max queue check size adjusts based on queue size and performance.
	 * <p>Default is true.
	 * @param enabled whether to enable adaptive queue check size
	 * @since 6.2
	 */
	public void setAdaptiveQueueCheckSize(boolean enabled) {
		this.adaptiveQueueCheckSize = enabled;
	}

	/**
	 * Return whether adaptive queue check size is enabled.
	 * @return true if enabled, false otherwise
	 * @since 6.2
	 */
	public boolean isAdaptiveQueueCheckSize() {
		return this.adaptiveQueueCheckSize;
	}

	/**
	 * Return the current warning log level.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @return the log level (DEBUG, INFO, WARN, or ERROR)
	 * @since 6.2
	 */
	@Override
	public String getWarningLogLevel() {
		return this.warningLogLevel.name();
	}

	/**
	 * Set the log level for delay warnings.
	 * <p>Default is WARN.
	 * @param level the log level (DEBUG, INFO, WARN, or ERROR)
	 * @since 6.2
	 */
	public void setWarningLogLevel(WarningLogLevel level) {
		Assert.notNull(level, "WarningLogLevel must not be null");
		this.warningLogLevel = level;
	}

	/**
	 * Set the log level for delay warnings from a string.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @param level the log level string (DEBUG, INFO, WARN, or ERROR)
	 * @since 6.2
	 */
	@Override
	public void setWarningLogLevel(String level) {
		Assert.hasText(level, "Log level must not be empty");
		try {
			this.warningLogLevel = WarningLogLevel.valueOf(level.toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid log level: " + level +
					". Valid values are: DEBUG, INFO, WARN, ERROR");
		}
	}

	/**
	 * Return whether structured logging is enabled.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @return true if structured logging is enabled, false otherwise
	 * @since 6.2
	 */
	@Override
	public boolean isStructuredLoggingEnabled() {
		return this.structuredLoggingEnabled;
	}

	/**
	 * Enable or disable structured logging (JSON format).
	 * <p>When enabled, warnings will be logged in JSON format for better indexing
	 * in centralized logging systems (ELK, Splunk, etc.).
	 * <p>Default is false.
	 * @param enabled whether to enable structured logging
	 * @since 6.2
	 */
	public void setStructuredLoggingEnabled(boolean enabled) {
		this.structuredLoggingEnabled = enabled;
	}

	/**
	 * Return the maximum delay observed for any task (in milliseconds).
	 * <p>This metric can be used for monitoring and alerting.
	 * @return the maximum delay in milliseconds
	 * @since 6.2
	 */
	public long getMaxDelayMillis() {
		return this.maxDelayMillis.get();
	}

	/**
	 * Return the number of times pool exhaustion has been detected.
	 * <p>This metric can be used for monitoring and alerting.
	 * @return the count of pool exhaustion events
	 * @since 6.2
	 */
	public int getPoolExhaustionCount() {
		return this.poolExhaustionCount.get();
	}

	/**
	 * Return the current queue size of the scheduler.
	 * <p>This metric can be used for monitoring.
	 * @return the current queue size
	 * @since 6.2
	 */
	public int getQueueSize() {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor executor) {
			return executor.getQueue().size();
		}
		return 0;
	}

	/**
	 * Return whether the circuit breaker is currently open.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @return true if circuit breaker is open or half-open, false if closed
	 * @since 6.2
	 */
	@Override
	public boolean isCircuitBreakerOpen() {
		CircuitBreakerState state = this.circuitBreakerState.get();
		// Update deprecated field for backwards compatibility
		this.circuitBreakerOpen.set(state != CircuitBreakerState.CLOSED);
		return state != CircuitBreakerState.CLOSED;
	}

	/**
	 * Return the current circuit breaker state as a string.
	 * @return "CLOSED", "OPEN", or "HALF_OPEN"
	 * @since 6.2
	 */
	public String getCircuitBreakerState() {
		CircuitBreakerState state = this.circuitBreakerState.get();
		return (state != null ? state.name() : CircuitBreakerState.CLOSED.name());
	}

	/**
	 * Reset all monitoring metrics.
	 * <p>This is useful for testing or when starting a new monitoring period.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @since 6.2
	 */
	@Override
	public void resetMonitoringMetrics() {
		this.maxDelayMillis.set(0);
		this.poolExhaustionCount.set(0);
		this.delayedTaskWarningCount.set(0);
		this.monitoringErrorCount.set(0);
		this.circuitBreakerState.set(CircuitBreakerState.CLOSED);
		this.circuitBreakerOpen.set(false);  // Deprecated field
		this.circuitBreakerOpenTime.set(0);
		this.halfOpenSuccessCount.set(0);
		if (logger.isDebugEnabled()) {
			logger.debug("Monitoring metrics reset");
		}
	}

	/**
	 * Reset the circuit breaker state.
	 * <p>This allows monitoring to resume immediately without waiting for the cool-down period.
	 * <p>Part of {@link ThreadPoolTaskSchedulerMonitoringMBean} interface.
	 * @since 6.2
	 */
	@Override
	public void resetCircuitBreaker() {
		this.circuitBreakerState.set(CircuitBreakerState.CLOSED);
		this.circuitBreakerOpen.set(false);  // Deprecated field
		this.circuitBreakerOpenTime.set(0);
		this.monitoringErrorCount.set(0);
		this.halfOpenSuccessCount.set(0);
		if (logger.isInfoEnabled()) {
			logger.info("Circuit breaker manually reset - monitoring resumed");
		}
	}

	/**
	 * Set the ScheduledExecutorService's pool size.
	 * Default is 1.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			threadPoolExecutor.setCorePoolSize(poolSize);
		}
		this.poolSize = poolSize;
	}

	/**
	 * Set the remove-on-cancel mode on {@link ScheduledThreadPoolExecutor}.
	 * <p>Default is {@code false}. If set to {@code true}, the target executor will be
	 * switched into remove-on-cancel mode (if possible).
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @see ScheduledThreadPoolExecutor#setRemoveOnCancelPolicy
	 */
	public void setRemoveOnCancelPolicy(boolean flag) {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			threadPoolExecutor.setRemoveOnCancelPolicy(flag);
		}
		this.removeOnCancelPolicy = flag;
	}

	/**
	 * Set whether to continue existing periodic tasks even when this executor has been shutdown.
	 * <p>Default is {@code false}. If set to {@code true}, the target executor will be
	 * switched into continuing periodic tasks (if possible).
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @since 5.3.9
	 * @see ScheduledThreadPoolExecutor#setContinueExistingPeriodicTasksAfterShutdownPolicy
	 */
	public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean flag) {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			threadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(flag);
		}
		this.continueExistingPeriodicTasksAfterShutdownPolicy = flag;
	}

	/**
	 * Set whether to execute existing delayed tasks even when this executor has been shutdown.
	 * <p>Default is {@code true}. If set to {@code false}, the target executor will be
	 * switched into dropping remaining tasks (if possible).
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 * @since 5.3.9
	 * @see ScheduledThreadPoolExecutor#setExecuteExistingDelayedTasksAfterShutdownPolicy
	 */
	public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean flag) {
		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			threadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(flag);
		}
		this.executeExistingDelayedTasksAfterShutdownPolicy = flag;
	}

	/**
	 * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
	 * about to be executed.
	 * <p>Note that such a decorator is not being applied to the user-supplied
	 * {@code Runnable}/{@code Callable} but rather to the scheduled execution
	 * callback (a wrapper around the user-supplied task).
	 * <p>The primary use case is to set some execution context around the task's
	 * invocation, or to provide some monitoring/statistics for task execution.
	 * @since 6.2
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	/**
	 * Set a custom {@link ErrorHandler} strategy.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Set the clock to use for scheduling purposes.
	 * <p>The default clock is the system clock for the default time zone.
	 * @since 5.3
	 * @see Clock#systemDefaultZone()
	 */
	public void setClock(Clock clock) {
		Assert.notNull(clock, "Clock must not be null");
		this.clock = clock;
	}

	@Override
	public Clock getClock() {
		return this.clock;
	}


	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		this.scheduledExecutor = createExecutor(this.poolSize, threadFactory, rejectedExecutionHandler);

		if (this.scheduledExecutor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
			if (this.removeOnCancelPolicy) {
				threadPoolExecutor.setRemoveOnCancelPolicy(true);
			}
			if (this.continueExistingPeriodicTasksAfterShutdownPolicy) {
				threadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
			}
			if (!this.executeExistingDelayedTasksAfterShutdownPolicy) {
				threadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
			}
		}

		if (this.enableDelayMonitoring) {
			startDelayMonitor(this.scheduledExecutor);
		}

		return this.scheduledExecutor;
	}

	/**
	 * Create a new {@link ScheduledExecutorService} instance.
	 * <p>The default implementation creates a {@link ScheduledThreadPoolExecutor}.
	 * Can be overridden in subclasses to provide custom {@link ScheduledExecutorService} instances.
	 * @param poolSize the specified pool size
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ScheduledExecutorService instance
	 * @see #afterPropertiesSet()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor
	 */
	protected ScheduledExecutorService createExecutor(
			int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler) {
			@Override
			protected void beforeExecute(Thread thread, Runnable task) {
				ThreadPoolTaskScheduler.this.beforeExecute(thread, task);
			}
			@Override
			protected void afterExecute(Runnable task, Throwable ex) {
				ThreadPoolTaskScheduler.this.afterExecute(task, ex);
			}
			@Override
			protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
				return decorateTaskIfNecessary(task);
			}
			@Override
			protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
				return decorateTaskIfNecessary(task);
			}
		};
	}

	/**
	 * Return the underlying ScheduledExecutorService for native access.
	 * @return the underlying ScheduledExecutorService (never {@code null})
	 * @throws IllegalStateException if the ThreadPoolTaskScheduler hasn't been initialized yet
	 */
	public ScheduledExecutorService getScheduledExecutor() throws IllegalStateException {
		Assert.state(this.scheduledExecutor != null, "ThreadPoolTaskScheduler not initialized");
		return this.scheduledExecutor;
	}

	/**
	 * Return the underlying ScheduledThreadPoolExecutor, if available.
	 * @return the underlying ScheduledExecutorService (never {@code null})
	 * @throws IllegalStateException if the ThreadPoolTaskScheduler hasn't been initialized yet
	 * or if the underlying ScheduledExecutorService isn't a ScheduledThreadPoolExecutor
	 * @see #getScheduledExecutor()
	 */
	public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.scheduledExecutor instanceof ScheduledThreadPoolExecutor,
				"No ScheduledThreadPoolExecutor available");
		return (ScheduledThreadPoolExecutor) this.scheduledExecutor;
	}

	/**
	 * Return the current pool size.
	 * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
	 * @see #getScheduledThreadPoolExecutor()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		if (this.scheduledExecutor == null) {
			// Not initialized yet: assume initial pool size.
			return this.poolSize;
		}
		return getScheduledThreadPoolExecutor().getPoolSize();
	}

	/**
	 * Return the number of currently active threads.
	 * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
	 * @see #getScheduledThreadPoolExecutor()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		if (this.scheduledExecutor == null) {
			// Not initialized yet: assume no active threads.
			return 0;
		}
		return getScheduledThreadPoolExecutor().getActiveCount();
	}

	/**
	 * Return the current setting for the remove-on-cancel mode.
	 * <p>Requires an underlying {@link ScheduledThreadPoolExecutor}.
	 * @deprecated in favor of direct {@link #getScheduledThreadPoolExecutor()} access
	 */
	@Deprecated(since = "5.3.9")
	public boolean isRemoveOnCancelPolicy() {
		if (this.scheduledExecutor == null) {
			// Not initialized yet: return our setting for the time being.
			return this.removeOnCancelPolicy;
		}
		return getScheduledThreadPoolExecutor().getRemoveOnCancelPolicy();
	}


	// SchedulingTaskExecutor implementation

	@Override
	public void execute(Runnable task) {
		Executor executor = getScheduledExecutor();
		try {
			executor.execute(errorHandlingTask(task, false));
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public Future<?> submit(Runnable task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			return executor.submit(errorHandlingTask(task, false));
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getScheduledExecutor();
		try {
			return executor.submit(new DelegatingErrorHandlingCallable<>(task, this.errorHandler));
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}


	// TaskScheduler implementation

	@Override
	public @Nullable ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			ErrorHandler errorHandler = this.errorHandler;
			if (errorHandler == null) {
				errorHandler = TaskUtils.getDefaultErrorHandler(true);
			}
			return new ReschedulingRunnable(task, trigger, this.clock, executor, errorHandler).schedule();
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		ScheduledExecutorService executor = getScheduledExecutor();
		Duration delay = Duration.between(this.clock.instant(), startTime);
		try {
			return executor.schedule(errorHandlingTask(task, false), NANO.convert(delay), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		Duration initialDelay = Duration.between(this.clock.instant(), startTime);
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true),
					NANO.convert(initialDelay), NANO.convert(period), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleAtFixedRate(errorHandlingTask(task, true),
					0, NANO.convert(period), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		Duration initialDelay = Duration.between(this.clock.instant(), startTime);
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true),
					NANO.convert(initialDelay), NANO.convert(delay), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		ScheduledExecutorService executor = getScheduledExecutor();
		try {
			return executor.scheduleWithFixedDelay(errorHandlingTask(task, true),
					0, NANO.convert(delay), NANO);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(executor, task, ex);
		}
	}

	/**
	 * Start the delay monitoring thread that periodically checks for tasks
	 * that are delayed due to thread pool exhaustion.
	 * @param executor the scheduled executor to monitor
	 * @since 6.2
	 */
	private void startDelayMonitor(ScheduledExecutorService executor) {
		if (!(executor instanceof ScheduledThreadPoolExecutor)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Delay monitoring is only supported for ScheduledThreadPoolExecutor");
			}
			return;
		}

		// Stop existing monitor if running
		stopDelayMonitor();

		ScheduledExecutorService newMonitor = Executors.newSingleThreadScheduledExecutor(runnable -> {
			String prefix = getThreadNamePrefix();
			String threadName = (prefix != null ? prefix : "") + "delay-monitor";
			Thread thread = new Thread(runnable, threadName);
			thread.setDaemon(true);
			this.monitorThreadId = thread.getId();
			return thread;
		});

		newMonitor.scheduleAtFixedRate(
				() -> checkForDelayedTasks((ScheduledThreadPoolExecutor) executor),
				this.delayMonitoringInterval,
				this.delayMonitoringInterval,
				TimeUnit.MILLISECONDS);

		this.delayMonitorExecutor.set(newMonitor);

		if (logger.isDebugEnabled()) {
			logger.debug("Started delay monitoring thread with interval: " + this.delayMonitoringInterval + "ms");
		}
	}

	/**
	 * Stop the delay monitoring thread.
	 * Lock-free thread-safe method to prevent concurrent shutdown attempts.
	 * Uses AtomicReference.getAndSet() for atomic shutdown without locks.
	 * @since 6.2
	 */
	private void stopDelayMonitor() {
		ScheduledExecutorService executor = this.delayMonitorExecutor.getAndSet(null);
		if (executor != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Stopping delay monitoring thread");
			}
			executor.shutdownNow();
			this.monitorThreadId = -1;
		}
	}

	/**
	 * Pool state information record.
	 */
	private record PoolState(int poolSize, int activeCount, int queueSize, boolean poolExhausted) {}

	/**
	 * Delay analysis result record.
	 */
	private record DelayAnalysis(int delayedCount, long maxDelay) {}

	/**
	 * Check the task queue for tasks whose scheduled execution time has passed
	 * but have not yet started executing due to thread pool exhaustion.
	 * @param executor the scheduled thread pool executor to monitor
	 * @since 6.2
	 */
	private void checkForDelayedTasks(ScheduledThreadPoolExecutor executor) {
		try {
			// Circuit breaker check
			if (!checkCircuitBreaker()) {
				return;
			}

			// Monitor CPU usage of monitoring thread itself
			monitorCpuUsage();

			BlockingQueue<Runnable> queue = executor.getQueue();
			if (queue.isEmpty()) {
				return;
			}

			PoolState poolState = capturePoolState(executor);

			if (!poolState.poolExhausted()) {
				// No exhaustion, no need to check
				return;
			}

			// Rate limiting check
			if (!shouldLogWarning()) {
				return;
			}

			// For large queues, skip detailed iteration and warn immediately
			if (poolState.queueSize() > this.maxQueueCheckSize) {
				handleLargeQueue(poolState);
				return;
			}

			// Analyze delayed tasks
			DelayAnalysis analysis = analyzeDelayedTasks(queue);

			// Update metrics and log if needed
			if (analysis.delayedCount() > 0) {
				recordDelayMetrics(analysis.maxDelay());
				this.poolExhaustionCount.incrementAndGet();
				logWarning(buildDelayedTasksMessage(analysis.delayedCount(), analysis.maxDelay(),
						poolState.poolSize(), poolState.activeCount(), poolState.queueSize()));
				delayedTaskWarningCount.addAndGet(analysis.delayedCount());
			}

			// Record successful monitoring execution for circuit breaker state machine
			recordMonitoringSuccess();
		}
		catch (Exception ex) {
			handleMonitoringError(ex);
		}
	}

	/**
	 * Check circuit breaker status and potentially transition to HALF_OPEN or CLOSED state.
	 * Uses atomic CAS operations to prevent race conditions in state transitions.
	 * @return true if monitoring should continue, false if circuit breaker is OPEN
	 */
	private boolean checkCircuitBreaker() {
		CircuitBreakerState currentState = this.circuitBreakerState.get();
		if (currentState == null) {
			return true;  // Defensive: treat null as CLOSED
		}

		switch (currentState) {
			case CLOSED:
				// Normal operation
				return true;

			case OPEN:
				// Check if cool-down period has passed
				long now = System.currentTimeMillis();
				long openTime = this.circuitBreakerOpenTime.get();
				if (now - openTime >= CIRCUIT_BREAKER_RESET_MS) {
					// Atomic transition to HALF_OPEN state using CAS
					// Only ONE thread will successfully transition from OPEN to HALF_OPEN
					if (this.circuitBreakerState.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
						this.halfOpenSuccessCount.set(0);
						if (logger.isInfoEnabled()) {
							logger.info("Circuit breaker transitioning to HALF_OPEN - testing if errors are resolved");
						}
					}
					return true;  // Allow monitoring in HALF_OPEN state
				}
				return false; // Still in cool-down period

			case HALF_OPEN:
				// Allow monitoring but track success/failure to decide next state
				return true;

			default:
				return false;
		}
	}

	/**
	 * Capture current pool state.
	 */
	private PoolState capturePoolState(ScheduledThreadPoolExecutor executor) {
		int poolSize = executor.getPoolSize();
		int activeCount = executor.getActiveCount();
		int queueSize = executor.getQueue().size();
		boolean poolExhausted = (activeCount >= poolSize);
		return new PoolState(poolSize, activeCount, queueSize, poolExhausted);
	}

	/**
	 * Check rate limiting using a sliding window - should we log a warning now?
	 * Uses sliding window approach: allows one warning per warningRateLimitMs window.
	 * More accurate than fixed window as it prevents burst warnings at window boundaries.
	 * Implements bounded queue to prevent memory leak.
	 * @return true if warning should be logged, false if rate limited
	 */
	private boolean shouldLogWarning() {
		long now = System.currentTimeMillis();

		synchronized (this.warningTimestampsLock) {
			// Remove timestamps outside the sliding window
			long windowStart = now - this.warningRateLimitMs;
			while (!this.warningTimestamps.isEmpty() && this.warningTimestamps.peek() < windowStart) {
				this.warningTimestamps.poll();
			}

			// Check if we're within rate limit
			if (!this.warningTimestamps.isEmpty()) {
				// We have recent warnings, check if we should allow this one
				// Allow max 1 warning per window
				return false;
			}

			// Bounded queue check: prevent memory leak
			// If we've reached the maximum, remove oldest entry
			if (this.warningTimestamps.size() >= MAX_WARNING_TIMESTAMPS) {
				this.warningTimestamps.poll();
			}

			// No recent warnings, allow this one and record timestamp
			this.warningTimestamps.offer(now);

			// Also update old lastWarningTime for backwards compatibility
			this.lastWarningTime.set(now);

			return true;
		}
	}

	/**
	 * Handle large queue scenario (skip detailed iteration).
	 */
	private void handleLargeQueue(PoolState poolState) {
		this.poolExhaustionCount.incrementAndGet();
		logWarning(buildLargeQueueMessage(poolState.queueSize(), poolState.poolSize(), poolState.activeCount()));
		delayedTaskWarningCount.incrementAndGet();
	}

	/**
	 * Calculate adaptive max queue check size based on current queue size.
	 * Scales between 10 (for small queues) and maxQueueCheckSize (for large queues).
	 * @param queueSize current queue size
	 * @return adaptive limit
	 */
	private int calculateAdaptiveLimit(int queueSize) {
		if (!this.adaptiveQueueCheckSize) {
			return this.maxQueueCheckSize;
		}

		// Scale adaptively:
		// - Queue <= 20: check 10 tasks (low overhead)
		// - Queue <= 50: check 25 tasks
		// - Queue <= 100: check 50 tasks
		// - Queue > 100: check maxQueueCheckSize tasks
		if (queueSize <= 20) {
			return Math.min(10, this.maxQueueCheckSize);
		}
		else if (queueSize <= 50) {
			return Math.min(25, this.maxQueueCheckSize);
		}
		else if (queueSize <= 100) {
			return Math.min(50, this.maxQueueCheckSize);
		}
		else {
			return this.maxQueueCheckSize;
		}
	}

	/**
	 * Analyze queue to find delayed tasks.
	 * @return DelayAnalysis with count and max delay
	 */
	private DelayAnalysis analyzeDelayedTasks(BlockingQueue<Runnable> queue) {
		int delayedCount = 0;
		long maxDelay = 0;
		int checked = 0;
		int adaptiveLimit = calculateAdaptiveLimit(queue.size());

		for (Runnable runnable : queue) {
			// Safety limit: don't check more than adaptive limit
			if (++checked > adaptiveLimit) {
				break;
			}

			if (runnable instanceof RunnableScheduledFuture<?> future) {
				// getDelay() returns time until scheduled execution:
				//   POSITIVE = task scheduled in the future (not yet time)
				//   ZERO     = task should execute now
				//   NEGATIVE = task is OVERDUE (missed its scheduled time)
				long delayMs = future.getDelay(TimeUnit.MILLISECONDS);

				// Task is overdue by more than threshold AND pool is exhausted = thread starvation
				// Example: delayMs = -3000 means task was supposed to run 3 seconds ago
				if (delayMs < -this.delayWarningThreshold) {
					delayedCount++;
					long delayedBy = Math.abs(delayMs);  // Convert to positive for reporting
					maxDelay = Math.max(maxDelay, delayedBy);
				}
			}
		}

		return new DelayAnalysis(delayedCount, maxDelay);
	}

	/**
	 * Record delay metrics in a thread-safe manner.
	 */
	private void recordDelayMetrics(long maxDelay) {
		if (maxDelay > 0) {
			this.maxDelayMillis.getAndUpdate(current -> Math.max(current, maxDelay));
		}
	}

	/**
	 * Handle monitoring error and potentially transition circuit breaker state.
	 * Uses atomic CAS operations to prevent race conditions in state transitions.
	 */
	private void handleMonitoringError(Exception ex) {
		CircuitBreakerState currentState = this.circuitBreakerState.get();

		if (currentState == CircuitBreakerState.HALF_OPEN) {
			// Error in HALF_OPEN state - atomically transition back to OPEN
			// CAS ensures only one thread transitions the state
			if (this.circuitBreakerState.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.OPEN)) {
				this.circuitBreakerOpenTime.set(System.currentTimeMillis());
				this.circuitBreakerOpen.set(true);  // Deprecated field
				if (logger.isWarnEnabled()) {
					logger.warn("Circuit breaker reopened after error in HALF_OPEN state - " +
							"monitoring suspended for " + (CIRCUIT_BREAKER_RESET_MS / 1000) + " seconds", ex);
				}
			}
			return;
		}

		// CLOSED state - increment error count
		int errorCount = this.monitoringErrorCount.incrementAndGet();
		if (errorCount >= CIRCUIT_BREAKER_THRESHOLD) {
			// Atomic transition from CLOSED to OPEN using CAS
			if (this.circuitBreakerState.compareAndSet(CircuitBreakerState.CLOSED, CircuitBreakerState.OPEN)) {
				this.circuitBreakerOpen.set(true);  // Deprecated field
				this.circuitBreakerOpenTime.set(System.currentTimeMillis());
				if (logger.isWarnEnabled()) {
					logger.warn("Circuit breaker opened after " + errorCount +
							" consecutive errors - monitoring suspended for " + (CIRCUIT_BREAKER_RESET_MS / 1000) + " seconds", ex);
				}
			}
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Error during delay monitoring (error count: " + errorCount + ")", ex);
		}
	}

	/**
	 * Record successful monitoring execution in HALF_OPEN state.
	 * After HALF_OPEN_MAX_CALLS successful executions, transition to CLOSED.
	 * Uses atomic CAS operations to prevent race conditions in state transitions.
	 */
	private void recordMonitoringSuccess() {
		CircuitBreakerState currentState = this.circuitBreakerState.get();
		if (currentState == CircuitBreakerState.HALF_OPEN) {
			int successCount = this.halfOpenSuccessCount.incrementAndGet();
			if (successCount >= HALF_OPEN_MAX_CALLS) {
				// Enough successful calls - atomically transition to CLOSED
				// CAS ensures only one thread transitions the state
				if (this.circuitBreakerState.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.CLOSED)) {
					this.circuitBreakerOpen.set(false);  // Deprecated field
					this.monitoringErrorCount.set(0);
					if (logger.isInfoEnabled()) {
						logger.info("Circuit breaker closed after " + successCount + " successful monitoring executions");
					}
				}
			}
		}
	}

	/**
	 * Monitor CPU usage of the monitoring thread itself.
	 * Logs a warning if CPU usage is excessive.
	 */
	private void monitorCpuUsage() {
		if (!threadMXBean.isThreadCpuTimeSupported() || this.monitorThreadId < 0) {
			return;
		}

		long now = System.currentTimeMillis();
		// Check CPU usage periodically
		if (now - this.lastCpuCheckTime < CPU_CHECK_INTERVAL_MS) {
			return;
		}

		long currentCpuTime = threadMXBean.getThreadCpuTime(this.monitorThreadId);
		if (this.lastMonitorCpuTime > 0) {
			long cpuTimeDelta = currentCpuTime - this.lastMonitorCpuTime;
			long wallTimeDelta = (now - this.lastCpuCheckTime) * 1_000_000; // Convert to nanos

			// Calculate CPU percentage
			double cpuPercent = (cpuTimeDelta * 100.0) / wallTimeDelta;

			// Warn if monitoring thread is using excessive CPU
			if (cpuPercent > CPU_USAGE_WARNING_THRESHOLD && logger.isWarnEnabled()) {
				logger.warn(String.format("Delay monitoring thread CPU usage is high (%.2f%%) - " +
						"consider increasing delayMonitoringInterval or disabling monitoring", cpuPercent));
			}
		}

		this.lastMonitorCpuTime = currentCpuTime;
		this.lastCpuCheckTime = now;
	}

	/**
	 * Escape string for JSON (minimal escaping for performance).
	 * Escapes quotes, backslashes, and control characters.
	 */
	private String escapeJson(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}

	/**
	 * Build warning message for large queue scenario.
	 */
	private String buildLargeQueueMessage(int queueSize, int poolSize, int activeCount) {
		if (this.structuredLoggingEnabled) {
			String message = escapeJson("Thread pool exhaustion detected with large queue size");
			return String.format("{\"event\":\"pool_exhaustion\",\"severity\":\"high\"," +
					"\"queue_size\":%d,\"pool_size\":%d,\"active_threads\":%d," +
					"\"message\":\"%s\"}",
					queueSize, poolSize, activeCount, message);
		}

		return String.format("Thread pool exhaustion detected with large queue size (%d). " +
				"Pool size: %d, Active threads: %d. " +
				"Consider significantly increasing the pool size via " +
				"ThreadPoolTaskScheduler.setPoolSize() or spring.task.scheduling.pool.size property, " +
				"or enable virtual threads via ThreadPoolTaskScheduler.setVirtualThreads(true).",
				queueSize, poolSize, activeCount);
	}

	/**
	 * Build warning message for delayed tasks.
	 */
	private String buildDelayedTasksMessage(int delayedCount, long maxDelay, int poolSize, int activeCount, int queueSize) {
		if (this.structuredLoggingEnabled) {
			String message = escapeJson("Scheduled tasks delayed due to thread pool exhaustion");
			return String.format("{\"event\":\"delayed_tasks\",\"severity\":\"medium\"," +
					"\"delayed_count\":%d,\"max_delay_ms\":%d,\"pool_size\":%d," +
					"\"active_threads\":%d,\"queue_size\":%d," +
					"\"message\":\"%s\"}",
					delayedCount, maxDelay, poolSize, activeCount, queueSize, message);
		}

		String baseMessage = String.format("%d scheduled task%s %s delayed (max delay: %dms) due to thread pool exhaustion. " +
				"Pool size: %d, Active threads: %d, Queue size: %d. " +
				"Consider increasing the pool size via ThreadPoolTaskScheduler.setPoolSize() " +
				"or spring.task.scheduling.pool.size property, or enable virtual threads " +
				"via ThreadPoolTaskScheduler.setVirtualThreads(true).",
				delayedCount, delayedCount == 1 ? "" : "s", delayedCount == 1 ? "is" : "are",
				maxDelay, poolSize, activeCount, queueSize);

		// Add extra hint if pool size is 1 (default)
		if (poolSize == 1) {
			return baseMessage + " Note: Pool size is 1 (default), which is often insufficient for multiple scheduled tasks.";
		}

		return baseMessage;
	}

	/**
	 * Log a warning message at the configured log level.
	 */
	private void logWarning(String message) {
		switch (this.warningLogLevel) {
			case DEBUG -> {
				if (logger.isDebugEnabled()) {
					logger.debug(message);
				}
			}
			case INFO -> {
				if (logger.isInfoEnabled()) {
					logger.info(message);
				}
			}
			case WARN -> {
				if (logger.isWarnEnabled()) {
					logger.warn(message);
				}
			}
			case ERROR -> {
				if (logger.isErrorEnabled()) {
					logger.error(message);
				}
			}
		}
	}

	@Override
	public void shutdown() {
		stopDelayMonitor();
		super.shutdown();
	}

	/**
	 * Return the total number of delayed task warnings that have been logged.
	 * <p>This can be used for monitoring and alerting purposes.
	 * @return the count of delayed task warnings
	 * @since 6.2
	 */
	public int getDelayedTaskWarningCount() {
		return this.delayedTaskWarningCount.get();
	}


	private <V> RunnableScheduledFuture<V> decorateTaskIfNecessary(RunnableScheduledFuture<V> future) {
		return (this.taskDecorator != null ? new DelegatingRunnableScheduledFuture<>(future, this.taskDecorator) :
				future);
	}

	private Runnable errorHandlingTask(Runnable task, boolean isRepeatingTask) {
		return TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
	}


	private static class DelegatingRunnableScheduledFuture<V> implements RunnableScheduledFuture<V> {

		private final RunnableScheduledFuture<V> future;

		private final Runnable decoratedRunnable;

		public DelegatingRunnableScheduledFuture(RunnableScheduledFuture<V> future, TaskDecorator taskDecorator) {
			Assert.notNull(future, "Future must not be null");
			Assert.notNull(taskDecorator, "TaskDecorator must not be null");
			this.future = future;
			Runnable decorated = taskDecorator.decorate(this.future);
			// Fall back to original future if decorator returns null
			this.decoratedRunnable = (decorated != null ? decorated : this.future);
		}

		@Override
		public void run() {
			this.decoratedRunnable.run();
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return this.future.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return this.future.isCancelled();
		}

		@Override
		public boolean isDone() {
			return this.future.isDone();
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			return this.future.get();
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return this.future.get(timeout, unit);
		}

		@Override
		public boolean isPeriodic() {
			return this.future.isPeriodic();
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return this.future.getDelay(unit);
		}

		@Override
		public int compareTo(Delayed o) {
			return this.future.compareTo(o);
		}
	}

}
