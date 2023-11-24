/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jms.connection;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionConsumer;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.ServerSessionPool;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/**
 * @author Juergen Hoeller
 */
public class TestConnection implements Connection {

	private ExceptionListener exceptionListener;

	private int startCount;

	private int closeCount;


	@Override
	public Session createSession(boolean b, int i) throws JMSException {
		return null;
	}

	@Override
	public Session createSession(int sessionMode) throws JMSException {
		return null;
	}

	@Override
	public Session createSession() throws JMSException {
		return null;
	}

	@Override
	public String getClientID() throws JMSException {
		return null;
	}

	@Override
	public void setClientID(String paramName) throws JMSException {
	}

	@Override
	public ConnectionMetaData getMetaData() throws JMSException {
		return null;
	}

	@Override
	public ExceptionListener getExceptionListener() throws JMSException {
		return exceptionListener;
	}

	@Override
	public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
		this.exceptionListener = exceptionListener;
	}

	@Override
	public void start() throws JMSException {
		this.startCount++;
	}

	@Override
	public void stop() throws JMSException {
	}

	@Override
	public void close() throws JMSException {
		this.closeCount++;
	}

	@Override
	public ConnectionConsumer createConnectionConsumer(Destination destination, String paramName, ServerSessionPool serverSessionPool, int i) throws JMSException {
		return null;
	}

	@Override
	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String paramName, String paramName1, ServerSessionPool serverSessionPool, int i) throws JMSException {
		return null;
	}

	@Override
	public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}

	@Override
	public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
		return null;
	}


	public int getStartCount() {
		return startCount;
	}

	public int getCloseCount() {
		return closeCount;
	}

}
