/*
 * Copyright 2002-2018 the original author or authors.
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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JMS-specific implementation of the JCA 1.7
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
 * @author Stephane Nicoll
 * @since 2.5
 * @see #setMessageListener
 * @see #setTransactionManager
 * @see JmsMessageEndpointManager
 */
public class JmsMessageEndpointFactory extends AbstractMessageEndpointFactory  {

	@Nullable
	private MessageListener messageListener;


	/**
	 * Set the JMS MessageListener for this endpoint.
	 */
	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	/**
	 * Return the JMS MessageListener for this endpoint.
	 */
	protected MessageListener getMessageListener() {
		Assert.state(messageListener != null, "No MessageListener set");
		return this.messageListener;
	}

	/**
	 * Creates a concrete JMS message endpoint, internal to this factory.
	 */
	@Override
	protected AbstractMessageEndpoint createEndpointInternal() throws UnavailableException {
		return new JmsMessageEndpoint();
	}


	/**
	 * Private inner class that implements the concrete JMS message endpoint.
	 */
	private class JmsMessageEndpoint extends AbstractMessageEndpoint implements MessageListener {

		@Override
		public void onMessage(Message message) {
			Throwable endpointEx = null;
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
				getMessageListener().onMessage(message);
			}
			catch (RuntimeException | Error ex) {
				endpointEx = ex;
				onEndpointException(ex);
				throw ex;
			}
			finally {
				if (applyDeliveryCalls) {
					try {
						afterDelivery();
					}
					catch (ResourceException ex) {
						if (endpointEx == null) {
							throw new JmsResourceException(ex);
						}
					}
				}
			}
		}

		@Override
		protected ClassLoader getEndpointClassLoader() {
			return getMessageListener().getClass().getClassLoader();
		}
	}


	/**
	 * Internal exception thrown when a ResourceException has been encountered
	 * during the endpoint invocation.
	 * <p>Will only be used if the ResourceAdapter does not invoke the
	 * endpoint's {@code beforeDelivery} and {@code afterDelivery}
	 * directly, leaving it up to the concrete endpoint to apply those -
	 * and to handle any ResourceExceptions thrown from them.
	 */
	@SuppressWarnings("serial")
	public static class JmsResourceException extends RuntimeException {

		public JmsResourceException(ResourceException cause) {
			super(cause);
		}
	}

}
