/*
 * Copyright 2002-2007 the original author or authors.
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

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;

/**
 * Strategy interface for creating JCA 1.5 ActivationSpec objects
 * based on a configured {@link JmsActivationSpecConfig} object.
 *
 * <p>JCA 1.5 ActivationSpec objects are typically JavaBeans, but
 * unfortunately provider-specific. This strategy interface allows
 * for plugging in any JCA-based JMS provider, creating corresponding
 * ActivationSpec objects based on common JMS configuration settings.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see JmsActivationSpecConfig
 * @see JmsMessageEndpointManager#setActivationSpecFactory
 * @see javax.resource.spi.ResourceAdapter#endpointActivation
 */
public interface JmsActivationSpecFactory {

	/**
	 * Create a JCA 1.5 ActivationSpec object based on the given
	 * {@link JmsActivationSpecConfig} object.
	 * @param adapter the ResourceAdapter to create an ActivationSpec object for
	 * @param config the configured object holding common JMS settings
	 * @return the provider-specific JCA ActivationSpec object,
	 * representing the same settings
	 */
	ActivationSpec createActivationSpec(ResourceAdapter adapter, JmsActivationSpecConfig config);

}
