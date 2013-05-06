/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;


/**
 * @author Rossen Stoyanchev
 */
public class StubTaskScheduler implements TaskScheduler {

	@Override
	public ScheduledFuture schedule(Runnable task, Trigger trigger) {
		return new StubScheduledFuture();
	}

	@Override
	public ScheduledFuture schedule(Runnable task, Date startTime) {
		return new StubScheduledFuture();
	}

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		return new StubScheduledFuture();
	}

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable task, long period) {
		return new StubScheduledFuture();
	}

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		return new StubScheduledFuture();
	}

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable task, long delay) {
		return new StubScheduledFuture();
	}


	private static class StubScheduledFuture extends FutureTask implements ScheduledFuture {

		@SuppressWarnings("unchecked")
		public StubScheduledFuture() {
			super(new Callable() {
				public Object call() throws Exception {
					return null;
				}
			});
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed o) {
			return 0;
		}
	}
}
