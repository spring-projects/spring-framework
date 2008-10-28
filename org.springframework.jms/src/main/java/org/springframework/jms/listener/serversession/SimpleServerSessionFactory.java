/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jms.listener.serversession;

import javax.jms.JMSException;
import javax.jms.ServerSession;
import javax.jms.Session;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.support.JmsUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * The simplest possible implementation of the ServerSessionFactory SPI:
 * creating a new ServerSession with a new JMS Session every time.
 * This is the default used by ServerSessionMessageListenerContainer.
 *
 * <p>The execution of a ServerSession (and its MessageListener) gets delegated
 * to a TaskExecutor. By default, a SimpleAsyncTaskExecutor will be used,
 * creating a new Thread for every execution attempt. Alternatives are a
 * TimerTaskExecutor, sharing a single Thread for the execution of all
 * ServerSessions, or a TaskExecutor implementation backed by a thread pool.
 *
 * <p>To reuse JMS Sessions and/or to limit the number of concurrent
 * ServerSession executions, consider using a pooling ServerSessionFactory:
 * for example, CommonsPoolServerSessionFactory.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 2.5, in favor of DefaultMessageListenerContainer
 * and JmsMessageEndpointManager. To be removed in Spring 3.0.
 * @see org.springframework.core.task.TaskExecutor
 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
 * @see org.springframework.scheduling.timer.TimerTaskExecutor
 * @see CommonsPoolServerSessionFactory
 * @see ServerSessionMessageListenerContainer
 */
public class SimpleServerSessionFactory implements ServerSessionFactory {

	/**
	 * Default thread name prefix: "SimpleServerSessionFactory-".
	 */
	public static final String DEFAULT_THREAD_NAME_PREFIX =
			ClassUtils.getShortName(SimpleServerSessionFactory.class) + "-";


	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor(DEFAULT_THREAD_NAME_PREFIX);


	/**
	 * Specify the TaskExecutor to use for executing ServerSessions
	 * (and consequently, the underlying MessageListener).
	 * <p>Default is a SimpleAsyncTaskExecutor, creating a new Thread for
	 * every execution attempt. Alternatives are a TimerTaskExecutor,
	 * sharing a single Thread for the execution of all ServerSessions,
	 * or a TaskExecutor implementation backed by a thread pool.
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 * @see org.springframework.scheduling.timer.TimerTaskExecutor
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "taskExecutor is required");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Return the TaskExecutor to use for executing ServerSessions.
	 */
	protected TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}


	/**
	 * Creates a new SimpleServerSession with a new JMS Session
	 * for every call.
	 */
	public ServerSession getServerSession(ListenerSessionManager sessionManager) throws JMSException {
		return new SimpleServerSession(sessionManager);
	}

	/**
	 * This implementation is empty, as there is no state held for
	 * each ListenerSessionManager.
	 */
	public void close(ListenerSessionManager sessionManager) {
	}


	/**
	 * ServerSession implementation that simply creates a new
	 * JMS Session and executes it via the specified TaskExecutor.
	 */
	private class SimpleServerSession implements ServerSession {

		private final ListenerSessionManager sessionManager;

		private final Session session;

		public SimpleServerSession(ListenerSessionManager sessionManager) throws JMSException {
			this.sessionManager = sessionManager;
			this.session = sessionManager.createListenerSession();
		}

		public Session getSession() {
			return session;
		}

		public void start() {
			getTaskExecutor().execute(new Runnable() {
				public void run() {
					try {
						sessionManager.executeListenerSession(session);
					}
					finally {
						JmsUtils.closeSession(session);
					}
				}
			});
		}
	}

}
