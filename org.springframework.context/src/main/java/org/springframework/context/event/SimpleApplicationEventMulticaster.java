/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.context.event;

import java.util.Iterator;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Simple implementation of the {@link ApplicationEventMulticaster} interface.
 *
 * <p>Multicasts all events to all registered listeners, leaving it up to
 * the listeners to ignore events that they are not interested in.
 * Listeners will usually perform corresponding <code>instanceof</code>
 * checks on the passed-in event object.
 *
 * <p>By default, all listeners are invoked in the calling thread.
 * This allows the danger of a rogue listener blocking the entire application,
 * but adds minimal overhead. Specify an alternative TaskExecutor to have
 * listeners executed in different threads, for example from a thread pool.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setTaskExecutor
 * @see #setConcurrentUpdates
 */
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

	private TaskExecutor taskExecutor = new SyncTaskExecutor();


	/**
	 * Set the TaskExecutor to execute application listeners with.
	 * <p>Default is a SyncTaskExecutor, executing the listeners synchronously
	 * in the calling thread.
	 * <p>Consider specifying an asynchronous TaskExecutor here to not block the
	 * caller until all listeners have been executed. However, note that asynchronous
	 * execution will not participate in the caller's thread context (class loader,
	 * transaction association) unless the TaskExecutor explicitly supports this.
	 * @see org.springframework.core.task.SyncTaskExecutor
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 * @see org.springframework.scheduling.timer.TimerTaskExecutor
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = (taskExecutor != null ? taskExecutor : new SyncTaskExecutor());
	}

	/**
	 * Return the current TaskExecutor for this multicaster.
	 */
	protected TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	public void multicastEvent(final ApplicationEvent event) {
		for (Iterator it = getApplicationListeners().iterator(); it.hasNext();) {
			final ApplicationListener listener = (ApplicationListener) it.next();
			getTaskExecutor().execute(new Runnable() {
				public void run() {
					listener.onApplicationEvent(event);
				}
			});
		}
	}

}
