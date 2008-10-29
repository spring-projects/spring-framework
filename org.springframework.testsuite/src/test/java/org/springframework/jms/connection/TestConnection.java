/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * @author Juergen Hoeller
 */
public class TestConnection implements Connection {

	private ExceptionListener exceptionListener;

	private int startCount;

	private int closeCount;


	public Session createSession(boolean b, int i) throws JMSException {
		return null;
	}

	public String getClientID() throws JMSException {
		return null;
	}

	public void setClientID(String paramName) throws JMSException {
	}

	public ConnectionMetaData getMetaData() throws JMSException {
		return null;
	}

	public ExceptionListener getExceptionListener() throws JMSException {
		return exceptionListener;
	}

	public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
		this.exceptionListener = exceptionListener;
	}

	public void start() throws JMSException {
		this.startCount++;
	}

	public void stop() throws JMSException {
	}

	public void close() throws JMSException {
		this.closeCount++;
	}

	public ConnectionConsumer createConnectionConsumer(Destination destination, String paramName, ServerSessionPool serverSessionPool, int i) throws JMSException {
		return null;
	}

	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String paramName, String paramName1, ServerSessionPool serverSessionPool, int i) throws JMSException {
		return null;
	}


	public int getStartCount() {
		return startCount;
	}

	public int getCloseCount() {
		return closeCount;
	}

}
