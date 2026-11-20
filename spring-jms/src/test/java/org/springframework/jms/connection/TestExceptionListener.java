/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.connection;

import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;

/**
 * @author Juergen Hoeller
 */
public class TestExceptionListener implements ExceptionListener {

	private int count = 0;

	@Override
	public void onException(JMSException ex) {
		this.count++;
	}

	public int getCount() {
		return count;
	}

}
