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

package org.springframework.jmx.support;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Provides supporting infrastructure for registering MBeans with an
 * {@link javax.management.MBeanServer}. The behavior when encountering
 * an existing MBean at a given {@link ObjectName} is fully configurable
 * allowing for flexible registration settings.
 *
 * <p>All registered MBeans are tracked and can be unregistered by calling
 * the #{@link #unregisterBeans()} method.
 *
 * <p>Sub-classes can receive notifications when an MBean is registered or
 * unregistered by overriding the {@link #onRegister(ObjectName)} and
 * {@link #onUnregister(ObjectName)} methods respectively.
 *
 * <p>By default, the registration process will fail if attempting to
 * register an MBean using a {@link javax.management.ObjectName} that is
 * already used.
 *
 * <p>By setting the {@link #setRegistrationPolicy(RegistrationPolicy) registrationPolicy}
 * property to {@link RegistrationPolicy#IGNORE_EXISTING} the registration process
 * will simply ignore existing MBeans leaving them registered. This is useful in settings
 * where multiple applications want to share a common MBean in a shared {@link MBeanServer}.
 *
 * <p>Setting {@link #setRegistrationPolicy(RegistrationPolicy) registrationPolicy} property
 * to {@link RegistrationPolicy#REPLACE_EXISTING} will cause existing MBeans to be replaced
 * during registration if necessary. This is useful in situations where you can't guarantee
 * the state of your {@link MBeanServer}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.0
 * @see #setServer
 * @see #setRegistrationPolicy
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class MBeanRegistrationSupport {

	/**
	 * {@code Log} instance for this class.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The {@code MBeanServer} instance being used to register beans.
	 */
	@Nullable
	protected MBeanServer server;

	/**
	 * The beans that have been registered by this exporter.
	 */
	private final Set<ObjectName> registeredBeans = new LinkedHashSet<>();

	/**
	 * The policy used when registering an MBean and finding that it already exists.
	 * By default an exception is raised.
	 */
	private RegistrationPolicy registrationPolicy = RegistrationPolicy.FAIL_ON_EXISTING;


	/**
	 * Specify the {@code MBeanServer} instance with which all beans should
	 * be registered. The {@code MBeanExporter} will attempt to locate an
	 * existing {@code MBeanServer} if none is supplied.
	 */
	public void setServer(@Nullable MBeanServer server) {
		this.server = server;
	}

	/**
	 * Return the {@code MBeanServer} that the beans will be registered with.
	 */
	@Nullable
	public final MBeanServer getServer() {
		return this.server;
	}

	/**
	 * The policy to use when attempting to register an MBean
	 * under an {@link javax.management.ObjectName} that already exists.
	 * @param registrationPolicy the policy to use
	 * @since 3.2
	 */
	public void setRegistrationPolicy(RegistrationPolicy registrationPolicy) {
		Assert.notNull(registrationPolicy, "RegistrationPolicy must not be null");
		this.registrationPolicy = registrationPolicy;
	}


	/**
	 * Actually register the MBean with the server. The behavior when encountering
	 * an existing MBean can be configured using {@link #setRegistrationPolicy}.
	 * @param mbean the MBean instance
	 * @param objectName the suggested ObjectName for the MBean
	 * @throws JMException if the registration failed
	 */
	protected void doRegister(Object mbean, ObjectName objectName) throws JMException {
		Assert.state(this.server != null, "No MBeanServer set");
		ObjectName actualObjectName;

		synchronized (this.registeredBeans) {
			ObjectInstance registeredBean = null;
			try {
				registeredBean = this.server.registerMBean(mbean, objectName);
			}
			catch (InstanceAlreadyExistsException ex) {
				if (this.registrationPolicy == RegistrationPolicy.IGNORE_EXISTING) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring existing MBean at [" + objectName + "]");
					}
				}
				else if (this.registrationPolicy == RegistrationPolicy.REPLACE_EXISTING) {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug("Replacing existing MBean at [" + objectName + "]");
						}
						this.server.unregisterMBean(objectName);
						registeredBean = this.server.registerMBean(mbean, objectName);
					}
					catch (InstanceNotFoundException ex2) {
						if (logger.isErrorEnabled()) {
							logger.error("Unable to replace existing MBean at [" + objectName + "]", ex2);
						}
						throw ex;
					}
				}
				else {
					throw ex;
				}
			}

			// Track registration and notify listeners.
			actualObjectName = (registeredBean != null ? registeredBean.getObjectName() : null);
			if (actualObjectName == null) {
				actualObjectName = objectName;
			}
			this.registeredBeans.add(actualObjectName);
		}

		onRegister(actualObjectName, mbean);
	}

	/**
	 * Unregisters all beans that have been registered by an instance of this class.
	 */
	protected void unregisterBeans() {
		Set<ObjectName> snapshot;
		synchronized (this.registeredBeans) {
			snapshot = new LinkedHashSet<>(this.registeredBeans);
		}
		if (!snapshot.isEmpty()) {
			logger.info("Unregistering JMX-exposed beans");
			for (ObjectName objectName : snapshot) {
				doUnregister(objectName);
			}
		}
	}

	/**
	 * Actually unregister the specified MBean from the server.
	 * @param objectName the suggested ObjectName for the MBean
	 */
	protected void doUnregister(ObjectName objectName) {
		Assert.state(this.server != null, "No MBeanServer set");
		boolean actuallyUnregistered = false;

		synchronized (this.registeredBeans) {
			if (this.registeredBeans.remove(objectName)) {
				try {
					// MBean might already have been unregistered by an external process
					if (this.server.isRegistered(objectName)) {
						this.server.unregisterMBean(objectName);
						actuallyUnregistered = true;
					}
					else {
						if (logger.isWarnEnabled()) {
							logger.warn("Could not unregister MBean [" + objectName + "] as said MBean " +
									"is not registered (perhaps already unregistered by an external process)");
						}
					}
				}
				catch (JMException ex) {
					if (logger.isErrorEnabled()) {
						logger.error("Could not unregister MBean [" + objectName + "]", ex);
					}
				}
			}
		}

		if (actuallyUnregistered) {
			onUnregister(objectName);
		}
	}

	/**
	 * Return the {@link ObjectName ObjectNames} of all registered beans.
	 */
	protected final ObjectName[] getRegisteredObjectNames() {
		synchronized (this.registeredBeans) {
			return this.registeredBeans.toArray(new ObjectName[0]);
		}
	}


	/**
	 * Called when an MBean is registered under the given {@link ObjectName}. Allows
	 * subclasses to perform additional processing when an MBean is registered.
	 * <p>The default implementation delegates to {@link #onRegister(ObjectName)}.
	 * @param objectName the actual {@link ObjectName} that the MBean was registered with
	 * @param mbean the registered MBean instance
	 */
	protected void onRegister(ObjectName objectName, Object mbean) {
		onRegister(objectName);
	}

	/**
	 * Called when an MBean is registered under the given {@link ObjectName}. Allows
	 * subclasses to perform additional processing when an MBean is registered.
	 * <p>The default implementation is empty. Can be overridden in subclasses.
	 * @param objectName the actual {@link ObjectName} that the MBean was registered with
	 */
	protected void onRegister(ObjectName objectName) {
	}

	/**
	 * Called when an MBean is unregistered under the given {@link ObjectName}. Allows
	 * subclasses to perform additional processing when an MBean is unregistered.
	 * <p>The default implementation is empty. Can be overridden in subclasses.
	 * @param objectName the {@link ObjectName} that the MBean was registered with
	 */
	protected void onUnregister(ObjectName objectName) {
	}

}
