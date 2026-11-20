/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;

/**
 * A stub implementation of the JMS ConnectionFactory for testing.
 *
 * @author Mark Fisher
 */
public class StubConnectionFactory implements ConnectionFactory {

	@Override
	public Connection createConnection() {
		return null;
	}

	@Override
	public Connection createConnection(String username, String password) {
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
