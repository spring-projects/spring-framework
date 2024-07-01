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

package org.springframework.util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;

/**
 * Simple stop watch, allowing for timing of a number of tasks, exposing total
 * running time and running time for each named task.
 *
 * <p>Conceals use of {@link System#nanoTime()}, improving the readability of
 * application code and reducing the likelihood of calculation errors.
 *
 * <p>Note that this object is not designed to be thread-safe and does not use
 * synchronization.
 *
 * <p>This class is normally used to verify performance during proof-of-concept
 * work and in development, rather than as part of production applications.
 *
 * <p>Running time is tracked and reported in nanoseconds. As of Spring Framework
 * 6.1, the default time unit for String renderings is seconds with decimal points
 * in nanosecond precision. Custom renderings with specific time units can be
 * requested through {@link #prettyPrint(TimeUnit)}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since May 2, 2001
 * @see #start()
 * @see #stop()
 * @see #shortSummary()
 * @see #prettyPrint()
 */
public class StopWatch {

	/**
	 * Identifier of this {@code StopWatch}.
	 * <p>Handy when we have output from multiple stop watches and need to
	 * distinguish between them in log or console output.
	 */
	private final String id;

	@Nullable
	private List<TaskInfo> taskList = new ArrayList<>(1);

	/** Start time of the current task. */
	private long startTimeNanos;

	/** Name of the current task. */
	@Nullable
	private String currentTaskName;

	@Nullable
	private TaskInfo lastTaskInfo;

	private int taskCount;

	/** Total running time. */
	private long totalTimeNanos;


	/**
	 * Construct a new {@code StopWatch}.
	 * <p>Does not start any task.
	 */
	public StopWatch() {
		this("");
	}

	/**
	 * Construct a new {@code StopWatch} with the given id.
	 * <p>The id is handy when we have output from multiple stop watches and need
	 * to distinguish between them.
	 * <p>Does not start any task.
	 * @param id identifier for this stop watch
	 */
	public StopWatch(String id) {
		this.id = id;
	}


	/**
	 * Get the id of this {@code StopWatch}, as specified on construction.
	 * @return the id (empty String by default)
	 * @since 4.2.2
	 * @see #StopWatch(String)
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Configure whether the {@link TaskInfo} array is built over time.
	 * <p>Set this to {@code false} when using a {@code StopWatch} for millions of
	 * tasks; otherwise, the {@code TaskInfo} structure will consume excessive memory.
	 * <p>Default is {@code true}.
	 */
	public void setKeepTaskList(boolean keepTaskList) {
		this.taskList = (keepTaskList ? new ArrayList<>() : null);
	}


	/**
	 * Start an unnamed task.
	 * <p>The results are undefined if {@link #stop()} or timing methods are
	 * called without invoking this method first.
	 * @see #start(String)
	 * @see #stop()
	 */
	public void start() throws IllegalStateException {
		start("");
	}

	/**
	 * Start a named task.
	 * <p>The results are undefined if {@link #stop()} or timing methods are
	 * called without invoking this method first.
	 * @param taskName the name of the task to start
	 * @see #start()
	 * @see #stop()
	 */
	public void start(String taskName) throws IllegalStateException {
		if (this.currentTaskName != null) {
			throw new IllegalStateException("Can't start StopWatch: it's already running");
		}
		this.currentTaskName = taskName;
		this.startTimeNanos = System.nanoTime();
	}

	/**
	 * Stop the current task.
	 * <p>The results are undefined if timing methods are called without invoking
	 * at least one pair of {@code start()} / {@code stop()} methods.
	 * @see #start()
	 * @see #start(String)
	 */
	public void stop() throws IllegalStateException {
		if (this.currentTaskName == null) {
			throw new IllegalStateException("Can't stop StopWatch: it's not running");
		}
		long lastTime = System.nanoTime() - this.startTimeNanos;
		this.totalTimeNanos += lastTime;
		this.lastTaskInfo = new TaskInfo(this.currentTaskName, lastTime);
		if (this.taskList != null) {
			this.taskList.add(this.lastTaskInfo);
		}
		++this.taskCount;
		this.currentTaskName = null;
	}

	/**
	 * Determine whether this {@code StopWatch} is currently running.
	 * @see #currentTaskName()
	 */
	public boolean isRunning() {
		return (this.currentTaskName != null);
	}

