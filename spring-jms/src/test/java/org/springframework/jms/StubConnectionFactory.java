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

package org.springframework.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;

/**
 * A stub implementation of the JMS ConnectionFactory for testing.
 *
 * @author Mark Fisher
 */
public class StubConnectionFactory implements ConnectionFactory {

	@Override
	public Connection createConnection() throws JMSException {
		return null;
	}

	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		return null;
	}

	@Override
	public JMSContext createContext() {
		return null;
	}

	@Override
	public JMSContext createContext(String userName, String password) {
		return null;
	}

	@Override
	public JMSContext createContext(String userName, String password, int sessionMode) {
		return null;
	}

	@Override
	public JMSContext createContext(int sessionMode) {
		return null;
	}

}
