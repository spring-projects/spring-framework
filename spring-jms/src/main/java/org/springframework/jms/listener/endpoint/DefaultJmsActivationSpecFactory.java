/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jms.listener.endpoint;

import javax.jms.Session;
import javax.resource.spi.ResourceAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;

/**
 * Default implementation of the {@link JmsActivationSpecFactory} interface.
 * Supports the standard JMS properties as defined by the JCA 1.5 specification,
 * as well as Spring's extended "maxConcurrency" and "prefetchSize" settings
 * through autodetection of well-known vendor-specific provider properties.
 *
 * <p>An ActivationSpec factory is effectively dependent on the concrete
 * JMS provider, e.g. on ActiveMQ. This default implementation simply
 * guesses the ActivationSpec class name from the provider's class name
 * ("ActiveMQResourceAdapter" -> "ActiveMQActivationSpec" in the same package,
 * or "ActivationSpecImpl" in the same package as the ResourceAdapter class),
 * and populates the ActivationSpec properties as suggested by the
 * JCA 1.5 specification (Appendix B). Specify the 'activationSpecClass'
 * property explicitly if these default naming rules do not apply.
 *
 * <p>Note: ActiveMQ, JORAM and WebSphere are supported in terms of extended
 * settings (through the detection of their bean property naming conventions).
 * The default ActivationSpec class detection rules may apply to other
 * JMS providers as well.
 *
 * <p>Thanks to Agim Emruli and Laurie Chan for pointing out WebSphere MQ
 * settings and contributing corresponding tests!
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setActivationSpecClass
 */
public class DefaultJmsActivationSpecFactory extends StandardJmsActivationSpecFactory {

	private static final String RESOURCE_ADAPTER_SUFFIX = "ResourceAdapter";

	private static final String RESOURCE_ADAPTER_IMPL_SUFFIX = "ResourceAdapterImpl";

	private static final String ACTIVATION_SPEC_SUFFIX = "ActivationSpec";

	private static final String ACTIVATION_SPEC_IMPL_SUFFIX = "ActivationSpecImpl";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * This implementation guesses the ActivationSpec class name from the
	 * provider's class name: e.g. "ActiveMQResourceAdapter" ->
	 * "ActiveMQActivationSpec" in the same package, or a class named
	 * "ActivationSpecImpl" in the same package as the ResourceAdapter class.
	 */
	@Override
	protected Class<?> determineActivationSpecClass(ResourceAdapter adapter) {
		String adapterClassName = adapter.getClass().getName();

		if (adapterClassName.endsWith(RESOURCE_ADAPTER_SUFFIX)) {
			// e.g. ActiveMQ
			String providerName =
					adapterClassName.substring(0, adapterClassName.length() - RESOURCE_ADAPTER_SUFFIX.length());
			String specClassName = providerName + ACTIVATION_SPEC_SUFFIX;
			try {
				return adapter.getClass().getClassLoader().loadClass(specClassName);
			}
			catch (ClassNotFoundException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No default <Provider>ActivationSpec class found: " + specClassName);
				}
			}
		}

		else if (adapterClassName.endsWith(RESOURCE_ADAPTER_IMPL_SUFFIX)){
			//e.g. WebSphere
			String providerName =
					adapterClassName.substring(0, adapterClassName.length() - RESOURCE_ADAPTER_IMPL_SUFFIX.length());
			String specClassName = providerName + ACTIVATION_SPEC_IMPL_SUFFIX;
			try {
				return adapter.getClass().getClassLoader().loadClass(specClassName);
			}
			catch (ClassNotFoundException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No default <Provider>ActivationSpecImpl class found: " + specClassName);
				}
			}
		}

		// e.g. JORAM
		String providerPackage = adapterClassName.substring(0, adapterClassName.lastIndexOf('.') + 1);
		String specClassName = providerPackage + ACTIVATION_SPEC_IMPL_SUFFIX;
		try {
			return adapter.getClass().getClassLoader().loadClass(specClassName);
		}
		catch (ClassNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No default ActivationSpecImpl class found in provider package: " + specClassName);
			}
		}

		// ActivationSpecImpl class in "inbound" subpackage (WebSphere MQ 6.0.2.1)
		specClassName = providerPackage + "inbound." + ACTIVATION_SPEC_IMPL_SUFFIX;
		try {
			return adapter.getClass().getClassLoader().loadClass(specClassName);
		}
		catch (ClassNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No default ActivationSpecImpl class found in inbound subpackage: " + specClassName);
			}
		}

		throw new IllegalStateException("No ActivationSpec class defined - " +
				"specify the 'activationSpecClass' property or override the 'determineActivationSpecClass' method");
	}

	/**
	 * This implementation supports Spring's extended "maxConcurrency"
	 * and "prefetchSize" settings through detecting corresponding
	 * ActivationSpec properties: "maxSessions"/"maxNumberOfWorks" and
	 * "maxMessagesPerSessions"/"maxMessages", respectively
	 * (following ActiveMQ's and JORAM's naming conventions).
	 */
	@Override
	protected void populateActivationSpecProperties(BeanWrapper bw, JmsActivationSpecConfig config) {
		super.populateActivationSpecProperties(bw, config);
		if (config.getMaxConcurrency() > 0) {
			if (bw.isWritableProperty("maxSessions")) {
				// ActiveMQ
				bw.setPropertyValue("maxSessions", Integer.toString(config.getMaxConcurrency()));
			}
			else if (bw.isWritableProperty("maxNumberOfWorks")) {
				// JORAM
				bw.setPropertyValue("maxNumberOfWorks", Integer.toString(config.getMaxConcurrency()));
			}
			else if (bw.isWritableProperty("maxConcurrency")){
				// WebSphere
				bw.setPropertyValue("maxConcurrency", Integer.toString(config.getMaxConcurrency()));
			}
		}
		if (config.getPrefetchSize() > 0) {
			if (bw.isWritableProperty("maxMessagesPerSessions")) {
				// ActiveMQ
				bw.setPropertyValue("maxMessagesPerSessions", Integer.toString(config.getPrefetchSize()));
			}
			else if (bw.isWritableProperty("maxMessages")) {
				// JORAM
				bw.setPropertyValue("maxMessages", Integer.toString(config.getPrefetchSize()));
			}
			else if (bw.isWritableProperty("maxBatchSize")){
				// WebSphere
				bw.setPropertyValue("maxBatchSize", Integer.toString(config.getPrefetchSize()));
			}
		}
	}

	/**
	 * This implementation maps {@code SESSION_TRANSACTED} onto an
	 * ActivationSpec property named "useRAManagedTransaction", if available
	 * (following ActiveMQ's naming conventions).
	 */
	@Override
	protected void applyAcknowledgeMode(BeanWrapper bw, int ackMode) {
		if (ackMode == Session.SESSION_TRANSACTED && bw.isWritableProperty("useRAManagedTransaction")) {
			// ActiveMQ
			bw.setPropertyValue("useRAManagedTransaction", "true");
		}
		else {
			super.applyAcknowledgeMode(bw, ackMode);
		}
	}

}
