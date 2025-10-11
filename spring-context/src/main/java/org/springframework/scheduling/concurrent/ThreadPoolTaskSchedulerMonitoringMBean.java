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

/**
 * JMX MBean interface for monitoring and managing {@link ThreadPoolTaskScheduler}
 * delay monitoring at runtime.
 *
 * <p>This interface exposes operational metrics and allows dynamic configuration
 * of monitoring settings without requiring application restart.
 *
 * @author Spring Framework Contributors
 * @since 6.2
 * @see ThreadPoolTaskScheduler
 */
public interface ThreadPoolTaskSchedulerMonitoringMBean {

	/**
	 * Return whether delay monitoring is currently enabled.
	 * @return true if monitoring is enabled, false otherwise
	 */
	boolean isDelayMonitoringEnabled();

	/**
	 * Enable or disable delay monitoring at runtime.
	 * @param enabled whether to enable delay monitoring
	 */
	void setDelayMonitoringEnabled(boolean enabled);

	/**
	 * Return the current monitoring interval in milliseconds.
	 * @return the monitoring interval
	 */
	long getDelayMonitoringInterval();

	/**
	 * Set the monitoring interval at runtime.
	 * @param interval the new interval in milliseconds (must be positive)
	 */
	void setDelayMonitoringInterval(long interval);

	/**
	 * Return the current delay warning threshold in milliseconds.
	 * @return the warning threshold
	 */
	long getDelayWarningThreshold();

	/**
	 * Set the delay warning threshold at runtime.
	 * @param threshold the new threshold in milliseconds (must be positive)
	 */
	void setDelayWarningThreshold(long threshold);

	/**
	 * Return the maximum delay observed for any task (in milliseconds).
	 * @return the maximum delay
	 */
	long getMaxDelayMillis();

	/**
	 * Return the number of times pool exhaustion has been detected.
	 * @return the pool exhaustion count
	 */
	int getPoolExhaustionCount();

	/**
	 * Return the total number of delayed task warnings that have been logged.
	 * @return the delayed task warning count
	 */
	int getDelayedTaskWarningCount();

	/**
	 * Return the current queue size of the scheduler.
	 * @return the queue size
	 */
	int getQueueSize();

	/**
	 * Return the current pool size.
	 * @return the pool size
	 */
	int getPoolSize();

	/**
	 * Return the number of currently active threads.
	 * @return the active thread count
	 */
	int getActiveCount();

	/**
	 * Return whether the circuit breaker is currently open.
	 * @return true if circuit breaker is open, false otherwise
	 */
	boolean isCircuitBreakerOpen();

	/**
	 * Return the current warning log level.
	 * @return the log level (DEBUG, INFO, WARN, or ERROR)
	 */
	String getWarningLogLevel();

	/**
	 * Set the warning log level at runtime.
	 * @param level the log level (DEBUG, INFO, WARN, or ERROR)
	 */
	void setWarningLogLevel(String level);

	/**
	 * Return whether structured logging is enabled.
	 * @return true if structured logging is enabled, false otherwise
	 */
	boolean isStructuredLoggingEnabled();

	/**
	 * Enable or disable structured logging at runtime.
	 * @param enabled whether to enable structured logging
	 */
	void setStructuredLoggingEnabled(boolean enabled);

	/**
	 * Reset all monitoring metrics.
	 */
	void resetMonitoringMetrics();

	/**
	 * Reset the circuit breaker state.
	 */
	void resetCircuitBreaker();
}
