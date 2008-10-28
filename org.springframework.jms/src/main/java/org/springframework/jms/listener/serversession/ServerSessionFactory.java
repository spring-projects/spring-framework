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

import org.springframework.jms.listener.serversession.ListenerSessionManager;

/**
 * SPI interface to be implemented by components that manage
 * JMS ServerSessions. Usually, but not necessarily, an implementation
 * of this interface will hold a pool of ServerSessions.
 *
 * <p>The passed-in ListenerSessionManager has to be used for creating
 * and executing JMS Sessions. This session manager is responsible for
 * registering a MessageListener with all Sessions that it creates.
 * Consequently, the ServerSessionFactory implementation has to
 * concentrate on the actual lifecycle (e.g. pooling) of JMS Sessions,
 * but is not concerned about Session creation or execution.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 2.5, in favor of DefaultMessageListenerContainer
 * and JmsMessageEndpointManager. To be removed in Spring 3.0.
 * @see org.springframework.jms.listener.serversession.ListenerSessionManager
 * @see org.springframework.jms.listener.serversession.ServerSessionMessageListenerContainer
 */
public interface ServerSessionFactory {

	/**
	 * Retrieve a JMS ServerSession for the given session manager.
	 * @param sessionManager the session manager to use for
	 * creating and executing new listener sessions
	 * (implicitly indicating the target listener to invoke)
	 * @return the JMS ServerSession
	 * @throws JMSException if retrieval failed
	 */
	ServerSession getServerSession(ListenerSessionManager sessionManager) throws JMSException;

	/**
	 * Close all ServerSessions for the given session manager.
	 * @param sessionManager the session manager used for
	 * creating and executing new listener sessions
	 * (implicitly indicating the target listener)
	 */
	void close(ListenerSessionManager sessionManager);

}
