/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.springframework.util.Assert;

/**
 * Implementation of the JMS ExceptionListener interface that supports chaining,
 * allowing the addition of multiple ExceptionListener instances in order.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ChainedExceptionListener implements ExceptionListener {

	/** List of ExceptionListeners */
	private final List<ExceptionListener> delegates = new ArrayList<ExceptionListener>(2);


	/**
	 * Add an ExceptionListener to the chained delegate list.
	 */
	public final void addDelegate(ExceptionListener listener) {
		Assert.notNull(listener, "ExceptionListener must not be null");
		this.delegates.add(listener);
	}

	/**
	 * Return all registered ExceptionListener delegates (as array).
	 */
	public final ExceptionListener[] getDelegates() {
		return this.delegates.toArray(new ExceptionListener[this.delegates.size()]);
	}


	public void onException(JMSException ex) {
		for (ExceptionListener listener : this.delegates) {
			listener.onException(ex);
		}
	}

}
