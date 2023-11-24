/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jca;

import javax.transaction.xa.XAResource;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

/**
 * @author Juergen Hoeller
 */
public class StubResourceAdapter implements ResourceAdapter {

	@Override
	public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
	}

	@Override
	public void stop() {
	}

	@Override
	public void endpointActivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) throws ResourceException {
	}

	@Override
	public void endpointDeactivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) {
	}

	@Override
	public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
		return null;
	}

}
