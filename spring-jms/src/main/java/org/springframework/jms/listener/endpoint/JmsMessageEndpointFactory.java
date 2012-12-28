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

package org.springframework.jms.listener.endpoint;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;

import org.springframework.jca.endpoint.AbstractMessageEndpointFactory;

/**
 * JMS-specific implementation of the JCA 1.5
 * {@link javax.resource.spi.endpoint.MessageEndpointFactory} interface,
 * providing transaction management capabilities for a JMS listener object
 * (e.g. a {@link javax.jms.MessageListener} object).
 *
 * <p>Uses a static endpoint implementation, simply wrapping the
 * specified message listener object and exposing all of its implemented
 * interfaces on the endpoint instance.
 *
 * <p>Typically used with Spring's {@link JmsMessageEndpointManager},
 * but not tied to it. As a consequence, this endpoint factory could
 * also be used with programmatic endpoint management on a native
 * {@link javax.resource.spi.ResourceAdapter} instance.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setMessageListener
 * @see #setTransactionManager
 * @see JmsMessageEndpointManager
 */
public class JmsMessageEndpointFactory extends AbstractMessageEndpointFactory  {

	private MessageListener messageListener;


	/**
	 * Set the JMS MessageListener for this endpoint.
	 */
	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	/**
	 * Creates a concrete JMS message endpoint, internal to this factory.
	 */
	protected AbstractMessageEndpoint createEndpointInternal() throws UnavailableException {
		return new JmsMessageEndpoint();
	}


	/**
	 * Private inner class that implements the concrete JMS message endpoint.
	 */
	private class JmsMessageEndpoint extends AbstractMessageEndpoint implements MessageListener {

		public void onMessage(Message message) {
			boolean applyDeliveryCalls = !hasBeforeDeliveryBeenCalled();
			if (applyDeliveryCalls) {
				try {
					beforeDelivery(null);
				}
				catch (ResourceException ex) {
					throw new JmsResourceException(ex);
				}
			}
			try {
				messageListener.onMessage(message);
			}
			catch (RuntimeException ex) {
				onEndpointException(ex);
				throw ex;
			}
			catch (Error err) {
				onEndpointException(err);
				throw err;
			}
			finally {
				if (applyDeliveryCalls) {
					try {
						afterDelivery();
					}
					catch (ResourceException ex) {
						throw new JmsResourceException(ex);
					}
				}
			}
		}

		protected ClassLoader getEndpointClassLoader() {
			return messageListener.getClass().getClassLoader();
		}
	}


	/**
	 * Internal exception thrown when a ResourceExeption has been encountered
	 * during the endpoint invocation.
	 * <p>Will only be used if the ResourceAdapter does not invoke the
	 * endpoint's {@code beforeDelivery} and {@code afterDelivery}
	 * directly, leavng it up to the concrete endpoint to apply those -
	 * and to handle any ResourceExceptions thrown from them.
	 */
	public static class JmsResourceException extends RuntimeException {

		public JmsResourceException(ResourceException cause) {
			super(cause);
		}
	}

}
