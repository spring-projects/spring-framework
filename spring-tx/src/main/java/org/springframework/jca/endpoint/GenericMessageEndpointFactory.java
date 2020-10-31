/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jca.endpoint;

import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Generic implementation of the JCA 1.7
 * {@link javax.resource.spi.endpoint.MessageEndpointFactory} interface,
 * providing transaction management capabilities for any kind of message
 * listener object (e.g. {@link javax.jms.MessageListener} objects or
 * {@link javax.resource.cci.MessageListener} objects.
 *
 * <p>Uses AOP proxies for concrete endpoint instances, simply wrapping
 * the specified message listener object and exposing all of its implemented
 * interfaces on the endpoint instance.
 *
 * <p>Typically used with Spring's {@link GenericMessageEndpointManager},
 * but not tied to it. As a consequence, this endpoint factory could
 * also be used with programmatic endpoint management on a native
 * {@link javax.resource.spi.ResourceAdapter} instance.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setMessageListener
 * @see #setTransactionManager
 * @see GenericMessageEndpointManager
 */
public class GenericMessageEndpointFactory extends AbstractMessageEndpointFactory {

	@Nullable
	private Object messageListener;


	/**
	 * Specify the message listener object that the endpoint should expose
	 * (e.g. a {@link javax.jms.MessageListener} objects or
	 * {@link javax.resource.cci.MessageListener} implementation).
	 */
	public void setMessageListener(Object messageListener) {
		this.messageListener = messageListener;
	}

	/**
	 * Return the message listener object for this endpoint.
	 * @since 5.0
	 */
	protected Object getMessageListener() {
		Assert.state(this.messageListener != null, "No message listener set");
		return this.messageListener;
	}

	/**
	 * Wrap each concrete endpoint instance with an AOP proxy,
	 * exposing the message listener's interfaces as well as the
	 * endpoint SPI through an AOP introduction.
	 */
	@Override
	public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
		GenericMessageEndpoint endpoint = (GenericMessageEndpoint) super.createEndpoint(xaResource);
		ProxyFactory proxyFactory = new ProxyFactory(getMessageListener());
		DelegatingIntroductionInterceptor introduction = new DelegatingIntroductionInterceptor(endpoint);
		introduction.suppressInterface(MethodInterceptor.class);
		proxyFactory.addAdvice(introduction);
		return (MessageEndpoint) proxyFactory.getProxy();
	}

	/**
	 * Creates a concrete generic message endpoint, internal to this factory.
	 */
	@Override
	protected AbstractMessageEndpoint createEndpointInternal() throws UnavailableException {
		return new GenericMessageEndpoint();
	}


	/**
	 * Private inner class that implements the concrete generic message endpoint,
	 * as an AOP Alliance MethodInterceptor that will be invoked by a proxy.
	 */
	private class GenericMessageEndpoint extends AbstractMessageEndpoint implements MethodInterceptor {

		@Override
		@Nullable
		public Object invoke(MethodInvocation methodInvocation) throws Throwable {
			Throwable endpointEx = null;
			boolean applyDeliveryCalls = !hasBeforeDeliveryBeenCalled();
			if (applyDeliveryCalls) {
				try {
					beforeDelivery(null);
				}
				catch (ResourceException ex) {
					throw adaptExceptionIfNecessary(methodInvocation, ex);
				}
			}
			try {
				return methodInvocation.proceed();
			}
			catch (Throwable ex) {
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
							throw adaptExceptionIfNecessary(methodInvocation, ex);
						}
					}
				}
			}
		}

		private Exception adaptExceptionIfNecessary(MethodInvocation methodInvocation, ResourceException ex) {
			if (ReflectionUtils.declaresException(methodInvocation.getMethod(), ex.getClass())) {
				return ex;
			}
			else {
				return new InternalResourceException(ex);
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
	public static class InternalResourceException extends RuntimeException {

		public InternalResourceException(ResourceException cause) {
			super(cause);
		}
	}

}
