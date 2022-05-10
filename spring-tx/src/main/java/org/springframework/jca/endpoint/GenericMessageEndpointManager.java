/*
 * Copyright 2002-2021 the original author or authors.
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

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Generic bean that manages JCA 1.7 message endpoints within a Spring
 * application context, activating and deactivating the endpoint as part
 * of the application context's lifecycle.
 *
 * <p>This class is completely generic in that it may work with any
 * ResourceAdapter, any MessageEndpointFactory, and any ActivationSpec.
 * It can be configured in standard bean style, for example through
 * Spring's XML bean definition format, as follows:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointManager"&gt;
 *  &lt;property name="resourceAdapter" ref="resourceAdapter"/&gt;
 *  &lt;property name="messageEndpointFactory"&gt;
 *    &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointFactory"&gt;
 *      &lt;property name="messageListener" ref="messageListener"/&gt;
 *    &lt;/bean&gt;
 *  &lt;/property&gt;
 *  &lt;property name="activationSpec"&gt;
 *    &lt;bean class="org.apache.activemq.ra.ActiveMQActivationSpec"&gt;
 *      &lt;property name="destination" value="myQueue"/&gt;
 *      &lt;property name="destinationType" value="jakarta.jms.Queue"/&gt;
 *    &lt;/bean&gt;
 *  &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>In this example, Spring's own {@link GenericMessageEndpointFactory} is used
 * to point to a standard message listener object that happens to be supported
 * by the specified target ResourceAdapter: in this case, a JMS
 * {@link jakarta.jms.MessageListener} object as supported by the ActiveMQ
 * message broker, defined as a Spring bean:
 *
 * <pre class="code">
 * &lt;bean id="messageListener" class="com.myorg.messaging.myMessageListener"&gt;
 *   &lt;!-- ... --&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>The target ResourceAdapter may be configured as a local Spring bean as well
 * (the typical case) or obtained from JNDI (e.g. on WebLogic). For the
 * example above, a local ResourceAdapter bean could be defined as follows
 * (matching the "resourceAdapter" bean reference above):
 *
 * <pre class="code">
 * &lt;bean id="resourceAdapter" class="org.springframework.jca.support.ResourceAdapterFactoryBean"&gt;
 *  &lt;property name="resourceAdapter"&gt;
 *    &lt;bean class="org.apache.activemq.ra.ActiveMQResourceAdapter"&gt;
 *      &lt;property name="serverUrl" value="tcp://localhost:61616"/&gt;
 *    &lt;/bean&gt;
 *  &lt;/property&gt;
 *  &lt;property name="workManager"&gt;
 *    &lt;bean class="..."/&gt;
 *  &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>For a different target resource, the configuration would simply point to a
 * different ResourceAdapter and a different ActivationSpec object (which are
 * both specific to the resource provider), and possibly a different message
 * listener (e.g. a CCI {@link jakarta.resource.cci.MessageListener} for a
 * resource adapter which is based on the JCA Common Client Interface).
 *
 * <p>The asynchronous execution strategy can be customized through the
 * "workManager" property on the ResourceAdapterFactoryBean as shown above,
 * where {@code <bean class="..."/>} should be replaced with configuration for
 * any JCA-compliant {@code WorkManager}.
 *
 * <p>Transactional execution is a responsibility of the concrete message endpoint,
 * as built by the specified MessageEndpointFactory. {@link GenericMessageEndpointFactory}
 * supports XA transaction participation through its "transactionManager" property,
 * typically with a Spring {@link org.springframework.transaction.jta.JtaTransactionManager}
 * or a plain {@link jakarta.transaction.TransactionManager} implementation specified there.
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointManager"&gt;
 *  &lt;property name="resourceAdapter" ref="resourceAdapter"/&gt;
 *  &lt;property name="messageEndpointFactory"&gt;
 *    &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointFactory"&gt;
 *      &lt;property name="messageListener" ref="messageListener"/&gt;
 *      &lt;property name="transactionManager" ref="transactionManager"/&gt;
 *    &lt;/bean&gt;
 *  &lt;/property&gt;
 *  &lt;property name="activationSpec"&gt;
 *    &lt;bean class="org.apache.activemq.ra.ActiveMQActivationSpec"&gt;
 *      &lt;property name="destination" value="myQueue"/&gt;
 *      &lt;property name="destinationType" value="jakarta.jms.Queue"/&gt;
 *    &lt;/bean&gt;
 *  &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager"/&gt;
 * </pre>
 *
 * <p>Alternatively, check out your resource provider's ActivationSpec object,
 * which should support local transactions through a provider-specific config flag,
 * e.g. ActiveMQActivationSpec's "useRAManagedTransaction" bean property.
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointManager"&gt;
 *  &lt;property name="resourceAdapter" ref="resourceAdapter"/&gt;
 *  &lt;property name="messageEndpointFactory"&gt;
 *    &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointFactory"&gt;
 *      &lt;property name="messageListener" ref="messageListener"/&gt;
 *    &lt;/bean&gt;
 *  &lt;/property&gt;
 *  &lt;property name="activationSpec"&gt;
 *    &lt;bean class="org.apache.activemq.ra.ActiveMQActivationSpec"&gt;
 *      &lt;property name="destination" value="myQueue"/&gt;
 *      &lt;property name="destinationType" value="jakarta.jms.Queue"/&gt;
 *      &lt;property name="useRAManagedTransaction" value="true"/&gt;
 *    &lt;/bean&gt;
 *  &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see jakarta.resource.spi.ResourceAdapter#endpointActivation
 * @see jakarta.resource.spi.ResourceAdapter#endpointDeactivation
 * @see jakarta.resource.spi.endpoint.MessageEndpointFactory
 * @see jakarta.resource.spi.ActivationSpec
 */