	/**
	 * Get the name of the currently running task, if any.
	 * @since 4.2.2
	 * @see #isRunning()
	 */
	@Nullable
	public String currentTaskName() {
		return this.currentTaskName;
	}

	/**
	 * Get the last task as a {@link TaskInfo} object.
	 * @throws IllegalStateException if no tasks have run yet
	 * @since 6.1
	 */
	public TaskInfo lastTaskInfo() throws IllegalStateException {
		Assert.state(this.lastTaskInfo != null, "No tasks run");
		return this.lastTaskInfo;
	}

	/**
	 * Get the last task as a {@link TaskInfo} object.
	 * @deprecated as of 6.1, in favor of {@link #lastTaskInfo()}
	 */
	@Deprecated(since = "6.1")
	public TaskInfo getLastTaskInfo() throws IllegalStateException {
		return lastTaskInfo();
	}

	/**
	 * Get the name of the last task.
	 * @see TaskInfo#getTaskName()
	 * @deprecated as of 6.1, in favor of {@link #lastTaskInfo()}
	 */
	@Deprecated(since = "6.1")
	public String getLastTaskName() throws IllegalStateException {
		return lastTaskInfo().getTaskName();
	}

	/**
	 * Get the time taken by the last task in nanoseconds.
	 * @since 5.2
	 * @see TaskInfo#getTimeNanos()
	 * @deprecated as of 6.1, in favor of {@link #lastTaskInfo()}
	 */
	@Deprecated(since = "6.1")
	public long getLastTaskTimeNanos() throws IllegalStateException {
		return lastTaskInfo().getTimeNanos();
	}

	/**
	 * Get the time taken by the last task in milliseconds.
	 * @see TaskInfo#getTimeMillis()
	 * @deprecated as of 6.1, in favor of {@link #lastTaskInfo()}
	 */
	@Deprecated(since = "6.1")
	public long getLastTaskTimeMillis() throws IllegalStateException {
		return lastTaskInfo().getTimeMillis();
	}

	/**
	 * Get an array of the data for tasks performed.
	 * @see #setKeepTaskList
	 */
	public TaskInfo[] getTaskInfo() {
		if (this.taskList == null) {
			throw new UnsupportedOperationException("Task info is not being kept!");
		}
		return this.taskList.toArray(new TaskInfo[0]);
	}

	/**
	 * Get the number of tasks timed.
	 */
	public int getTaskCount() {
		return this.taskCount;
	}

	/**
	 * Get the total time for all tasks in nanoseconds.
	 * @since 5.2
	 * @see #getTotalTime(TimeUnit)
	 */
	public long getTotalTimeNanos() {
		return this.totalTimeNanos;
	}

	/**
	 * Get the total time for all tasks in milliseconds.
	 * @see #getTotalTime(TimeUnit)
	 */
	public long getTotalTimeMillis() {
		return TimeUnit.NANOSECONDS.toMillis(this.totalTimeNanos);
	}

	/**
	 * Get the total time for all tasks in seconds.
	 * @see #getTotalTime(TimeUnit)
	 */
	public double getTotalTimeSeconds() {
		return getTotalTime(TimeUnit.SECONDS);
	}

	/**
	 * Get the total time for all tasks in the requested time unit
	 * (with decimal points in nanosecond precision).
	 * @param timeUnit the unit to use
	 * @since 6.1
	 * @see #getTotalTimeNanos()
	 * @see #getTotalTimeMillis()
	 * @see #getTotalTimeSeconds()
	 */
	public double getTotalTime(TimeUnit timeUnit) {
		return (double) this.totalTimeNanos / TimeUnit.NANOSECONDS.convert(1, timeUnit);
	}


	/**
	 * Generate a table describing all tasks performed in seconds
	 * (with decimal points in nanosecond precision).
	 * <p>For custom reporting, call {@link #getTaskInfo()} and use the data directly.
	 * @see #prettyPrint(TimeUnit)
	 * @see #getTotalTimeSeconds()
	 * @see TaskInfo#getTimeSeconds()
	 */
	public String prettyPrint() {
		return prettyPrint(TimeUnit.SECONDS);
	}

