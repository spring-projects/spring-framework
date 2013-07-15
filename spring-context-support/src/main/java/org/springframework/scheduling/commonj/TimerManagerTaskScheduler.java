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

package org.springframework.scheduling.commonj;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import commonj.timers.Timer;
import commonj.timers.TimerListener;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.ErrorHandler;

/**
 * Implementation of Spring's {@link TaskScheduler} interface, wrapping
 * a CommonJ {@link commonj.timers.TimerManager}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 */
public class TimerManagerTaskScheduler extends TimerManagerAccessor implements TaskScheduler {

	private volatile ErrorHandler errorHandler;


	/**
	 * Provide an {@link ErrorHandler} strategy.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}


	@Override
	public ScheduledFuture schedule(Runnable task, Trigger trigger) {
		return new ReschedulingTimerListener(errorHandlingTask(task, true), trigger).schedule();
	}

	@Override
	public ScheduledFuture schedule(Runnable task, Date startTime) {
		TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, false));
		Timer timer = getTimerManager().schedule(futureTask, startTime);
		futureTask.setTimer(timer);
		return futureTask;
	}

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, true));
		Timer timer = getTimerManager().scheduleAtFixedRate(futureTask, startTime, period);
		futureTask.setTimer(timer);
		return futureTask;
	}

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, long period) {
		TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, true));
		Timer timer = getTimerManager().scheduleAtFixedRate(futureTask, 0, period);
		futureTask.setTimer(timer);
		return futureTask;
	}

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, true));
		Timer timer = getTimerManager().schedule(futureTask, startTime, delay);
		futureTask.setTimer(timer);
		return futureTask;
	}

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, long delay) {
		TimerScheduledFuture futureTask = new TimerScheduledFuture(errorHandlingTask(task, true));
		Timer timer = getTimerManager().schedule(futureTask, 0, delay);
		futureTask.setTimer(timer);
		return futureTask;
	}

	private Runnable errorHandlingTask(Runnable delegate, boolean isRepeatingTask) {
		return TaskUtils.decorateTaskWithErrorHandler(delegate, this.errorHandler, isRepeatingTask);
	}


	/**
	 * ScheduledFuture adapter that wraps a CommonJ Timer.
	 */
	private static class TimerScheduledFuture extends FutureTask<Object> implements TimerListener, ScheduledFuture<Object> {

		protected transient Timer timer;

		protected transient boolean cancelled = false;

		public TimerScheduledFuture(Runnable runnable) {
			super(runnable, null);
		}

		public void setTimer(Timer timer) {
			this.timer = timer;
		}

		@Override
		public void timerExpired(Timer timer) {
			runAndReset();
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean result = super.cancel(mayInterruptIfRunning);
			this.timer.cancel();
			this.cancelled = true;
			return result;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(System.currentTimeMillis() - this.timer.getScheduledExecutionTime(), TimeUnit.MILLISECONDS);
		}

		@Override
		public int compareTo(Delayed other) {
			if (this == other) {
				return 0;
			}
			long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
			return (diff == 0 ? 0 : ((diff < 0)? -1 : 1));
		}
	}


	/**
	 * ScheduledFuture adapter for trigger-based rescheduling.
	 */
	private class ReschedulingTimerListener extends TimerScheduledFuture {

		private final Trigger trigger;

		private final SimpleTriggerContext triggerContext = new SimpleTriggerContext();

		private volatile Date scheduledExecutionTime;

		public ReschedulingTimerListener(Runnable runnable, Trigger trigger) {
			super(runnable);
			this.trigger = trigger;
		}

		public ScheduledFuture schedule() {
			this.scheduledExecutionTime = this.trigger.nextExecutionTime(this.triggerContext);
			if (this.scheduledExecutionTime == null) {
				return null;
			}
			setTimer(getTimerManager().schedule(this, this.scheduledExecutionTime));
			return this;
		}

		@Override
		public void timerExpired(Timer timer) {
			Date actualExecutionTime = new Date();
			super.timerExpired(timer);
			Date completionTime = new Date();
			this.triggerContext.update(this.scheduledExecutionTime, actualExecutionTime, completionTime);
			if (!this.cancelled) {
				schedule();
			}
		}
	}

}
