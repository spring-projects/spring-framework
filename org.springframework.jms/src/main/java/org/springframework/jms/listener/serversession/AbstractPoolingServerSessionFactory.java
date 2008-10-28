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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.support.JmsUtils;
import org.springframework.scheduling.timer.TimerTaskExecutor;

/**
 * Abstract base class for ServerSessionFactory implementations
 * that pool ServerSessionFactory instances.
 * 
 * <p>Provides a factory method that creates a poolable ServerSession
 * (to be added as new instance to a pool), a callback method invoked
 * when a ServerSession finished an execution of its listener (to return
 * an instance to the pool), and a method to destroy a ServerSession instance
 * (after removing an instance from the pool).
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 2.5, in favor of DefaultMessageListenerContainer
 * and JmsMessageEndpointManager. To be removed in Spring 3.0.
 * @see org.springframework.jms.listener.serversession.CommonsPoolServerSessionFactory
 */
public abstract class AbstractPoolingServerSessionFactory implements ServerSessionFactory {

	protected final Log logger = LogFactory.getLog(getClass());

	private TaskExecutor taskExecutor;

	private int maxSize;


	/**
	 * Specify the TaskExecutor to use for executing ServerSessions
	 * (and consequently, the underlying MessageListener).
	 * <p>Default is a {@link org.springframework.scheduling.timer.TimerTaskExecutor}
	 * for each pooled ServerSession, using one Thread per pooled JMS Session.
	 * Alternatives are a shared TimerTaskExecutor, sharing a single Thread
	 * for the execution of all ServerSessions, or a TaskExecutor
	 * implementation backed by a thread pool.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Return the TaskExecutor to use for executing ServerSessions.
	 */
	protected TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	/**
	 * Set the maximum size of the pool.
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * Return the maximum size of the pool.
	 */
	public int getMaxSize() {
		return this.maxSize;
	}


	/**
	 * Create a new poolable ServerSession.
	 * To be called when a new instance should be added to the pool.
	 * @param sessionManager the listener session manager to create the
	 * poolable ServerSession for
	 * @return the new poolable ServerSession
	 * @throws JMSException if creation failed
	 */
	protected final ServerSession createServerSession(ListenerSessionManager sessionManager) throws JMSException {
		return new PoolableServerSession(sessionManager);
	}

	/**
	 * Destroy the given poolable ServerSession.
	 * To be called when an instance got removed from the pool.
	 * @param serverSession the poolable ServerSession to destroy
	 */
	protected final void destroyServerSession(ServerSession serverSession) {
		if (serverSession != null) {
			((PoolableServerSession) serverSession).close();
		}
	}


	/**
	 * Template method called by a ServerSession if it finished
	 * execution of its listener and is ready to go back into the pool.
	 * <p>Subclasses should implement the actual returning of the instance
	 * to the pool.
	 * @param serverSession the ServerSession that finished its execution
	 * @param sessionManager the session manager that the ServerSession belongs to
	 */
	protected abstract void serverSessionFinished(
			ServerSession serverSession, ListenerSessionManager sessionManager);


	/**
	 * ServerSession implementation designed to be pooled.
	 * Creates a new JMS Session on instantiation, reuses it
	 * for all executions, and closes it on <code>close</code>.
	 * <p>Creates a TimerTaskExecutor (using a single Thread) per
	 * ServerSession, unless given a specific TaskExecutor to use.
	 */
	private class PoolableServerSession implements ServerSession {

		private final ListenerSessionManager sessionManager;

		private final Session session;

		private TaskExecutor taskExecutor;

		private TimerTaskExecutor internalExecutor;

		public PoolableServerSession(final ListenerSessionManager sessionManager) throws JMSException {
			this.sessionManager = sessionManager;
			this.session = sessionManager.createListenerSession();
			this.taskExecutor = getTaskExecutor();
			if (this.taskExecutor == null) {
				this.internalExecutor = new TimerTaskExecutor();
				this.internalExecutor.afterPropertiesSet();
				this.taskExecutor = this.internalExecutor;
			}
		}

		public Session getSession() {
			return this.session;
		}

		public void start() {
			this.taskExecutor.execute(new Runnable() {
				public void run() {
					try {
						sessionManager.executeListenerSession(session);
					}
					finally {
						serverSessionFinished(PoolableServerSession.this, sessionManager);
					}
				}
			});
		}

		public void close() {
			if (this.internalExecutor != null) {
				this.internalExecutor.destroy();
			}
			JmsUtils.closeSession(this.session);
		}
	}

}