	/**
	 * Generate a table describing all tasks performed in the requested time unit
	 * (with decimal points in nanosecond precision).
	 * <p>For custom reporting, call {@link #getTaskInfo()} and use the data directly.
	 * @param timeUnit the unit to use for rendering total time and task time
	 * @since 6.1
	 * @see #prettyPrint()
	 * @see #getTotalTime(TimeUnit)
	 * @see TaskInfo#getTime(TimeUnit)
	 */
	public String prettyPrint(TimeUnit timeUnit) {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
		nf.setMaximumFractionDigits(9);
		nf.setGroupingUsed(false);

		NumberFormat pf = NumberFormat.getPercentInstance(Locale.ENGLISH);
		pf.setMinimumIntegerDigits(2);
		pf.setGroupingUsed(false);

		StringBuilder sb = new StringBuilder(128);
		sb.append("StopWatch '").append(getId()).append("': ");
		String total = (timeUnit == TimeUnit.NANOSECONDS ?
				nf.format(getTotalTimeNanos()) : nf.format(getTotalTime(timeUnit)));
		sb.append(total).append(" ").append(timeUnit.name().toLowerCase(Locale.ENGLISH));
		int width = Math.max(sb.length(), 40);
		sb.append("\n");

		if (this.taskList != null) {
			String line = "-".repeat(width) + "\n";
			String unitName = timeUnit.name();
			unitName = unitName.charAt(0) + unitName.substring(1).toLowerCase(Locale.ENGLISH);
			unitName = String.format("%-12s", unitName);
			sb.append(line);
			sb.append(unitName).append("  %       Task name\n");
			sb.append(line);

			int digits = total.indexOf('.');
			if (digits < 0) {
				digits = total.length();
			}
			nf.setMinimumIntegerDigits(digits);
			nf.setMaximumFractionDigits(10 - digits);

			for (TaskInfo task : this.taskList) {
				sb.append(String.format("%-14s", (timeUnit == TimeUnit.NANOSECONDS ?
						nf.format(task.getTimeNanos()) : nf.format(task.getTime(timeUnit)))));
				sb.append(String.format("%-8s",
						pf.format(task.getTimeSeconds() / getTotalTimeSeconds())));
				sb.append(task.getTaskName()).append('\n');
			}
		}
		else {
			sb.append("No task info kept");
		}

		return sb.toString();
	}

	/**
	 * Get a short description of the total running time in seconds.
	 * @see #prettyPrint()
	 * @see #prettyPrint(TimeUnit)
	 */
	public String shortSummary() {
		return "StopWatch '" + getId() + "': " + getTotalTimeSeconds() + " seconds";
	}

	/**
	 * Generate an informative string describing all tasks performed in seconds.
	 * @see #prettyPrint()
	 * @see #prettyPrint(TimeUnit)
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(shortSummary());
		if (this.taskList != null) {
			for (TaskInfo task : this.taskList) {
				sb.append("; [").append(task.getTaskName()).append("] took ").append(task.getTimeSeconds()).append(" seconds");
				long percent = Math.round(100.0 * task.getTimeSeconds() / getTotalTimeSeconds());
				sb.append(" = ").append(percent).append('%');
			}
		}
		else {
			sb.append("; no task info kept");
		}
		return sb.toString();
	}


	/**
	 * Nested class to hold data about one task executed within the {@code StopWatch}.
	 */
	public static final class TaskInfo {

		private final String taskName;

		private final long timeNanos;

		TaskInfo(String taskName, long timeNanos) {
			this.taskName = taskName;
			this.timeNanos = timeNanos;
		}

		/**
		 * Get the name of this task.
		 */
		public String getTaskName() {
			return this.taskName;
		}

		/**
		 * Get the time this task took in nanoseconds.
		 * @since 5.2
		 * @see #getTime(TimeUnit)
		 */
		public long getTimeNanos() {
			return this.timeNanos;
		}

		/**
		 * Get the time this task took in milliseconds.
		 * @see #getTime(TimeUnit)
		 */
		public long getTimeMillis() {
			return TimeUnit.NANOSECONDS.toMillis(this.timeNanos);
		}

		/**
		 * Get the time this task took in seconds.
		 * @see #getTime(TimeUnit)
		 */
		public double getTimeSeconds() {
			return getTime(TimeUnit.SECONDS);
		}

		/**
		 * Get the time this task took in the requested time unit
		 * (with decimal points in nanosecond precision).
		 * @param timeUnit the unit to use
		 * @since 6.1
		 * @see #getTimeNanos()
		 * @see #getTimeMillis()
		 * @see #getTimeSeconds()
		 */
		public double getTime(TimeUnit timeUnit) {
			return (double) this.timeNanos / TimeUnit.NANOSECONDS.convert(1, timeUnit);
		}
	}

}