public class GenericMessageEndpointManager implements SmartLifecycle, InitializingBean, DisposableBean {

	@Nullable
	private ResourceAdapter resourceAdapter;

	@Nullable
	private MessageEndpointFactory messageEndpointFactory;

	@Nullable
	private ActivationSpec activationSpec;

	private boolean autoStartup = true;

	private int phase = DEFAULT_PHASE;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Set the JCA ResourceAdapter to manage endpoints for.
	 */
	public void setResourceAdapter(@Nullable ResourceAdapter resourceAdapter) {
		this.resourceAdapter = resourceAdapter;
	}

	/**
	 * Return the JCA ResourceAdapter to manage endpoints for.
	 */
	@Nullable
	public ResourceAdapter getResourceAdapter() {
		return this.resourceAdapter;
	}

	/**
	 * Set the JCA MessageEndpointFactory to activate, pointing to a
	 * MessageListener object that the endpoints will delegate to.
	 * <p>A MessageEndpointFactory instance may be shared across multiple
	 * endpoints (i.e. multiple GenericMessageEndpointManager instances),
	 * with different {@link #setActivationSpec ActivationSpec} objects applied.
	 * @see GenericMessageEndpointFactory#setMessageListener
	 */
	public void setMessageEndpointFactory(@Nullable MessageEndpointFactory messageEndpointFactory) {
		this.messageEndpointFactory = messageEndpointFactory;
	}

	/**
	 * Return the JCA MessageEndpointFactory to activate.
	 */
	@Nullable
	public MessageEndpointFactory getMessageEndpointFactory() {
		return this.messageEndpointFactory;
	}

	/**
	 * Set the JCA ActivationSpec to use for activating the endpoint.
	 * <p>Note that this ActivationSpec instance should not be shared
	 * across multiple ResourceAdapter instances.
	 */
	public void setActivationSpec(@Nullable ActivationSpec activationSpec) {
		this.activationSpec = activationSpec;
	}

	/**
	 * Return the JCA ActivationSpec to use for activating the endpoint.
	 */
	@Nullable
	public ActivationSpec getActivationSpec() {
		return this.activationSpec;
	}

	/**
	 * Set whether to auto-start the endpoint activation after this endpoint
	 * manager has been initialized and the context has been refreshed.
	 * <p>Default is "true". Turn this flag off to defer the endpoint
	 * activation until an explicit {@link #start()} call.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Return the value for the 'autoStartup' property.	If "true", this
	 * endpoint manager will start upon a ContextRefreshedEvent.
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Specify the phase in which this endpoint manager should be started
	 * and stopped. The startup order proceeds from lowest to highest, and
	 * the shutdown order is the reverse of that. By default this value is
	 * Integer.MAX_VALUE meaning that this endpoint manager starts as late
	 * as possible and stops as soon as possible.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Return the phase in which this endpoint manager will be started and stopped.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * Prepares the message endpoint, and automatically activates it
	 * if the "autoStartup" flag is set to "true".
	 */
	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (getResourceAdapter() == null) {
			throw new IllegalArgumentException("Property 'resourceAdapter' is required");
		}
		if (getMessageEndpointFactory() == null) {
			throw new IllegalArgumentException("Property 'messageEndpointFactory' is required");
		}
		ActivationSpec activationSpec = getActivationSpec();
		if (activationSpec == null) {
			throw new IllegalArgumentException("Property 'activationSpec' is required");
		}

		if (activationSpec.getResourceAdapter() == null) {
			activationSpec.setResourceAdapter(getResourceAdapter());
		}
		else if (activationSpec.getResourceAdapter() != getResourceAdapter()) {
			throw new IllegalArgumentException("ActivationSpec [" + activationSpec +
					"] is associated with a different ResourceAdapter: " + activationSpec.getResourceAdapter());
		}
	}

	/**
	 * Activates the configured message endpoint.
	 */
	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				ResourceAdapter resourceAdapter = getResourceAdapter();
				Assert.state(resourceAdapter != null, "No ResourceAdapter set");
				try {
					resourceAdapter.endpointActivation(getMessageEndpointFactory(), getActivationSpec());
				}
				catch (ResourceException ex) {
					throw new IllegalStateException("Could not activate message endpoint", ex);
				}
				this.running = true;
			}
		}
	}

	/**
	 * Deactivates the configured message endpoint.
	 */
	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				ResourceAdapter resourceAdapter = getResourceAdapter();
				Assert.state(resourceAdapter != null, "No ResourceAdapter set");
				resourceAdapter.endpointDeactivation(getMessageEndpointFactory(), getActivationSpec());
				this.running = false;
			}
		}
	}

	@Override
	public void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	/**
	 * Return whether the configured message endpoint is currently active.
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Deactivates the message endpoint, preparing it for shutdown.
	 */
	@Override
	public void destroy() {
		stop();
	}

}
